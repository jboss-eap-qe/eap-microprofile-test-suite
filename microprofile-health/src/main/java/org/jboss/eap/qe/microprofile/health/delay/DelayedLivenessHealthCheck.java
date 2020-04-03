package org.jboss.eap.qe.microprofile.health.delay;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

@ApplicationScoped
@Liveness
public class DelayedLivenessHealthCheck implements HealthCheck {
    public static final String NAME = "delayed-liveness";

    public DelayedLivenessHealthCheck() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.named("live").up().withData("name", NAME).build();
    }
}
