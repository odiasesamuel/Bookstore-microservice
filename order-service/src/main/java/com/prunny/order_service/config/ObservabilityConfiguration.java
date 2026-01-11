package com.prunny.order_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.observation.SecurityObservationSettings;

@Configuration
public class ObservabilityConfiguration {
    /**
     * Disables Spring Security "filterchain before/after", authn/authz spans.
     * Keeps normal HTTP server spans + Feign/client spans in zipkin.
     * You can still keep track of security-related metrics via Micrometer. Because those security spans are often the fastest way to spot “why is my gateway slow?” (JWT parsing, auth manager, filter ordering, etc.)
     */
    @Bean
    SecurityObservationSettings noSpringSecurityObservations() {
        return SecurityObservationSettings.noObservations();
    }
}
