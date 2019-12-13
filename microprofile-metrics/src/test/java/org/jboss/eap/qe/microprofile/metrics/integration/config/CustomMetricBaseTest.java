package org.jboss.eap.qe.microprofile.metrics.integration.config;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.TimeoutException;

import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.ConfigurationException;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.junit.Before;
import org.junit.Test;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;

import io.restassured.http.ContentType;

public abstract class CustomMetricBaseTest {

    public static final String INCREMENT_CONFIG_PROPERTY = "dummy.increment";

    public static final int DEFAULT_VALUE = 2;

    @ArquillianResource
    URL deploymentUrl;

    @ContainerResource
    ManagementClient managementClient;

    String metricsURL;

    @Before
    public void before() throws ConfigurationException, InterruptedException, TimeoutException, IOException {
        metricsURL = "http://" + managementClient.getMgmtAddress() + ":" + managementClient.getMgmtPort() + "/metrics";
        new Administration(ManagementClientProvider.onlineStandalone()).reload();
    }

    abstract void setConfigProperties(int number) throws Exception;

    /**
     * @tpTestDetails High-level multi-component customer scenario to verify custom counter metric is incremented
     *                accordingly to the number of a CDI bean invocations.
     *                The metric increment depends on MP Config property value and is provided by a CDI bean.
     * @tpPassCrit Counter metric is incremented by configured value
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testCustomMetricDefault() throws Exception {
        setConfigProperties(DEFAULT_VALUE);
        performRequest();
        testMetricForValue(DEFAULT_VALUE);
    }

    void testMetricForValue(int value) {
        given()
                .baseUri("http://" + managementClient.getMgmtAddress() + ":" + managementClient.getMgmtPort() + "/metrics")
                .accept(ContentType.JSON)
                .get()
                .then()
                .body("application.custom-metric", equalTo(value));
    }

    void performRequest() {
        get(deploymentUrl.toString()).then()
                .statusCode(200)
                .body(equalTo("Hello from custom metric service!"));
    }

}
