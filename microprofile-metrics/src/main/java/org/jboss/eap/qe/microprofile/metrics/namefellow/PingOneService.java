package org.jboss.eap.qe.microprofile.metrics.namefellow;

import org.eclipse.microprofile.metrics.annotation.Counted;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PingOneService {
    public static final String MESSAGE = "pong one";
    public static final String PING_ONE_SERVICE_TAG = "ping-one-service-tag";

    @Counted(name = "ping-count", absolute = true, displayName = "Pong Count", description = "Number of ping invocations", tags = "_app="
            + PING_ONE_SERVICE_TAG)
    public String ping() {
        return MESSAGE;
    }
}
