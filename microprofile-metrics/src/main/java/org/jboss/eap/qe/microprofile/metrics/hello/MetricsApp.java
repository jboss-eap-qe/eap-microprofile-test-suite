package org.jboss.eap.qe.microprofile.metrics.hello;

import jakarta.inject.Inject;
import jakarta.json.JsonObject;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;

@ApplicationPath("/")
public class MetricsApp extends Application {

    @Path("/")
    public static class HelloResource {

        @Inject
        HelloService hello;

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String doGet() throws InterruptedException {
            return hello.hello();
        }
    }

    @Path("/another-hello")
    public static class AnotherHelloResource {

        @Inject
        AnotherHelloService anotherHello;

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String doGet() throws InterruptedException {
            return anotherHello.hello();
        }
    }

    @Path("/summary/{of}")
    public static class MetricsSummaryResource {
        @Inject
        private MetricsSummary metricsSummary;

        @GET
        @Produces(MediaType.APPLICATION_JSON)
        public JsonObject doGet(@PathParam("of") String of) {
            switch (of) {
                case "all-registries":
                    return metricsSummary.summarizeAllRegistries();
                case "app-registry":
                    return metricsSummary.summarizeAppRegistry();
                default:
                    return null;
            }
        }
    }
}
