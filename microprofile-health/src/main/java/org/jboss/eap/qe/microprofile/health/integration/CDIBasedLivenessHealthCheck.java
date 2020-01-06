package org.jboss.eap.qe.microprofile.health.integration;

import javax.inject.Inject;

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
                .state(dummyService.isLive())
                .build();
    }
}
