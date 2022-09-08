package org.jboss.eap.qe.microprofile.metrics;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

import java.net.URL;
import java.util.Arrays;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.eap.qe.microprofile.metrics.hello.MetricsApp;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.ConfigurationException;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.ArquillianContainerProperties;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.ArquillianDescriptorWrapper;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.deployment.ConfigurationUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

@RunWith(Arquillian.class)
public class MetricsTest {

    @ArquillianResource
    URL deploymentUrl;

    private static RequestSpecification jsonMetricsRequest;
    private static RequestSpecification textMetricsRequest;

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, MetricsTest.class.getSimpleName() + ".war")
                .addPackage(MetricsApp.class.getPackage())
                .addAsWebInfResource(new StringAsset(ConfigurationUtil.BEANS_XML_FILE_CONTENT), "beans.xml");
    }

    @BeforeClass
    public static void composeMetricsEndpointURL() throws ConfigurationException {
        ArquillianContainerProperties arqProps = new ArquillianContainerProperties(
                ArquillianDescriptorWrapper.getArquillianDescriptor());
        String metricsURL = "http://" + arqProps.getDefaultManagementAddress() + ":" + arqProps.getDefaultManagementPort()
                + "/metrics";
        jsonMetricsRequest = given()
                .baseUri(metricsURL)
                .accept(ContentType.JSON);
        textMetricsRequest = given()
                .baseUri(metricsURL)
                .accept(ContentType.TEXT);
    }

    /**
     * @tpTestDetails MP Metrics specification compatibility scenario to verify MP Metrics are registered - HTTP OPTIONS
     *                method is used
     * @tpPassCrit metrics are registered and exposed via /metrics end-point.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @RunAsClient
    public void applicationMetricsAreRegisteredAtDeploymentTime() {
        jsonMetricsRequest.options().then()
                .contentType(ContentType.JSON)
                .header("Content-Type", containsString("application/json"))
                .body("$", hasKey("application"),
                        "application", hasKey("hello-count"),
                        "application", hasKey("hello-time"),
                        "application", hasKey("hello-freq"),
                        "application", hasKey("hello-invocations"),
                        "application.keySet()", hasSize(4));
    }

    /**
     * @tpTestDetails MP Metrics specification compatibility scenario to verify application MP Metrics have correct
     *                metadata according to MP Metric specification.
     *                Metadata are exposed under /metrics endpoint with HTTP OPTIONS method.
     * @tpPassCrit Correct metadata (against the specification) are provided for metrics.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @RunAsClient
    public void jsonMetadata() {
        jsonMetricsRequest.options().then()
                .contentType(ContentType.JSON)
                .header("Content-Type", containsString("application/json"))
                .body("$", hasKey("application"),
                        "application", hasKey("hello-count"),
                        "application", hasKey("hello-count"),
                        "application.hello-count", hasKey("displayName"),
                        "application.hello-count.displayName", equalTo("Hello Count"),
                        "application.hello-count", hasKey("description"),
                        "application.hello-count.description", equalTo("Number of hello invocations"),
                        "application.hello-count", hasKey("unit"),
                        "application.hello-count.unit", equalTo("none"),

                        "application", hasKey("hello-time"),
                        "application.hello-time", hasKey("displayName"),
                        "application.hello-time.displayName", equalTo("Hello Time"),
                        "application.hello-time", hasKey("description"),
                        "application.hello-time.description", equalTo("Time of hello invocations"),
                        "application.hello-time", hasKey("unit"),
                        "application.hello-time.unit", equalTo("milliseconds"),

                        "application", hasKey("hello-freq"),
                        "application.hello-freq", hasKey("displayName"),
                        "application.hello-freq.displayName", equalTo("Hello Freq"),
                        "application.hello-freq", hasKey("description"),
                        "application.hello-freq.description", equalTo("Frequency of hello invocations"),
                        "application.hello-freq", hasKey("unit"),
                        "application.hello-freq.unit", equalTo("per_second"),

                        "application", hasKey("hello-invocations"),
                        "application.hello-invocations", hasKey("displayName"),
                        "application.hello-invocations.displayName", equalTo("Hello Invocations"),
                        "application.hello-invocations", hasKey("description"),
                        "application.hello-invocations.description", equalTo("Number of current hello invocations"),
                        "application.hello-invocations", hasKey("unit"),
                        "application.hello-invocations.unit", equalTo("none"));
    }

    /**
     * @tpTestDetails MP Metrics specification compatibility scenario to verify CDI invocation are taken in account in
     *                MP Metrics subsystem and values are correct according to number of invocations.
     * @tpPassCrit Metrics has correct values (according to number of CDI bean invocations) in JSON and prometheus format.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @RunAsClient
    public void dataTest() {
        get(deploymentUrl.toString()).then()
                .statusCode(200)
                .body(equalTo("Hello from counted and timed and metered and concurrent-gauged method"));

        given().baseUri(deploymentUrl.toString()).basePath("another-hello").get().then()
                .statusCode(200)
                .body(equalTo("Hello from another counted method"));

        for (int i = 0; i < 10; i++) {
            get(deploymentUrl.toString()).then()
                    .statusCode(200);
        }

        jsonData();
        prometheusData();
    }

    /**
     * Verify application metrics in JSON format.
     */
    private void jsonData() {
        jsonMetricsRequest.get().then()
                .contentType(ContentType.JSON)
                .header("Content-Type", containsString("application/json"))
                .body("$", hasKey("application"),
                        "application", hasKey("hello-count"),
                        "application.hello-count", equalTo(12), // 11 at `/` + 1 at `/another-hello`

                        "application", hasKey("hello-time"),
                        "application.hello-time.count", equalTo(11),
                        "application.hello-time.min.toDouble()", greaterThanOrEqualTo(1.0),
                        "application.hello-time.min.toDouble()", lessThanOrEqualTo(110.0),
                        "application.hello-time.max.toDouble()", greaterThanOrEqualTo(1.0),
                        "application.hello-time.max.toDouble()", lessThanOrEqualTo(110.0),

                        "application", hasKey("hello-freq"),
                        "application.hello-freq.count", equalTo(11),

                        "application", hasKey("hello-invocations"),
                        "application.hello-invocations.current", equalTo(0));
    }

    /**
     * Verify application metrics in prometheus format.
     */
    private void prometheusData() {
        textMetricsRequest.get().then()
                .contentType(ContentType.TEXT)
                .header("Content-Type", containsString("text/plain"))
                .body(
                        containsString("application_hello_count_total 12.0"),
                        containsString("application_hello_time_seconds_count 11.0"),
                        containsString("application_hello_time_seconds_count 11.0"),
                        new PrometheusMetricValueMatcher("application_hello_time_min_seconds", greaterThanOrEqualTo(0.001),
                                lessThanOrEqualTo(0.110)),
                        containsString("application_hello_freq_total 11.0"),
                        containsString("application_hello_invocations_current 0.0"),
                        new PrometheusMetricValueMatcher("application_hello_time_min_seconds", greaterThanOrEqualTo(0.001),
                                lessThanOrEqualTo(0.110)));
    }

    /**
     * @tpTestDetails MP Metrics specification compatibility scenario to verify WildFly provides vendor metrics.
     * @tpPassCrit Server provides at least one vendor metric.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @RunAsClient
    public void vendorMetrics() {
        jsonMetricsRequest.get().then()
                .contentType(ContentType.JSON)
                .header("Content-Type", containsString("application/json"))
                .body("$", hasKey("vendor"),
                        "vendor.keySet().size()", greaterThan(0));
    }

    /**
     * @tpTestDetails MP Metrics specification compatibility scenario to verify application is able to inject MetricRegistry
     *                and get all MP metrics
     * @tpPassCrit Application is able to obtain base, vendor and application metrics from injected MetricRegistry.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @RunAsClient
    public void getAllRegisteredMetrics() {
        given().baseUri(deploymentUrl.toString()).basePath("summary/all-registries").accept(ContentType.JSON).get().then()
                .contentType(ContentType.JSON)
                .header("Content-Type", containsString("application/json"))
                .body("$", hasKey("base"),
                        "base", hasItem("cpu.availableProcessors"),
                        "base", hasItem("memory.usedHeap"),
                        "base", hasItem("memory.committedHeap"),
                        "base", hasItem("memory.maxHeap"),
                        "base", hasItem("jvm.uptime"),
                        "base", hasItem("thread.count"),
                        "base", hasItem("thread.daemon.count"),
                        "base", hasItem("thread.max.count"),
                        "base", hasItem("classloader.loadedClasses.count"),
                        "base", hasItem("classloader.loadedClasses.total"),
                        "base", hasItem("classloader.unloadedClasses.total"),

                        "$", hasKey("vendor"),
                        "vendor.size()", greaterThan(0),

                        "$", hasKey("app"),
                        "app", hasItem("hello-count"),
                        "app", hasItem("hello-time"),
                        "app", hasItem("hello-freq"),
                        "app", hasItem("hello-invocations"));
    }

    /**
     * @tpTestDetails MP Metrics specification compatibility scenario to verify application is able to inject MetricRegistry
     *                and get application MP metrics (counters, timers,...)
     * @tpPassCrit Application is able to obtain application metrics from injected MetricRegistry.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @RunAsClient
    public void getAppRegisteredMetrics() {
        given().baseUri(deploymentUrl.toString()).basePath("summary/app-registry").accept(ContentType.JSON).get().then()
                .contentType(ContentType.JSON)
                .header("Content-Type", containsString("application/json"))
                .body("$", hasKey("app-counters"),
                        "app-counters", hasSize(1),
                        "app-counters", hasItem("hello-count"),

                        "$", hasKey("app-timers"),
                        "app-timers", hasSize(1),
                        "app-timers", hasItem("hello-time"),

                        "$", hasKey("app-meters"),
                        "app-meters", hasSize(1),
                        "app-meters", hasItem("hello-freq"),

                        "$", hasKey("app-gauges"),
                        "app-gauges", hasSize(0),

                        "$", hasKey("app-concurrent-gauges"),
                        "app-concurrent-gauges", hasSize(1),
                        "app-concurrent-gauges", hasItem("hello-invocations"));
    }

    /**
     * Matcher for metrics in prometheus format for rest-assured library.
     * Purpose of the class is to parse metric of given name from WildFlt /metrics endpoint and apply matchers to the value.
     */
    static class PrometheusMetricValueMatcher extends BaseMatcher {
        private final String metricName;
        private final Matcher[] matchers;

        PrometheusMetricValueMatcher(String metricName, Matcher... matchers) {
            this.metricName = metricName;
            this.matchers = matchers;
        }

        /**
         * Parse a response and return the value of a metric.
         */
        private static double prometheusMetricValue(String prometheusResponse, String metricName) {
            String lineBeginning = metricName + " ";
            return Arrays.stream(prometheusResponse.split("\n"))
                    .filter(line -> line.startsWith(lineBeginning))
                    .map(line -> line.substring(lineBeginning.length()))
                    .mapToDouble(Double::parseDouble)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Metric " + metricName + " not found in response"));
        }

        /**
         * @param item expected /metrics endpoint response in prometheus format
         * @return true if all matchers match the value of a given metric
         */
        @Override
        public boolean matches(Object item) {
            if (item instanceof String) {
                double value = prometheusMetricValue((String) item, metricName);
                for (Matcher matcher : matchers) {
                    if (!matcher.matches(value)) {
                        return false;
                    }
                }
                return true;
            } else {
                return false;
            }
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("prometheus metric matcher for '").appendText(metricName).appendText("' matchers: ");
            for (Matcher matcher : matchers) {
                matcher.describeTo(description);
            }
        }
    }
}
