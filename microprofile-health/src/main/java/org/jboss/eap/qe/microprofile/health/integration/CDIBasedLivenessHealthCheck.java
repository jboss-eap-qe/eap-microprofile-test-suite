package org.jboss.eap.qe.microprofile.health.integration;

import jakarta.inject.Inject;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

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
