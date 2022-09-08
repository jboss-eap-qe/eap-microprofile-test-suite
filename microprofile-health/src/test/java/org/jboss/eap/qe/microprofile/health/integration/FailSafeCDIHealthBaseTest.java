package org.jboss.eap.qe.microprofile.health.integration;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.eap.qe.microprofile.health.tools.HealthUrlProvider;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.ConfigurationException;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.ArquillianContainerProperties;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.ArquillianDescriptorWrapper;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.deployment.ConfigurationUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;

import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

/**
 * Testclass to test {@code /health}, {@code /health/live}, {@code /health/ready} endpoints.
 * {@link CDIBasedLivenessHealthCheck} and {@link CDIBasedReadinessHealthCheck} probes are based on fail-safe CDI bean
 * {@link FailSafeDummyService} configured at the beginning via MP Config properties:
 * <ul>
 * <li>{@link FailSafeDummyService#LIVE_CONFIG_PROPERTY} - service is live</li>
 * <li>{@link FailSafeDummyService#READY_CONFIG_PROPERTY} - service is ready if
 * {@link FailSafeDummyService#IN_MAINTENANCE_CONFIG_PROPERTY} is {@code false}</li>
 * <li>{@link FailSafeDummyService#READY_IN_MAINTENANCE_CONFIG_PROPERTY} - service is ready if
 * {@link FailSafeDummyService#IN_MAINTENANCE_CONFIG_PROPERTY} is {@code true}</li>
 * </ul>
 * If service is in maintenance a method simulating opening resources {@link FailSafeDummyService#simulateOpeningResources}
 * throws a {@link IOException}.
 * Subclasses are supposed to configure only MP Config - set MP Config properties in different ways.
 *
 * Tests in this test class are not expected to change the state of probes after their initialization since some
 * ways of configuring MP Config would require a Wildfly reload in order to update those values.
 */
@RunAsClient
public abstract class FailSafeCDIHealthBaseTest {

    protected static RequestSpecification metricsRequest;

    @BeforeClass
    public static void setURL() throws ConfigurationException {
        ArquillianContainerProperties arqProps = new ArquillianContainerProperties(
                ArquillianDescriptorWrapper.getArquillianDescriptor());
        String url = "http://" + arqProps.getDefaultManagementAddress() + ":" + arqProps.getDefaultManagementPort()
                + "/metrics";
        metricsRequest = given().baseUri(url).accept(ContentType.JSON);
    }

