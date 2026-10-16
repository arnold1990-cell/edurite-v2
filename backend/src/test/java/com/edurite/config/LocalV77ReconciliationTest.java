package com.edurite.config;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class LocalV77ReconciliationTest {
    @Test
    void beanIsOnlyEnabledForDevelopmentWithoutProduction() {
        for (String[] profiles : new String[][]{{"dev"}, {"prod"}, {"production"}, {"dev", "prod"},
                {"dev", "production"}, {"test"}, {}}) {
            new ApplicationContextRunner().withInitializer(context -> context.getEnvironment().setActiveProfiles(profiles))
                    .withUserConfiguration(LocalV77Reconciliation.class).run(context -> {
                        if (profiles.length == 1 && profiles[0].equals("dev")) {
                            assertThat(context).hasSingleBean(LocalV77Reconciliation.class);
                        } else {
                            assertThat(context).doesNotHaveBean(LocalV77Reconciliation.class);
                        }
                    });
        }
    }

    @Test
    void remoteAndMultiHostDatabasesAreRefused() throws Exception {
        Connection connection = mock(Connection.class);
        DatabaseMetaData metadata = mock(DatabaseMetaData.class);
        when(connection.getMetaData()).thenReturn(metadata);
        for (String url : new String[]{"jdbc:postgresql://production.example:5432/edurite",
                "jdbc:postgresql://localhost.evil.example:5432/edurite",
                "jdbc:postgresql://localhost:5432,production.example:5432/edurite"}) {
            when(metadata.getURL()).thenReturn(url);
            assertThatThrownBy(() -> LocalV77Reconciliation.requireLocal(connection)).isInstanceOf(RuntimeException.class);
        }
        for (String host : new String[]{"localhost", "127.0.0.1", "[::1]"}) {
            when(metadata.getURL()).thenReturn("jdbc:postgresql://" + host + ":5432/edurite");
            LocalV77Reconciliation.requireLocal(connection);
        }
    }

    @Test
    void autocommitCannotRunReconciliation() throws Exception {
        Context context = mock(Context.class);
        Connection connection = mock(Connection.class);
        when(context.getConnection()).thenReturn(connection);
        when(connection.getAutoCommit()).thenReturn(true);
        assertThatThrownBy(() -> new LocalV77Reconciliation().handle(Event.BEFORE_EACH_MIGRATE, context))
                .hasMessageContaining("requires the migration transaction");
        verify(connection, never()).createStatement();
    }
}
