package org.jboss.eap.qe.microprofile.health.delay;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@Readiness
public class DelayedReadinessHealthCheck implements HealthCheck {
    public static final String NAME = "delayed-readiness";

    public DelayedReadinessHealthCheck() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.down(NAME);
    }
}
