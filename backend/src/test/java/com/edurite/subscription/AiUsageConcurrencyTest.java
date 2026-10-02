package com.edurite.subscription;
import com.edurite.subscription.service.*;
import com.edurite.subscription.entity.PlanType;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
@Testcontainers(disabledWithoutDocker=true)
class AiUsageConcurrencyTest {
    @Container static PostgreSQLContainer<?> postgres=new PostgreSQLContainer<>("postgres:16-alpine");
    @Test void simultaneousRequestsCannotExceedAllowanceAndFailuresReleaseSlots() throws Exception {
        var ds=new DriverManagerDataSource(postgres.getJdbcUrl(),postgres.getUsername(),postgres.getPassword());
        var jdbc=new JdbcTemplate(ds);var tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        jdbc.execute("create table users(id uuid primary key)");
        jdbc.execute("create table student_ai_usage(id uuid primary key,user_id uuid,period_start date,status varchar(16),created_at timestamptz)");
        UUID id=UUID.randomUUID();jdbc.update("insert into users values (?)",id);
        var entitlements=mock(EntitlementService.class);when(entitlements.plan(id)).thenReturn(PlanType.BASIC);
        var usage=new AiUsageService(jdbc,entitlements);
        var ids=new ConcurrentLinkedQueue<UUID>();
        try(var pool=Executors.newFixedThreadPool(12)) {
            List<Callable<Boolean>> calls=new ArrayList<>();
            for(int i=0;i<20;i++) calls.add(()->{try{ids.add(tx.execute(status->usage.reserve(id)));return true;}catch(org.springframework.web.server.ResponseStatusException ex){return false;}});
            long admitted=pool.invokeAll(calls).stream().filter(f->{try{return f.get();}catch(Exception ex){throw new RuntimeException(ex);}}).count();
            assertThat(admitted).isEqualTo(5);
        }
        assertThat(usage.current(id).remaining()).isZero();
        tx.executeWithoutResult(s->usage.finish(ids.poll(),false));
        assertThat(usage.current(id).remaining()).isEqualTo(1);
        tx.executeWithoutResult(s->usage.finish(ids.poll(),true));
        assertThat(usage.current(id).used()).isEqualTo(1);
        jdbc.update("update student_ai_usage set period_start=period_start - interval '1 month'");
        assertThat(usage.current(id).remaining()).isEqualTo(5);
    }
}
