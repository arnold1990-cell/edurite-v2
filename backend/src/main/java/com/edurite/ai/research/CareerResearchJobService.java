package com.edurite.ai.research;

import static com.edurite.ai.research.CareerResearchDtos.*;

import com.edurite.security.service.CurrentUserService;
import com.edurite.student.repository.StudentProfileRepository;
import com.edurite.subscription.service.AiUsageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.annotation.PreDestroy;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;

/** Durable job metadata/report live in the existing versioned profile JSON; no new schema. */
@Service
public class CareerResearchJobService {
    private final StudentProfileRepository profiles;
    private final CurrentUserService users;
    private final AiUsageService usage;
    private final CareerResearchReportBuilder builder;
    private final ObjectMapper mapper;
    private final TransactionTemplate tx;

    @org.springframework.beans.factory.annotation.Autowired
    private com.edurite.student.service.ProfileGuidanceContext guidanceContext;

    private final ThreadPoolExecutor worker =
            new ThreadPoolExecutor(
                    1,
                    1,
                    0,
                    TimeUnit.SECONDS,
                    new ArrayBlockingQueue<>(2),
                    r -> {
                        Thread t = new Thread(r, "career-research");
                        t.setDaemon(true);
                        return t;
                    },
                    new ThreadPoolExecutor.AbortPolicy());

    public CareerResearchJobService(
            StudentProfileRepository profiles,
            CurrentUserService users,
            AiUsageService usage,
            CareerResearchReportBuilder builder,
            ObjectMapper mapper,
            PlatformTransactionManager transactions) {
        this.profiles = profiles;
        this.users = users;
        this.usage = usage;
        this.builder = builder;
        this.mapper = mapper;
        this.tx = new TransactionTemplate(transactions);
    }

    public JobStatus start(Principal principal) {
        return start(principal, null);
    }

    public JobStatus start(Principal principal, String requestKey) {
        if (requestKey != null)
            try {
                UUID.fromString(requestKey);
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Invalid research request key");
            }
        var user = users.requireUser(principal);
        UUID owner = user.getId();
        var state =
                edit(
                        owner,
                        root -> {
                            ObjectNode job = root.withObject("careerResearchJob");
                            if (requestKey != null
                                    && requestKey.equals(job.path("requestKey").asText())
                                    && "SUCCEEDED".equals(job.path("status").asText()))
                                return new StartDecision(job.deepCopy(), false);
                            if (active(job) && !expired(job))
                                return new StartDecision(job.deepCopy(), false);
                            releaseExpired(job);
                            UUID reservation = usage.reserve(owner);
                            job.removeAll();
                            job.put("jobId", UUID.randomUUID().toString());
                            if (requestKey != null) job.put("requestKey", requestKey);
                            job.put("reservation", reservation.toString());
                            job.put("status", "QUEUED");
                            job.put("stage", "Queued for research");
                            job.put("updatedAt", Instant.now().toString());
                            job.put("createdAt", Instant.now().toString());
                            job.put("evidenceHash", signature(owner));
                            return new StartDecision(job.deepCopy(), true);
                        });
        if (state.dispatch()) {
            String id = state.job().path("jobId").asText();
            try {
                worker.execute(
                        () ->
                                run(
                                        owner,
                                        user.getEmail(),
                                        id,
                                        UUID.fromString(state.job().path("reservation").asText())));
            } catch (RejectedExecutionException ex) {
                fail(
                        owner,
                        id,
                        "Research capacity is busy. Please retry shortly.",
                        UUID.fromString(state.job().path("reservation").asText()));
            }
        }
        return status(principal);
    }

    private record StartDecision(ObjectNode job, boolean dispatch) {}

    public JobStatus status(Principal principal) {
        UUID owner = users.requireUser(principal).getId();
        return edit(
                owner,
                root -> {
                    ObjectNode job =
                            root.get("careerResearchJob") instanceof ObjectNode stored
                                    ? stored
                                    : mapper.createObjectNode();
                    if (active(job) && expired(job)) {
                        releaseExpired(job);
                        job.put("status", "FAILED");
                        job.put("error", "Research was interrupted or timed out. Please retry.");
                    }
                    Report report = null;
                    if (job.path("status").asText().equals("SUCCEEDED")
                            && root.has("careerResearchReport"))
                        try {
                            if (!job.path("evidenceHash").asText().equals(signature(owner))
                                    || Instant.parse(
                                                    root.path("careerResearchReport")
                                                            .path("generatedAt")
                                                            .asText())
                                            .isBefore(Instant.now().minusSeconds(7 * 86400))) {
                                job.put("status", "STALE");
                                job.put(
                                        "error",
                                        "Your saved evidence changed or the research snapshot needs"
                                                + " refreshing. Generate a fresh report.");
                            } else
                                report =
                                        mapper.treeToValue(
                                                root.get("careerResearchReport"), Report.class);
                        } catch (Exception ex) {
                            job.put("status", "FAILED");
                            job.put(
                                    "error",
                                    "Saved research could not be read. Generate a fresh report.");
                        }
                    return new JobStatus(
                            job.path("jobId").asText(null),
                            job.path("status").asText("IDLE"),
                            job.path("stage").asText(null),
                            job.path("updatedAt").asText(null),
                            job.path("error").asText(null),
                            report);
                });
    }

