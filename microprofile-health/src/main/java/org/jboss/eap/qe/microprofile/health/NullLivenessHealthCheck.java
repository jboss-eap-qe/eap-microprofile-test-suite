package org.jboss.eap.qe.microprofile.health;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

@Liveness
public class NullLivenessHealthCheck implements HealthCheck {
    @Override
    public HealthCheckResponse call() {
        return null;
    }
}
