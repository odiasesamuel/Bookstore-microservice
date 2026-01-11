package com.prunny.book_service.config;

import io.micrometer.core.instrument.binder.grpc.ObservationGrpcServerInterceptor;
import io.micrometer.observation.ObservationRegistry;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
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

    /**
     * Optional: explicit gRPC server tracing/observation interceptor.
     *
     * Note: In this project, Micrometer (via auto-configuration) already registers a global
     * ObservationGrpcServerInterceptor (see startup logs: "globalObservationGrpcServerInterceptorConfigurer").
     * If you also register this bean, you will get duplicate server spans in Zipkin.
     *
     * Keep this here for learning; leave it commented out unless you disable the auto-config.
     */
//    @Bean
//    @GrpcGlobalServerInterceptor
//    ObservationGrpcServerInterceptor observationGrpcServerInterceptor(ObservationRegistry observationRegistry) {
//        return new ObservationGrpcServerInterceptor(observationRegistry);
//    }
}
