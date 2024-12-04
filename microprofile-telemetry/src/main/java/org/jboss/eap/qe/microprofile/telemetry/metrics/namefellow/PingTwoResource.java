package org.jboss.eap.qe.microprofile.telemetry.metrics.namefellow;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/" + PingTwoResource.RESOURCE)
public class PingTwoResource {
    public static final String RESOURCE = "ping-one";

    @Inject
    private PingTwoService ping;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String doGet() {
        return ping.ping();
    }
}
