package com.edurite.subscription;

import com.edurite.subscription.service.*;
import com.edurite.subscription.entity.*;
import com.edurite.subscription.repository.SubscriptionRepository;
import com.edurite.security.service.CurrentUserService;
import com.edurite.user.entity.*;
import com.edurite.user.repository.UserRepository;
import com.edurite.common.exception.ApiExceptionHandler;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class EntitlementSecurityTest {
    final SubscriptionRepository subscriptions=mock(SubscriptionRepository.class);
    final UserRepository users=mock(UserRepository.class);
    final StudentPlanAccessService access=new StudentPlanAccessService(subscriptions,users);
    final EntitlementService entitlements=new EntitlementService(access);
    final CurrentUserService current=mock(CurrentUserService.class);
    final AiUsageService usage=mock(AiUsageService.class);
    final User user=new User();
    SubscriptionRecord subscription;
    MockMvc mvc;
    @BeforeEach void setup() {
        user.setId(UUID.randomUUID()); user.setPlanType(PlanType.PRO); // A cached user plan must not grant access.
        Role role=new Role(); role.setName("STUDENT"); user.setRoles(Set.of(role));
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        when(current.requireUser(any())).thenReturn(user);
        subscription=new SubscriptionRecord(); subscription.setPlanCode("PLAN_BASIC"); subscription.setStatus("ACTIVE");
        subscription.setEndDate(LocalDate.now(ZoneOffset.UTC).plusMonths(1));
        when(subscriptions.findTopByUserIdOrderByCreatedAtDesc(user.getId())).thenReturn(Optional.of(subscription));
        mvc=MockMvcBuilders.standaloneSetup(new Endpoints()).setControllerAdvice(new ApiExceptionHandler())
            .addInterceptors(new SubscriptionAccessInterceptor(current,entitlements,usage)).build();
    }
    @RestController static class Endpoints {
        @RequestMapping({"/api/student/career-roadmaps/generate","/api/v1/student/career-roadmaps/generate","/api/student/cv","/api/ai/analyse-university-sources","/api/student/psychometric/latest","/api/student/tutor/ask","/api/student/aps/calculate","/api/careers"})
        Map<String,String> response() { return Map.of("result","allowed"); }
    }
    @ParameterizedTest @EnumSource(PlanType.class)
    void completePlanMatrix(PlanType plan) {
        subscription.setPlanCode("PLAN_"+plan);
        for(Feature feature:Feature.values()) assertThat(entitlements.hasFeature(user.getId(),feature)).as(plan+":"+feature).isEqualTo(plan.ordinal()>=feature.minimumPlan.ordinal());
        assertThat(EntitlementService.allowance(plan)).isEqualTo(switch(plan){case BASIC->5;case PREMIUM->30;case PRO->100;});
    }
    @ParameterizedTest @ValueSource(strings={"CANCELLED","EXPIRED","PAYMENT_FAILED","PAST_DUE","PENDING"})
    void inactivePaidPlanFallsBack(String status) {
        subscription.setPlanCode("PLAN_PRO"); subscription.setStatus(status);
        subscription.setTrialEndDate(OffsetDateTime.now().plusDays(10));
        assertThat(entitlements.plan(user.getId())).isEqualTo(PlanType.BASIC);
    }
    @Test void expiredActivePlanFallsBack() { subscription.setPlanCode("PLAN_PRO");subscription.setEndDate(LocalDate.now(ZoneOffset.UTC));assertThat(entitlements.plan(user.getId())).isEqualTo(PlanType.BASIC); }
    @Test void futurePaidPeriodDoesNotGrantAccess() { subscription.setPlanCode("PLAN_PRO");subscription.setStartDate(LocalDate.now(ZoneOffset.UTC).plusDays(1));assertThat(entitlements.plan(user.getId())).isEqualTo(PlanType.BASIC); }
    @ParameterizedTest @ValueSource(strings={"PLAN_PRO_FAKE","PLAN_PREMIUM_UNKNOWN","PROFESSIONAL"})
    void unknownPlanNamesFailClosed(String code) { subscription.setPlanCode(code);assertThat(entitlements.plan(user.getId())).isEqualTo(PlanType.BASIC); }
    @Test void expiredPaidPeriodCannotReuseUnrelatedTrialDates() { subscription.setPlanCode("PLAN_PRO");subscription.setEndDate(LocalDate.now(ZoneOffset.UTC));subscription.setTrialEndDate(OffsetDateTime.now().plusDays(10));assertThat(entitlements.plan(user.getId())).isEqualTo(PlanType.BASIC); }
    @Test void adminWithStudentRoleIsNotSubjectToStudentBilling() throws Exception {
        Role admin=new Role();admin.setName("ADMIN");Role student=new Role();student.setName("STUDENT");user.setRoles(Set.of(admin,student));
        mvc.perform(post("/api/student/tutor/ask")).andExpect(status().isOk());verifyNoInteractions(usage);
    }
    @Test void missingSubscriptionIsBasic() { when(subscriptions.findTopByUserIdOrderByCreatedAtDesc(any())).thenReturn(Optional.empty());assertThat(entitlements.plan(user.getId())).isEqualTo(PlanType.BASIC); }
    @Test void cancelAtPeriodEndRetainsAccessUntilExpiry() { subscription.setPlanCode("PLAN_PREMIUM");subscription.setCancelAtPeriodEnd(true);assertThat(entitlements.plan(user.getId())).isEqualTo(PlanType.PREMIUM);subscription.setEndDate(LocalDate.now(ZoneOffset.UTC));assertThat(entitlements.plan(user.getId())).isEqualTo(PlanType.BASIC); }
    @ParameterizedTest @ValueSource(strings={"/api/student/career-roadmaps/generate","/api/v1/student/career-roadmaps/generate","/api/student/cv","/api/ai/analyse-university-sources","/api/student/psychometric/latest"})
    void directBasicApiDeniedEvenWithForgedPlan(String path) throws Exception { mvc.perform(post(path).principal(()->"student@example.com").param("plan","PRO").header("X-Plan","PRO")).andExpect(status().isForbidden());verifyNoInteractions(usage); }
    @Test void premiumCannotCallProApi() throws Exception {subscription.setPlanCode("PLAN_PREMIUM");mvc.perform(post("/api/ai/analyse-university-sources")).andExpect(status().isForbidden());mvc.perform(get("/api/student/cv")).andExpect(status().isForbidden());}
    @Test void basicApsAndCareerRemainAvailable() throws Exception {mvc.perform(post("/api/student/aps/calculate")).andExpect(status().isOk());mvc.perform(get("/api/careers")).andExpect(status().isOk());}
    @Test void successfulAiRequestIsReservedAndCounted() throws Exception {UUID reservation=UUID.randomUUID();when(usage.reserve(user.getId())).thenReturn(reservation);mvc.perform(post("/api/student/tutor/ask")).andExpect(status().isOk());verify(usage).finish(reservation,true);}
    @Test void quotaDenialDoesNotExecuteAi() throws Exception {when(usage.reserve(user.getId())).thenThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS,"Quota exceeded"));mvc.perform(post("/api/student/tutor/ask")).andExpect(status().isTooManyRequests());verify(usage,never()).finish(any(),anyBoolean());}
    @ParameterizedTest @ValueSource(strings={"ADMIN","SCHOOL_ADMIN","TEACHER","DISTRICT_ADMIN","CIRCUIT_MANAGER","SUBJECT_ADVISOR"})
    void nonStudentRolesAreNotCharged(String name) throws Exception {Role role=new Role();role.setName(name);user.setRoles(Set.of(role));mvc.perform(post("/api/student/tutor/ask")).andExpect(status().isOk());verifyNoInteractions(usage);}
    @Test void utcMonthlyPeriodRollsOverWithoutScheduledJob() {assertThat(AiUsageService.period(Instant.parse("2026-01-31T23:59:59Z"))).isEqualTo(LocalDate.of(2026,1,1));assertThat(AiUsageService.period(Instant.parse("2026-02-01T00:00:00Z"))).isEqualTo(LocalDate.of(2026,2,1));}
}
