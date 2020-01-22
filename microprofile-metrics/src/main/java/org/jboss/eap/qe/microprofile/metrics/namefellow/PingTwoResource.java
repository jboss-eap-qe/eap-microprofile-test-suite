package org.jboss.eap.qe.microprofile.metrics.namefellow;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
