package com.erd.core;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

/**
 * {@link CoreApplicationTests} loads the application context; this test covers the {@code main}
 * entry point itself, which is otherwise never executed by the suite. {@code SpringApplication.run}
 * is stubbed so no second context is started.
 */
class CoreApplicationMainTest {

    @Test
    void testMain_bootstrapsTheApplication() {
        // Given
        String[] args = {"--server.port=0"};

        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            springApplication
                    .when(() -> SpringApplication.run(any(Class.class), any(String[].class)))
                    .thenReturn(mock(ConfigurableApplicationContext.class));

            // When
            CoreApplication.main(args);

            // Then
            springApplication.verify(() -> SpringApplication.run(CoreApplication.class, args));
        }
    }

    @Test
    void testApplicationClass_enablesSchedulingAndAutoConfiguration() {
        // When & Then
        assertNotNull(CoreApplication.class.getAnnotation(SpringBootApplication.class));
        assertNotNull(CoreApplication.class.getAnnotation(EnableScheduling.class),
                "The stale-lock reaper in CollaborationService depends on @EnableScheduling");
        assertNotNull(new CoreApplication());
    }

}
