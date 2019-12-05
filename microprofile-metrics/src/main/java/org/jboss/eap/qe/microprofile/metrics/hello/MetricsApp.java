package org.jboss.eap.qe.microprofile.metrics.hello;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;

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
