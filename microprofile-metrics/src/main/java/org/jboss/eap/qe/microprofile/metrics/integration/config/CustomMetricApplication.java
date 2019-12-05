package org.jboss.eap.qe.microprofile.metrics.integration.config;

import javax.inject.Inject;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;

@ApplicationPath("/")
public class CustomMetricApplication extends Application {

    @Path("")
    public static class Resource {

        @Inject
        CustomMetricService service;

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String doGet() {
            return service.hello();
        }
    }
}
