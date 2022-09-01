package org.jboss.eap.qe.microprofile.fault.tolerance.deployments.nofaulttolerance;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Simple deployment without MP FT stuff. Used to test that MP FT subsystem does not get activated.
 */
@ApplicationScoped
public class HelloService {
    public String ping() {
        return "Pong from HelloService";
    }
}
