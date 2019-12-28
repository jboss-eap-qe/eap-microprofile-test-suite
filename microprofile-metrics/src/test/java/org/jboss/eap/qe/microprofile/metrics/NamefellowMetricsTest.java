package org.jboss.eap.qe.microprofile.metrics;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;

import java.io.IOException;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

/**
 * Abstract test class expect two non-reusable metrics of the same name defined in different resources. Metrics shall
 * be tagged by {@code mp.metrics.appName} MP Config property. No other metrics are expected. Type of metrics shall be Counter.
 *
 * The name shall be {@code ping-count}. Metrics shall be incremented by calling endpoints defined in by
 * {@link #getPingOneTag()} and {@link #getPingTwoURL()} tagged by {@link #getPingOneTag()} and {@link #getPingTwoTag()}
 *
 * valid for multiple deployment / deployment with sub-deployments scenario
 */
@RunWith(Arquillian.class)
public abstract class NamefellowMetricsTest {

    abstract String getPingOneURL();

    abstract String getPingTwoURL();

    abstract String getPingOneTag();

    abstract String getPingTwoTag();

    private RequestSpecification jsonMetricsRequest;
    private RequestSpecification textMetricsRequest;

    @ContainerResource
    ManagementClient managementClient;

    @Before
    public void prepare() {
        String metricsURL = "http://" + managementClient.getMgmtAddress() + ":" + managementClient.getMgmtPort() + "/metrics";
        jsonMetricsRequest = given()
                .baseUri(metricsURL)
                .accept(ContentType.JSON);
        textMetricsRequest = given()
                .baseUri(metricsURL)
                .accept(ContentType.TEXT);
    }

    /**
     * @tpTestDetails High level scenario to verify two none-reusable counter metrics of the same name are registered
     *                and tagged properly. The information is available under {@code /metrics} endpoint via HTTP OPTIONS.
     *                Metrics are in separate archives - multiple-deployment / sub-deployment scenario.
     * @tpPassCrit Metrics are tagged properly
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @RunAsClient
    public void applicationMetricsAreRegisteredAtDeploymentTime() throws IOException {
        jsonMetricsRequest.options().then()
                .contentType(ContentType.JSON)
                .header("Content-Type", containsString("application/json"))
                .body("$", hasKey("application"),
                        "application", hasKey("ping-count"),
                        "application.ping-count", hasKey("tags"), // 11 at `/` + 1 at `/another-hello`
                        "application.ping-count.tags", hasSize(2),
                        "application.ping-count.tags[0]", hasSize(1),
                        "application.ping-count.tags[1]", hasSize(1),
                        "application.ping-count.tags.flatten()",
                        contains("_app=" + getPingOneTag(), "_app=" + getPingTwoTag()));
    }

    /**
     * @tpTestDetails High level scenario to verify two none-reusable counter metrics of the same name are incremented
     *                properly according to the number of a CDI beans invocation.
     *                Metrics are in separate archives - multiple-deployment / sub-deployment scenario.
     * @tpPassCrit Counters have correct values (according to number of the CDI bean invocations) in JSON and prometheus format.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @RunAsClient
    public void dataTest() throws IOException {

        get(getPingOneURL()).then()
                .statusCode(200)
                .body(equalTo("pong one"));

        given().baseUri(getPingTwoURL())
                .get().then()
                .statusCode(200)
                .body(equalTo("pong two"));

        get(getPingTwoURL()).then().statusCode(200);
        get(getPingTwoURL()).then().statusCode(200);
        get(getPingTwoURL()).then().statusCode(200);

        get(getPingOneURL()).then().statusCode(200);

        jsonDataTest();
        prometheusDataTest();
    }

    /**
     * Verify correct data of counters in JSON format. ping one: 2, ping-two: 4
     */
    private void jsonDataTest() {
        jsonMetricsRequest.get().then()
                .contentType(ContentType.JSON)
                .header("Content-Type", containsString("application/json"))
                .body("$", hasKey("application"),
                        "application", hasKey("ping-count;_app=" + getPingOneTag()),
                        "application.ping-count;_app=" + getPingOneTag(), equalTo(2),

                        "application", hasKey("ping-count;_app=" + getPingTwoTag()),
                        "application.ping-count;_app=" + getPingTwoTag(), equalTo(4));
    }

    /**
     * Verify correct data of counters in prometheus format. ping one: 2, ping-two: 4
     */
    private void prometheusDataTest() {
        textMetricsRequest.get().then()
                .contentType(ContentType.TEXT)
                .header("Content-Type", containsString("text/plain"))
                .body(
                        containsString("application_ping_count_total{_app=\"dep2\"} 4.0"),
                        containsString("application_ping_count_total{_app=\"dep1\"} 2.0"));
    }

}
