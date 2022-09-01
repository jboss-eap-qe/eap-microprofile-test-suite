package org.jboss.eap.qe.microprofile.metrics.integration.ft;

import jakarta.inject.Inject;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;

@ApplicationPath("/")
public class FTCustomMetricApplication extends Application {

    @Path("")
    public static class Resource {

        @Inject
        FTCustomMetricService service;

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String doGet() {
            return service.hello();
        }
    }
}
