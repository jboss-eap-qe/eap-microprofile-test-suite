package org.jboss.eap.qe.microprofile.opentracing.v13;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.net.URL;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.eap.qe.microprofile.opentracing.MicroProfileOpenTracingServerHelper;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.deployment.ConfigurationUtil;
import org.jboss.eap.qe.ts.common.docker.Docker;
import org.jboss.eap.qe.ts.common.docker.DockerContainers;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
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
public class MPOpenTracing13Test {

    private static final String APPLICATION_NAME = MPOpenTracing13Test.class.getSimpleName();

    private static final String PREFIX = MPOpenTracing13Test.class.getPackage().getName() + ".";

    private static final String GET_PREFIX = "GET:" + PREFIX;

    private Map<String, String> URL_TO_RESPONSES = createURLMap();

    private Map<String, Integer> URL_EXPECTED_ERRORS = createExpectedErrorsMap();

    private List<ExpectedTrace> EXPECTED_TRACES = createExpectedTracesMap();

    private static Map<String, String> createURLMap() {
        Map<String, String> urlMap = new HashMap<>();

        urlMap.put("tracedlogged", "Hello from tracedLoggedMethod");
        urlMap.put("traced", "Hello from tracedMethod");
        urlMap.put("logged", "Hello from loggedMethod");
        urlMap.put("nottracednotlogged", "Hello from notTracedNotLoggedMethod");

        urlMap.put("named/true", "Hello from tracedNamedTrue");
        urlMap.put("true", "Hello from tracedTrue");
        urlMap.put("named/false", "Hello from tracedNamedFalse");
        urlMap.put("false", "Hello from tracedFalse");

        // test http-path when path contains regular expressions
        urlMap.put("test/1/hello", "Hello from twoWildcard: 1, hello");

        return Collections.unmodifiableMap(urlMap);
    }

    private static Map<String, Integer> createExpectedErrorsMap() {
        Map<String, Integer> errorMap = new HashMap<>();
        errorMap.put("traceerror", 500);

        return Collections.unmodifiableMap(errorMap);
    }

    private List<ExpectedTrace> createExpectedTracesMap() {
        List<ExpectedTrace> traceList = new ArrayList<>();

        // traced and tracer.span.logging combinations
        traceList.add(new ExpectedTrace("Resource.tracedLoggedMethod", null, null, PREFIX + "TracedService.tracedLoggedMethod",
                "event", "tracer: tracedLoggedMethod"));
        traceList
                .add(new ExpectedTrace("Resource.tracedMethod", null, null, PREFIX + "TracedService.tracedMethod", null, null));
        traceList.add(new ExpectedTrace("Resource.loggedMethod", "event", "tracer: loggedMethod"));
        traceList.add(new ExpectedTrace("Resource.notTracedNotLoggedMethod"));

        // operationName with on/off combination
        traceList.add(new ExpectedTrace("Resource.tracedNamedTrue", null, null, "operationName-should-appear-tracedNamedTrue",
                "event", "tracedNamedTrue"));
        traceList.add(new ExpectedTrace("Resource.tracedTrue", null, null, PREFIX + "TracedService.tracedTrue", "event",
                "tracedTrue"));
        traceList.add(new ExpectedTrace("Resource.tracedNamedFalse", "event", "tracedNamedFalse"));
        traceList.add(new ExpectedTrace("Resource.tracedFalse", "event", "tracedFalse"));

        // two wild cards
        traceList.add(new ExpectedTrace("Resource.twoWildcard", null, null, PREFIX + "TracedService.twoWildcard", "event",
                "twoWildcard: 1, hello"));

        // errors
        traceList.add(new ExpectedTrace("Resource.traceError", "event", "error", "error", "true",
                PREFIX + "TracedService.traceError", "error.object", "java.lang.RuntimeException", "error", "true"));

        return Collections.unmodifiableList(traceList);
    }

    @ArquillianResource
    private URL baseApplicationUrl;

