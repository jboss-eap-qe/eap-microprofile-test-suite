package org.jboss.eap.qe.microprofile.metrics.namefellow;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/" + PingOneResource.RESOURCE)
public class PingOneResource {
    public static final String RESOURCE = "ping-one";

    @Inject
    private PingOneService ping;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String doGet() {
        return ping.ping();
    }
}
