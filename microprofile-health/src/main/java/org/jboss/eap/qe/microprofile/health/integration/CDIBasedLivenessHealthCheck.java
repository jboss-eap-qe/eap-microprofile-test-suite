package org.jboss.eap.qe.microprofile.health.integration;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

import jakarta.inject.Inject;

@Liveness
public class CDIBasedLivenessHealthCheck implements HealthCheck {

    @Inject
    FailSafeDummyService dummyService;

    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.named("dummyLiveness")
                .status(dummyService.isLive())
                .build();
    }
}
