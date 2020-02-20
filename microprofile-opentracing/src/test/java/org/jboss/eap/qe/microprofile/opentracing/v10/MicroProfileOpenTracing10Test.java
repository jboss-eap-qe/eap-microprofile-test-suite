package org.jboss.eap.qe.microprofile.opentracing.v10;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.eap.qe.microprofile.opentracing.MicroProfileOpenTracingServerHelper;
import org.jboss.eap.qe.ts.common.docker.Docker;
import org.jboss.eap.qe.ts.common.docker.DockerContainers;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.opentracing.Tracer;
import io.restassured.RestAssured;

@RunWith(Arquillian.class)
public class MicroProfileOpenTracing10Test {

    private static final String APPLICATION_NAME = MicroProfileOpenTracing10Test.class.getSimpleName();

    @ClassRule
    public static Docker jaegerContainer = DockerContainers.jaeger();

    @ArquillianResource
    private URL baseApplicationUrl;

    @BeforeClass
    public static void reloadServer() throws Exception {
        MicroProfileOpenTracingServerHelper.reload();
    }

    @Deployment
    public static Archive<?> deployment() {
        String mpConfig = "sampler.param=1\n" + // sample every trace
                "sampler.type=const\n"; // use the same decision strategy for all traces

        return ShrinkWrap.create(WebArchive.class, APPLICATION_NAME + ".war")
                .addPackage(RestApplication.class.getPackage())
                .addPackage(Tracer.class.getPackage())
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsManifestResource(new StringAsset(mpConfig), "microprofile-config.properties");
    }

    /**
     * @tpTestDetails Send a request to deployed MP OT application on /rest endpoint.
     * @tpPassCrit Verify that deployed MP OT application is available and returns expected response from traced service.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @InSequence(1)
    @RunAsClient
    public void testApplicationRequest() {
        String response = RestAssured.when().get(baseApplicationUrl + "rest").asString();
        assertThat(response, equalTo("Hello from traced service"));
    }

    /**
     * @tpTestDetails Jaeger server should have recorded traces of traced service initiated by
     *                the request from testApplicationRequest() test.
     * @tpPassCrit Traced service traces are recorded on Jaeger server and associated with the deployed MP OT application.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @InSequence(2)
    @RunAsClient
    public void testTracePresence() {
        // the tracer inside the application doesn't send traces to the Jaeger server immediately,
        // they are batched, so we need to wait a bit
        await().atMost(20, TimeUnit.SECONDS).untilAsserted(() -> {
            String response = RestAssured.when()
                    .get("http://localhost:16686/api/traces?service=" + APPLICATION_NAME + ".war").asString();

            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            assertThat(json.has("data"), is(true));

            JsonArray data = json.getAsJsonArray("data");
            assertThat(data.size(), is(equalTo(1)));

            JsonObject trace = data.get(0).getAsJsonObject();
            assertThat(trace.has("spans"), is(true));

            JsonArray spans = trace.getAsJsonArray("spans");
            assertThat(spans.size(), is(equalTo(2)));

            List<JsonElement> spanList = new ArrayList<>();
            spans.forEach(spanList::add);

            assertThat(spanList.stream().anyMatch(element -> serviceTraceIsPresented(element)), is(true));
            assertThat(spanList.stream().anyMatch(element -> resourceTraceIsPresented(element)), is(true));
        });
    }

    /**
     * Check whether recorded spans contain MyService traces.
     * 
     * @param element - span recorded by Jaeger.
     * @return true in case MyService traces are registered by Jaeger, otherwise false.
     */
    private boolean serviceTraceIsPresented(JsonElement element) {
        JsonObject span = element.getAsJsonObject();
        if ((span.get("operationName").getAsString()).equalsIgnoreCase(MyService.class.getName() + ".hello")) {

            if (span.has("logs") && span.getAsJsonArray("logs").size() == 1) {
                JsonArray logs = span.getAsJsonArray("logs");
                JsonObject log = logs.get(0).getAsJsonObject();

                return log.getAsJsonArray("fields")
                        .get(0)
                        .getAsJsonObject().get("value")
                        .getAsString()
                        .equalsIgnoreCase("Hello tracer");
            }
        }
        return false;
    }

    /**
     * Check whether recorded spans contain RestSimpleResource traces.
     * 
     * @param element - span recorded by Jaeger.
     * @return true in case RestSimpleResource traces are registered by Jaeger, otherwise false.
     */
    private boolean resourceTraceIsPresented(JsonElement element) {
        JsonObject span = element.getAsJsonObject();
        if ((span.get("operationName").getAsString())
                .equalsIgnoreCase("GET:" + RestSimpleResource.class.getName() + ".tracedOperation")) {

            if (span.has("tags")) {
                JsonArray tags = span.getAsJsonArray("tags");
                boolean traceIsPresented = false;
                for (JsonElement tagElement : tags) {
                    JsonObject tag = tagElement.getAsJsonObject();
                    switch (tag.get("key").getAsString()) {
                        case "http.method":
                            traceIsPresented = tag.get("value").getAsString().equalsIgnoreCase("GET");
                            break;
                        case "http.url":
                            traceIsPresented = tag.get("value").getAsString().equalsIgnoreCase(baseApplicationUrl + "rest");
                            break;
                        case "http.status.code":
                            traceIsPresented = tag.get("value").getAsInt() == 200;
                            break;
                    }
                }
                return traceIsPresented;
            }
        }
        return false;
    }
}
