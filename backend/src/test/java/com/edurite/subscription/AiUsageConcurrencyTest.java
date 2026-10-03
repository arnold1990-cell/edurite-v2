package com.edurite.subscription;
import com.edurite.subscription.service.*;
import com.edurite.subscription.entity.PlanType;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.DockerClientFactory;
import org.springframework.test.context.junit.jupiter.EnabledIf;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
@EnabledIf(expression = "#{T(com.edurite.subscription.AiUsageConcurrencyTest).canRunWithDatabase()}")
class AiUsageConcurrencyTest {
    private static PostgreSQLContainer<?> postgres;
    private static final String EXTERNAL_URL = System.getenv("EDURITE_TEST_DB_URL");
    private static boolean external() { return EXTERNAL_URL != null && !EXTERNAL_URL.isBlank(); }
    public static boolean canRunWithDatabase() {
        return external() || DockerClientFactory.instance().isDockerAvailable();
    }
    private static synchronized DriverManagerDataSource database() {
        if (external()) return new DriverManagerDataSource(EXTERNAL_URL,
                System.getenv().getOrDefault("EDURITE_TEST_DB_USERNAME", "postgres"),
                System.getenv().getOrDefault("EDURITE_TEST_DB_PASSWORD", ""));
        if (postgres == null) { postgres = new PostgreSQLContainer<>("postgres:16-alpine"); postgres.start(); }
        return new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }
    @AfterAll static void stopContainer() { if (postgres != null) postgres.stop(); }

    @ParameterizedTest @EnumSource(PlanType.class)
    void simultaneousRequestsCannotExceedAllowanceAndFailuresReleaseSlots(PlanType plan) throws Exception {
        var base = database();
        String schema = "quota_test_" + UUID.randomUUID().toString().replace("-", "");
        var admin = new JdbcTemplate(base);
        admin.execute("create schema " + schema);
        try {
        var ds = new DriverManagerDataSource(base.getUrl() + (base.getUrl().contains("?") ? "&" : "?") + "currentSchema=" + schema,
                base.getUsername(), base.getPassword());
        var jdbc=new JdbcTemplate(ds);var tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        jdbc.execute("create table users(id uuid primary key)");
        jdbc.execute("create table student_ai_usage(id uuid primary key,user_id uuid,period_start date,status varchar(16),created_at timestamptz)");
        UUID id=UUID.randomUUID();jdbc.update("insert into users values (?)",id);
        var entitlements=mock(EntitlementService.class);when(entitlements.plan(id)).thenReturn(plan);
        int allowance = EntitlementService.allowance(plan);
        var usage=new AiUsageService(jdbc,entitlements);
        var ids=new ConcurrentLinkedQueue<UUID>();
        try(var pool=Executors.newFixedThreadPool(12)) {
            List<Callable<Boolean>> calls=new ArrayList<>();
            for(int i=0;i<allowance+15;i++) calls.add(()->{try{ids.add(tx.execute(status->usage.reserve(id)));return true;}catch(org.springframework.web.server.ResponseStatusException ex){assertThat(ex.getStatusCode().value()).isEqualTo(429);return false;}});
            long admitted=pool.invokeAll(calls).stream().filter(f->{try{return f.get();}catch(Exception ex){throw new RuntimeException(ex);}}).count();
            assertThat(admitted).isEqualTo(allowance);
        }
        assertThat(usage.current(id).remaining()).isZero();
        tx.executeWithoutResult(s->usage.finish(ids.poll(),false));
        assertThat(usage.current(id).remaining()).isEqualTo(1);
        tx.executeWithoutResult(s->usage.finish(ids.poll(),true));
        assertThat(usage.current(id).used()).isEqualTo(1);
        jdbc.update("update student_ai_usage set period_start=period_start - interval '1 month'");
        assertThat(usage.current(id).remaining()).isEqualTo(allowance);

        // Race completions against fresh reservations: only the monthly allowance can succeed.
        try(var pool=Executors.newFixedThreadPool(12)) {
            List<Callable<Boolean>> calls=new ArrayList<>();
            for(int i=0;i<allowance+30;i++) calls.add(() -> {
                try {
                    UUID reservation=tx.execute(status -> usage.reserve(id));
                    tx.executeWithoutResult(status -> usage.finish(reservation,true));
                    return true;
                } catch(org.springframework.web.server.ResponseStatusException ex) {
                    assertThat(ex.getStatusCode().value()).isEqualTo(429); return false;
                }
            });
            long admitted=pool.invokeAll(calls).stream().filter(f->{try{return f.get();}catch(Exception ex){throw new RuntimeException(ex);}}).count();
            assertThat(admitted).isEqualTo(allowance);
        }
        assertThat(usage.current(id).used()).isEqualTo(allowance);
        assertThat(usage.current(id).pending()).isZero();
        } finally { admin.execute("drop schema " + schema + " cascade"); }
    }
}