    @Before
    public void before() throws InterruptedException, TimeoutException, IOException, ConfigurationException {
        // reset MP Metrics
        try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
            new Administration(client).reload();
        }
    }

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return ShrinkWrap
                .create(WebArchive.class, FailSafeCDIHealthBaseTest.class.getSimpleName() + ".war")
                .addClasses(FailSafeDummyService.class, CDIBasedLivenessHealthCheck.class, CDIBasedReadinessHealthCheck.class)
                .addAsWebInfResource(new StringAsset(ConfigurationUtil.BEANS_XML_FILE_CONTENT), "beans.xml");
    }

    /**
     * Subclass is expected to implement the method to set the MP Config properties in a way specific to MP Config
     * configuration.
     */
    protected abstract void setConfigProperties(boolean live, boolean ready, boolean inMaintenance, boolean readyInMaintenance)
            throws Exception;

    /**
     * @tpTestDetails Multiple-component customer scenario - health check status is based on fail-safe CDI bean and MP Config
     *                property.
     * @tpPassCrit Overall and the health check status are up. MP Metrics are increased according to the specification.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    final public void testHealthEndpointUp() throws Exception {
        setConfigProperties(true, true, false, false);
        get(HealthUrlProvider.healthEndpoint()).then()
                .contentType(ContentType.JSON)
                .statusCode(200)
                .body("status", is("UP"),
                        "checks", hasSize(2),
                        "checks.status", hasItems("UP"),
                        "checks.status", not(hasItems("DOWN")),
                        "checks.name", containsInAnyOrder("dummyLiveness", "dummyReadiness"));

        MetricsChecker.get(metricsRequest)
                .validateSimulationCounter(1)
                .validateInvocationsTotal(1)
                .validateRetryCallsSucceededNotTriedTotal(1);

        // same request have been validated above, now we need to increase metrics
        get(HealthUrlProvider.healthEndpoint()).then().statusCode(200);

        MetricsChecker.get(metricsRequest)
                .validateSimulationCounter(2)
                .validateInvocationsTotal(2)
                .validateRetryCallsSucceededNotTriedTotal(2);
    }

    /**
     * @tpTestDetails Multiple-component customer scenario - health check status is based on fail-safe CDI bean and MP Config
     *                property.
     * @tpPassCrit Overall and the health check status are up.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    final public void testLivenessEndpointUp() throws Exception {
        setConfigProperties(true, true, false, false);
        get(HealthUrlProvider.liveEndpoint()).then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("status", is("UP"),
                        "checks", hasSize(1),
                        "checks.status", hasItems("UP"),
                        "checks.status", not(hasItems("DOWN")),
                        "checks.name", containsInAnyOrder("dummyLiveness"));
    }

    /**
     * @tpTestDetails Multiple-component customer scenario - health check status is based on fail-safe CDI bean and MP Config
     *                property.
     * @tpPassCrit Overall and the health check status are up. MP Metrics are increased according to the specification.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    final public void testReadinessEndpointUp() throws Exception {
        setConfigProperties(true, true, false, false);
        get(HealthUrlProvider.readyEndpoint()).then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("status", is("UP"),
                        "checks", hasSize(1),
                        "checks.status", hasItems("UP"),
                        "checks.name", containsInAnyOrder("dummyReadiness"));

        MetricsChecker.get(metricsRequest)
                .validateSimulationCounter(1)
                .validateInvocationsTotal(1)
                .validateRetryCallsSucceededNotTriedTotal(1);

        // same request have been validated above, now we need to increase metrics
        get(HealthUrlProvider.readyEndpoint()).then().statusCode(200);

        MetricsChecker.get(metricsRequest)
                .validateSimulationCounter(2)
                .validateInvocationsTotal(2)
                .validateRetryCallsSucceededNotTriedTotal(2);
    }

    /**
     * @tpTestDetails Multiple-component customer scenario - health check status is based on fail-safe CDI bean and MP Config
     *                property. There is IOException since service is in maintenance but fallback method should be used.
     * @tpPassCrit Overall and the health check status are down and fallback method is invoked. MP Metrics are increased
     *             according to the specification.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    final public void testHealthEndpointDownInMaintenance() throws Exception {
        setConfigProperties(false, true, true, false);
        // TODO Java 11 Map<String, String> liveCheck = Map.of( "name", "dummyLiveness", "status", "DOWN");
        Map<String, String> liveCheck = Collections.unmodifiableMap(new HashMap<String, String>() {
            {
                put("name", "dummyLiveness");
                put("status", "DOWN");
            }
        });
        Map<String, String> readyCheck = Collections.unmodifiableMap(new HashMap<String, String>() {
            {
                put("name", "dummyReadiness");
                put("status", "DOWN");
            }
        });
        get(HealthUrlProvider.healthEndpoint()).then()
                .statusCode(503)
                .contentType(ContentType.JSON)
                .body("status", is("DOWN"),
                        "checks", hasSize(2),
                        "checks", containsInAnyOrder(liveCheck, readyCheck));

        MetricsChecker.get(metricsRequest)
                .validateSimulationCounter(FailSafeDummyService.MAX_RETRIES + 1) // 1 call + N retries
                .validateInvocationsTotal(1, true)
                .validateRetryRetriesTotal(FailSafeDummyService.MAX_RETRIES); // N retries

        // same request have been validated above, now we need to increase metrics
        get(HealthUrlProvider.healthEndpoint()).then().statusCode(503);

        MetricsChecker.get(metricsRequest)
                .validateSimulationCounter(FailSafeDummyService.MAX_RETRIES * 2 + 2) // 2 calls + 2N retries
                .validateInvocationsTotal(2, true)
                .validateRetryRetriesTotal(FailSafeDummyService.MAX_RETRIES * 2); // 2N retries
    }

    /**
     * @tpTestDetails Multiple-component customer scenario - health check status is based on fail-safe CDI bean and MP Config
     *                property. There is IOException since service is in maintenance but fallback method should be used.
     * @tpPassCrit Overall and the health check status are down and fallback method is invoked.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    final public void testLivenessEndpointDownInMaintenance() throws Exception {
        setConfigProperties(false, true, true, false);
        get(HealthUrlProvider.liveEndpoint()).then()
                .statusCode(503)
                .contentType(ContentType.JSON)
                .body("status", is("DOWN"),
                        "checks", hasSize(1),
                        "checks.status", hasItems("DOWN"),
                        "checks.name", containsInAnyOrder("dummyLiveness"));
    }

    /**
     * @tpTestDetails Multiple-component customer scenario - health check status is based on fail-safe CDI bean and MP Config
     *                property. There is IOException since service is in maintenance but fallback method should be used.
     * @tpPassCrit Overall and the health check status are down and fallback method is invoked. MP Metrics are increased
     *             according to the specification.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    final public void testReadinessEndpointDownInMaintenance() throws Exception {
        setConfigProperties(false, true, true, false);
        get(HealthUrlProvider.readyEndpoint()).then()
                .statusCode(503)
                .contentType(ContentType.JSON)
                .body("status", is("DOWN"),
                        "checks", hasSize(1),
                        "checks.status", hasItems("DOWN"),
                        "checks.name", containsInAnyOrder("dummyReadiness"));

        MetricsChecker.get(metricsRequest)
                .validateSimulationCounter(FailSafeDummyService.MAX_RETRIES + 1) // 1 call + N retries
                .validateInvocationsTotal(1, true)
                .validateRetryRetriesTotal(FailSafeDummyService.MAX_RETRIES); // N retries

        // same request have been validated above, now we need to increase metrics
        get(HealthUrlProvider.readyEndpoint()).then().statusCode(503);

        MetricsChecker.get(metricsRequest)
                .validateSimulationCounter(FailSafeDummyService.MAX_RETRIES * 2 + 2) // 2 calls + 2N retries
                .validateInvocationsTotal(2, true)
                .validateRetryRetriesTotal(FailSafeDummyService.MAX_RETRIES * 2); // 2N retries
    }

}
