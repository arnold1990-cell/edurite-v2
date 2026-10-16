package com.edurite.subscription.service;

import com.edurite.security.service.CurrentUserService;
import com.edurite.subscription.entity.AdminSubscriptionChange;
import com.edurite.subscription.entity.PlanType;
import com.edurite.subscription.entity.SubscriptionRecord;
import com.edurite.subscription.repository.AdminSubscriptionChangeRepository;
import com.edurite.subscription.repository.SubscriptionRepository;
import com.edurite.user.entity.User;
import com.edurite.user.repository.UserRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.security.Principal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class AdminSubscriptionService {
    private static final Logger log = LoggerFactory.getLogger(AdminSubscriptionService.class);
    private final CurrentUserService currentUser;
    private final UserRepository users;
    private final SubscriptionRepository subscriptions;
    private final AdminSubscriptionChangeRepository history;
    private final StudentPlanAccessService access;
    private final com.edurite.admin.repository.AuditLogRepository auditLogs;

    public AdminSubscriptionService(CurrentUserService currentUser, UserRepository users,
            SubscriptionRepository subscriptions, AdminSubscriptionChangeRepository history, StudentPlanAccessService access,
            com.edurite.admin.repository.AuditLogRepository auditLogs) {
        this.currentUser = currentUser;
        this.users = users;
        this.subscriptions = subscriptions;
        this.history = history;
        this.access = access;
        this.auditLogs = auditLogs;
    }

    public Page<Summary> list(Principal principal, String search, int page, int size) {
        requireAdmin(principal);
        return users.searchForAdmin(search == null ? "" : search.trim(), "", "", "", false,
                pagination(page, size)).map(this::summary);
    }

    public Summary detail(Principal principal, UUID userId) {
        requireAdmin(principal);
        return summary(requireTarget(userId));
    }

    public Page<AdminSubscriptionChange> history(Principal principal, UUID userId, int page, int size) {
        requireAdmin(principal);
        requireTarget(userId);
        return history.findByUserIdOrderByCreatedAtDesc(userId, pagination(page, size));
    }

    @Transactional
    public Summary change(Principal principal, UUID userId, ChangeRequest request) {
        User administrator = requireAdmin(principal);
        if (request == null || request.plan() == null || request.reason() == null
                || request.reason().isBlank() || request.reason().length() > 1000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid plan and reason are required");
        }
        User user = users.lockForSubscription(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getDeletedAt() != null) throw new ResponseStatusException(HttpStatus.CONFLICT, "Deleted accounts cannot receive overrides");
        String previous = access.getCurrentPlan(userId).name();
        // Retire only previous platform overrides. Paid billing agreements remain untouched.
        for (SubscriptionRecord record : subscriptions.findByUserIdOrderByCreatedAtDesc(userId)) {
            if (StudentPlanAccessService.isAdminOverride(record)) {
                record.setStatus("CANCELLED");
                subscriptions.save(record);
            }
        }
        subscriptions.flush(); // Release the partial unique index before inserting the replacement.
        SubscriptionRecord record = new SubscriptionRecord();
        record.setUserId(userId);
        record.setPlanCode("PLAN_" + request.plan().name());
        record.setProvider("ADMIN_OVERRIDE");
        record.setStatus("ACTIVE");
        record.setStartDate(LocalDate.now(ZoneOffset.UTC));
        record.setTrialUsed(true);
        subscriptions.saveAndFlush(record);
        user.setPlanType(request.plan());
        users.save(user);
        AdminSubscriptionChange change = new AdminSubscriptionChange();
        change.setUserId(userId);
        change.setAdministratorId(administrator.getId());
        change.setAdministratorEmail(administrator.getEmail());
        change.setSubscriptionId(record.getId());
        change.setPreviousPlan(previous);
        change.setNewPlan(request.plan().name());
        change.setReason(request.reason().trim());
        history.save(change);
        var audit = new com.edurite.admin.entity.AuditLog();
        audit.setActorId(administrator.getId());
        audit.setAction("ADMIN_SUBSCRIPTION_OVERRIDE");
        audit.setEntityType("USER");
        audit.setEntityId(userId);
        audit.setDetails(com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode()
                .put("previousPlan", previous).put("newPlan", request.plan().name()).put("reason", request.reason().trim()));
        auditLogs.save(audit);
        log.info("Admin subscription override: administratorId={}, userId={}, previousPlan={}, newPlan={}",
                administrator.getId(), userId, previous, request.plan());
        return summary(user);
    }

    private User requireAdmin(Principal principal) {
        User user = currentUser.requireUser(principal);
        if (user.getRoles().stream().noneMatch(role -> Set.of("ADMIN", "SUPER_ADMIN", "ROLE_ADMIN", "ROLE_SUPER_ADMIN")
                .contains(role.getName()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrator permission required");
        }
        return user;
    }

    private User requireTarget(UUID id) {
        return users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private Summary summary(User user) {
        SubscriptionRecord record = access.currentRecord(user.getId());
        var effective = access.resolveByUserId(user.getId());
        String source = record == null ? "PROMOTION" : StudentPlanAccessService.isAdminOverride(record)
                ? "ADMIN_OVERRIDE" : Set.of("paypal", "payfast", "stripe").contains(String.valueOf(record.getProvider()))
                ? "PAYMENT" : "PROMOTION";
        return new Summary(user.getId(), user.getFirstName() + " " + user.getLastName(), user.getEmail(),
                user.getUsername(), PlanType.fromPlanCode(effective.planCode()).name(), source, effective.status(), record);
    }

    private PageRequest pagination(int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination");
        return PageRequest.of(page, size);
    }

    public record ChangeRequest(@NotNull PlanType plan, @NotBlank @Size(max = 1000) String reason) {}
    public record Summary(UUID userId, String fullName, String email, String username, String currentPlan,
                          String source, String status, SubscriptionRecord subscription) {}
}
