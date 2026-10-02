package com.edurite.subscription.service;

import java.time.*;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Reserve before execution, commit only successful responses, release failures. UTC calendar months. */
@Service
public class AiUsageService {
    private final JdbcTemplate jdbc;
    private final EntitlementService entitlements;
    public AiUsageService(JdbcTemplate jdbc, EntitlementService entitlements) { this.jdbc = jdbc; this.entitlements = entitlements; }
    public static LocalDate period(Instant instant) { return instant.atZone(ZoneOffset.UTC).toLocalDate().withDayOfMonth(1); }
    public record Usage(int allowance, int used, int pending, int remaining, LocalDate periodStart, LocalDate periodEnd) {}
    public Usage current(UUID userId) { return usage(userId, period(Instant.now())); }
    private Usage usage(UUID userId, LocalDate month) {
        int allowance = EntitlementService.allowance(entitlements.plan(userId));
        Integer used = jdbc.queryForObject("select count(*) from student_ai_usage where user_id=? and period_start=? and status='SUCCEEDED'", Integer.class, userId, month);
        Integer pending = jdbc.queryForObject("select count(*) from student_ai_usage where user_id=? and period_start=? and status='RESERVED'", Integer.class, userId, month);
        return new Usage(allowance, used, pending, Math.max(0, allowance-used-pending), month, month.plusMonths(1));
    }
    @Transactional
    public UUID reserve(UUID userId) {
        // The existing user row is a cross-instance lock, including the first request in a new month.
        jdbc.queryForObject("select id from users where id=? for update", UUID.class, userId);
        LocalDate month = period(Instant.now());
        Usage usage = usage(userId, month);
        if (usage.remaining() == 0) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Monthly AI allowance exhausted. View plans or wait until " + usage.periodEnd() + ".");
        UUID id = UUID.randomUUID();
        jdbc.update("insert into student_ai_usage(id,user_id,period_start,status,created_at) values (?,?,?,'RESERVED',CURRENT_TIMESTAMP)", id, userId, month);
        return id;
    }
    @Transactional
    public void finish(UUID reservation, boolean successful) {
        jdbc.update("update student_ai_usage set status=? where id=? and status='RESERVED'", successful ? "SUCCEEDED" : "FAILED", reservation);
    }
}
