package org.jboss.eap.qe.microprofile.metrics.namefellow;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.metrics.annotation.Counted;

@ApplicationScoped
public class PingOneService {
    @Counted(name = "ping-count", absolute = true, displayName = "Pong Count", description = "Number of ping invocations")
    public String ping() {
        return "pong one";
    }
}
