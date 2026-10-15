package com.edurite.config;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class FlywayRuntimeConfigTest {
    @Test void validationFailureStopsMigrationWithoutRepairingHistory() {
        var flyway = mock(Flyway.class);
        doThrow(new IllegalStateException("checksum mismatch")).when(flyway).migrate();
        assertThatThrownBy(() -> new FlywayRuntimeConfig().flywayMigrationStrategy().migrate(flyway))
                .hasMessage("checksum mismatch");
        verify(flyway, never()).repair();
        verify(flyway, never()).validate();
    }
}
