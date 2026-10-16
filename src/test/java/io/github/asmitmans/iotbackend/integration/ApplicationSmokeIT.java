package io.github.asmitmans.iotbackend.integration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApplicationSmokeIT extends AbstractIntegrationTest{

    @Test
    void contextLoadsAndFlywayMigratesCleanly() {
        assertThat(POSTGRES.isRunning()).isTrue();
    }
}

