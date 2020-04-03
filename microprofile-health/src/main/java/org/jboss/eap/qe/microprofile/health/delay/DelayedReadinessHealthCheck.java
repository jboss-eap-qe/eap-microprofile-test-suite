package org.jboss.eap.qe.microprofile.health.delay;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

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
