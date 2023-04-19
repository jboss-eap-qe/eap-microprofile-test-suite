package org.jboss.eap.qe.microprofile.health.integration;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.eap.qe.microprofile.health.DisableDefaultHealthProceduresSetupTask;
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
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;

import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

/**
 * MP FT integration will be tested relying on microprofile-config.properties,
 * MP Config properties are defined in microprofile-config.properties in META INF.
 *
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
 *
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({ DisableDefaultHealthProceduresSetupTask.class })
public class FailSafeCDIConfigFileHealthTest {

    private static final String MPConfigContent = String.format("%s=%s\n%s=%s\n%s=%s\n%s=%s",
            FailSafeDummyService.LIVE_CONFIG_PROPERTY, false,
            FailSafeDummyService.READY_CONFIG_PROPERTY, true,
            FailSafeDummyService.IN_MAINTENANCE_CONFIG_PROPERTY, true,
            FailSafeDummyService.READY_IN_MAINTENANCE_CONFIG_PROPERTY, false);

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
                .addAsManifestResource(new StringAsset(MPConfigContent), "microprofile-config.properties")
                .addAsWebInfResource(ConfigurationUtil.BEANS_XML_FILE_LOCATION, "beans.xml");
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

        //  TODO - Resume for XP 5/Micrometer?
        //
        //        MetricsChecker.get(metricsRequest)
        //                .validateSimulationCounter(FailSafeDummyService.MAX_RETRIES + 1) // 1 call + N retries
        //                .validateInvocationsTotal(1, true)
        //                .validateRetryRetriesTotal(FailSafeDummyService.MAX_RETRIES) // N retries
        //                .validateRetryCallsTotalMaxRetriesReached(1);
        //
        //        // same request have been validated above, now we need to increase metrics
        //        get(HealthUrlProvider.healthEndpoint()).then().statusCode(503);
        //
        //        MetricsChecker.get(metricsRequest)
        //                .validateSimulationCounter(FailSafeDummyService.MAX_RETRIES * 2 + 2) // 2 calls + 2N retries
        //                .validateInvocationsTotal(2, true)
        //                .validateRetryCallsTotalMaxRetriesReached(2)
        //                .validateRetryRetriesTotal(FailSafeDummyService.MAX_RETRIES * 2); // 2N retries
    }

    /**
     * @tpTestDetails Multiple-component customer scenario - health check status is based on fail-safe CDI bean and MP Config
     *                property. There is IOException since service is in maintenance but fallback method should be used.
     * @tpPassCrit Overall and the health check status are down and fallback method is invoked.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    final public void testLivenessEndpointDownInMaintenance() throws Exception {
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
        get(HealthUrlProvider.readyEndpoint()).then()
                .statusCode(503)
                .contentType(ContentType.JSON)
                .body("status", is("DOWN"),
                        "checks", hasSize(1),
                        "checks.status", hasItems("DOWN"),
                        "checks.name", containsInAnyOrder("dummyReadiness"));

        //  TODO - Resume for XP 5/Micrometer?
        //
        //        MetricsChecker.get(metricsRequest)
        //                .validateSimulationCounter(FailSafeDummyService.MAX_RETRIES + 1) // 1 call + N retries
        //                .validateInvocationsTotal(1, true)
        //                .validateRetryRetriesTotal(FailSafeDummyService.MAX_RETRIES); // N retries
        //
        //        // same request have been validated above, now we need to increase metrics
        //        get(HealthUrlProvider.readyEndpoint()).then().statusCode(503);
        //
        //        MetricsChecker.get(metricsRequest)
        //                .validateSimulationCounter(FailSafeDummyService.MAX_RETRIES * 2 + 2) // 2 calls + 2N retries
        //                .validateInvocationsTotal(2, true)
        //                .validateRetryRetriesTotal(FailSafeDummyService.MAX_RETRIES * 2); // 2N retries
    }
}