    @ClassRule
    public static Docker jaegerContainer = DockerContainers.jaeger();

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
                .addAsManifestResource(new StringAsset(ConfigurationUtil.BEANS_XML_FILE_CONTENT), "beans.xml")
                .addAsManifestResource(new StringAsset(mpConfig), "microprofile-config.properties");
    }

    /**
     * @tpTestDetails Send a request to deployed MP OT application on /rest endpoint.
     * @tpPassCrit Verify that deployed MP OT application is available and returns expected response (including a negative one)
     *             from traced service.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @InSequence(1)
    @RunAsClient
    public void testApplicationRequest() {
        String APPLICATION_URL = baseApplicationUrl + "rest/";

        URL_TO_RESPONSES.forEach((url, expectedResponse) -> {
            assertThat(RestAssured.when().get(APPLICATION_URL + url).asString(), is(equalTo(expectedResponse)));
        });

        URL_EXPECTED_ERRORS.forEach((url, expectedResponse) -> {
            assertThat("Error response was expected for url: " + APPLICATION_URL + url,
                    RestAssured.when().get(APPLICATION_URL + url).getStatusCode(), is(equalTo(expectedResponse)));
        });
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
            String response = RestAssured.when().get("http://localhost:16686/api/traces?service=" + APPLICATION_NAME + ".war")
                    .asString();

            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            assertThat(json.has("data"), is(true));
            JsonArray data = json.getAsJsonArray("data");

            assertThat(data.size(), is(equalTo(EXPECTED_TRACES.size())));

            EXPECTED_TRACES.forEach(trace -> checkTrace(data, trace));
        });
    }

    /**
     * Checks whether trace has the expected attributes such as operationName, log/childLog and tag/childTag.
     *
     * @param data - recorded traces in JSON format.
     * @param trace - trace with expected attributes.
     */
    private static void checkTrace(JsonArray data, ExpectedTrace trace) {

        List<JsonElement> spanList = new ArrayList<>();
        data.forEach(spanList::add);

        assertThat((spanList.stream().anyMatch(element -> {
            JsonObject traceJson = element.getAsJsonObject();
            if (traceJson.has("spans")) {
                JsonArray spans = traceJson.getAsJsonArray("spans");

                if (spans.size() == 1) {
                    JsonObject span = spans.get(0).getAsJsonObject();
                    return checkTraceLogTag(trace, span);
                } else if (spans.size() == 2) {
                    if (trace.childOperationName != null) {

                        List<JsonElement> traceList = new ArrayList<>();
                        spans.forEach(traceList::add);

                        return traceList.stream().anyMatch(el -> checkTraceLogTag(trace, el.getAsJsonObject())) ||
                                traceList.stream().anyMatch(el -> checkTraceChildLogTag(trace, el.getAsJsonObject()));
                    }
                } else {
                    // an unexpected trace with incorrect number of spans
                    return false;
                }
            }
            return false;
        })), is(true));
    }

    /**
     * Checks whether recorded span attributes match the expected ones.
     * Expected tag or log can be empty, so in that case we check the present ones only.
     *
     * @param trace - trace with expected attributes.
     * @param span - recorded span in JSON format
     * @return true in case when all expected span attributes matches match the expected ones, otherwise false.
     */
    private static boolean checkTraceLogTag(ExpectedTrace trace, JsonObject span) {
        if (span.get("operationName").getAsString().equalsIgnoreCase(trace.operationName)) {
            if (trace.log == null) {
                return checkSpanTag(trace.tag, span);
            } else if (trace.tag == null) {
                return checkSpanLog(trace.log, span);
            } else {
                return checkSpanLog(trace.log, span) && checkSpanTag(trace.tag, span);
            }
        }
        return false;
    }

    /**
     * Checks whether recorded span child attributes match the expected ones.
     * Expected childTag or childLog can be empty, so in that case we check the present ones only.
     *
     * @param trace - trace with expected child attributes.
     * @param span - recorded span in JSON format.
     * @return true in case when all expected child span attributes matches match the expected ones, otherwise false.
     */
    private static boolean checkTraceChildLogTag(ExpectedTrace trace, JsonObject span) {
        if (span.get("operationName").getAsString().equalsIgnoreCase(trace.childOperationName)) {
            if (trace.childLog == null) {
                return checkSpanTag(trace.childTag, span);
            } else if (trace.childTag == null) {
                return checkSpanLog(trace.childLog, span);
            } else {
                return checkSpanLog(trace.childLog, span) && checkSpanTag(trace.childTag, span);
            }
        }
        return false;
    }

    /**
     * Checks whether recorded span log key and value match the expected ones. Expected log can be empty.
     * 
     * @param expectedLog - expected log <key,value> pair.
     * @param span - recorded span in JSON format.
     * @return true in case when span log key and value match the expected ones, otherwise false.
     */
    private static boolean checkSpanLog(Map.Entry<String, String> expectedLog, JsonObject span) {
        if (expectedLog == null) {
            return true;
        }

        JsonObject foundLog = null;
        for (JsonElement log : span.get("logs").getAsJsonArray()) {
            for (JsonElement logFieldElement : log.getAsJsonObject().getAsJsonArray("fields")) {
                JsonObject logField = logFieldElement.getAsJsonObject();
                if (logField.get("key").getAsString().equals(expectedLog.getKey())) {
                    foundLog = logField;
                    break;
                }
            }
        }

        return foundLog != null &&
                foundLog.get("key").getAsString().equalsIgnoreCase(expectedLog.getKey()) &&
                foundLog.get("value").getAsString().equalsIgnoreCase(expectedLog.getValue());
    }

    /**
     * Checks whether recorded span tag key and value match the expected ones. Expected tag can be empty.
     * 
     * @param expectedTag - expected tag <key,value> pair.
     * @param span - recorded span in JSON format.
     * @return true in case when span tag key and value match the expected ones, otherwise false.
     */
    private static boolean checkSpanTag(Map.Entry<String, String> expectedTag, JsonObject span) {
        if (expectedTag == null) {
            return true;
        }

        JsonObject foundTag = null;
        for (JsonElement tag : span.get("tags").getAsJsonArray()) {
            if (tag.getAsJsonObject().get("key").getAsString().equals(expectedTag.getKey())) {
                foundTag = tag.getAsJsonObject();
                break;
            }
        }

        return foundTag != null &&
                foundTag.get("key").getAsString().equalsIgnoreCase(expectedTag.getKey()) &&
                foundTag.get("value").getAsString().equalsIgnoreCase(expectedTag.getValue());
    }

    private class ExpectedTrace {
        final String operationName;

        final Map.Entry<String, String> log;

        final Map.Entry<String, String> tag;

        final String childOperationName;

        final Map.Entry<String, String> childLog;

        final Map.Entry<String, String> childTag;

        ExpectedTrace(String operationName) {
            this(operationName, null, null);
        }

        ExpectedTrace(String operationName, String logKey, String logValue) {
            this(operationName, logKey, logValue, null, null, null);
        }

        ExpectedTrace(String operationName, String logKey, String logValue, String childOperationName, String childLogKey,
                String childLogValue) {
            this(operationName, logKey, logValue, null, null, childOperationName, childLogKey, childLogValue, null, null);
        }

        ExpectedTrace(String operationName, String logKey, String logValue, String tagKey, String tagValue,
                String childOperationName, String childLogKey, String childLogValue, String childTagKey, String childTagValue) {
            this.operationName = GET_PREFIX + operationName;
            this.log = logKey != null ? new AbstractMap.SimpleImmutableEntry<>(logKey, logValue) : null;
            this.tag = tagKey != null ? new AbstractMap.SimpleImmutableEntry<>(tagKey, tagValue) : null;
            this.childOperationName = childOperationName;
            this.childLog = childLogKey != null ? new AbstractMap.SimpleImmutableEntry<>(childLogKey, childLogValue) : null;
            this.childTag = childTagKey != null ? new AbstractMap.SimpleImmutableEntry<>(childTagKey, childTagValue) : null;
        }
    }
}