    private void run(UUID owner, String email, String id, UUID reservation) {
        try {
            users.requireUserById(owner);
            update(owner, id, "Gathering evidence");
            Report report = builder.build(() -> email, stage -> update(owner, id, stage));
            validateReport(report);
            edit(
                    owner,
                    root -> {
                        var job = root.withObject("careerResearchJob");
                        requireActive(job, id);
                        if (!job.path("evidenceHash").asText().equals(signature(owner)))
                            throw new IllegalArgumentException("Evidence changed during research");
                        var encoded = mapper.valueToTree(report);
                        if (encoded.toString().length() > 2_000_000)
                            throw new IllegalArgumentException(
                                    "Research report exceeds the safe storage budget; narrow your"
                                            + " preferences.");
                        root.set("careerResearchReport", encoded);
                        job.put("status", "SUCCEEDED");
                        job.put("stage", "Complete");
                        job.put("updatedAt", Instant.now().toString());
                        usage.finish(reservation, true);
                        return null;
                    });
        } catch (Exception ex) {
            fail(
                    owner,
                    id,
                    ex instanceof IllegalArgumentException
                            ? "The report could not be completed or validated. Review your profile"
                                    + " and retry."
                            : "Research could not be completed. Your previous successful report was"
                                    + " retained; please retry.",
                    reservation);
        }
    }

    static void validateReport(Report report) {
        if (report == null
                || !"career-research-v1".equals(report.schemaVersion())
                || report.academic() == null
                || report.careers() == null
                || report.careers().isEmpty()
                || report.programmes() == null
                || report.interpretation() == null
                || report.interpretation().nextSteps() == null
                || report.interpretation().nextSteps().isEmpty()
                || report.sources() == null
                || report.funding() == null
                || report.limitations() == null
                || report.careers().stream()
                        .anyMatch(
                                c ->
                                        c.id() == null
                                                || c.title() == null
                                                || c.title().isBlank()
                                                || c.explanation() == null
                                                || c.explanation().isBlank())) {
            throw new IllegalArgumentException("Incomplete research report");
        }
        Instant.parse(report.generatedAt());
    }

    private void update(UUID owner, String id, String stage) {
        edit(
                owner,
                root -> {
                    var job = root.withObject("careerResearchJob");
                    requireActive(job, id);
                    if (!job.path("evidenceHash").asText().equals(signature(owner)))
                        throw new IllegalArgumentException("Profile changed during research");
                    job.put("status", "RUNNING");
                    job.put("stage", stage);
                    job.put("updatedAt", Instant.now().toString());
                    return null;
                });
    }

    private void requireActive(ObjectNode job, String id) {
        if (!id.equals(job.path("jobId").asText()) || !active(job) || expired(job))
            throw new CancellationException("Research job no longer active");
    }

    private void fail(UUID owner, String id, String message, UUID reservation) {
        edit(
                owner,
                root -> {
                    var job = root.withObject("careerResearchJob");
                    if (id.equals(job.path("jobId").asText())
                            && !job.path("status").asText().equals("SUCCEEDED")) {
                        job.put("status", "FAILED");
                        job.put("error", message);
                        job.put("updatedAt", Instant.now().toString());
                    }
                    usage.finish(reservation, false);
                    return null;
                });
    }

    private boolean active(ObjectNode job) {
        return Set.of("QUEUED", "RUNNING").contains(job.path("status").asText());
    }

    private String signature(UUID owner) {
        var p = profiles.findByUserId(owner).orElseThrow();
        try {
            ObjectNode prefs = (ObjectNode) mapper.readTree(p.getPreferencesJson());
            prefs.remove(List.of("careerResearchJob", "careerResearchReport"));
            String evidence =
                    Objects.toString(p.getSelectedGrade(), "")
                            + "|"
                            + p.getQualificationLevel()
                            + "|"
                            + p.getSubjectAchievementsJson()
                            + "|"
                            + p.getInterests()
                            + "|"
                            + p.getSkills()
                            + "|"
                            + p.getCareerGoals()
                            + "|"
                            + p.getLocation()
                            + "|"
                            + p.getTranscriptFileUrl()
                            + "|"
                            + p.getTranscriptHistoryJson()
                            + "|"
                            + prefs
                            + "|"
                            + (guidanceContext == null ? "" : guidanceContext.describe(p));
            return java.util.HexFormat.of()
                    .formatHex(
                            java.security.MessageDigest.getInstance("SHA-256")
                                    .digest(
                                            evidence.getBytes(
                                                    java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unable to fingerprint saved evidence", ex);
        }
    }

    private boolean expired(ObjectNode job) {
        try {
            return Instant.parse(job.path("createdAt").asText())
                    .isBefore(Instant.now().minusSeconds(600));
        } catch (Exception ex) {
            return true;
        }
    }

    private void releaseExpired(ObjectNode job) {
        if (active(job) && job.hasNonNull("reservation"))
            usage.finish(UUID.fromString(job.path("reservation").asText()), false);
    }

    private <T> T edit(UUID owner, Function<ObjectNode, T> operation) {
        return tx.execute(
                status -> {
                    var profile =
                            profiles.findByUserIdForUpdate(owner)
                                    .orElseThrow(
                                            () ->
                                                    new ResponseStatusException(
                                                            HttpStatus.NOT_FOUND,
                                                            "Student profile not found"));
                    ObjectNode root;
                    try {
                        var parsed = mapper.readTree(profile.getPreferencesJson());
                        if (!(parsed instanceof ObjectNode)) throw new IllegalArgumentException();
                        root = (ObjectNode) parsed;
                    } catch (Exception ex) {
                        throw new ResponseStatusException(
                                HttpStatus.CONFLICT, "Profile preferences cannot be read safely");
                    }
                    ObjectNode before = root.deepCopy();
                    T value = operation.apply(root);
                    if (!before.equals(root)) {
                        profile.setPreferencesJson(root.toString());
                        profiles.save(profile);
                    }
                    return value;
                });
    }

    @PreDestroy
    public void close() {
        worker.shutdownNow();
    }
}
