package org.jboss.eap.qe.microprofile.health.integration;

import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.eap.qe.microprofile.health.tools.HealthUrlProvider;
import org.junit.Test;

import io.restassured.http.ContentType;

/**
 * Testclass to test {@code /health}, {@code /health/live}, {@code /health/ready} endpoints. Liveness and readiness probes
 * are configured via MP Config properties at the beginning and changed later in test execution. Tests in the testclass
 * expect no reload operation after changing the values (during the test execution).
 * Test class extends {@link FailSafeCDIHealthBaseTest} for those scenarios that expect dynamic MP Config.
 */
@RunAsClient
public abstract class FailSafeCDIHealthDynamicBaseTest extends FailSafeCDIHealthBaseTest {

    /**
     * @tpTestDetails Multiple-component customer scenario - health check status is based on fail-safe CDI bean and MP Config
     *                property.
     * @tpPassCrit Change in MP Config properties are propagated to health checks correctly.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    final public void testHealthEndpointUpToDown() throws Exception {
        setConfigProperties(true, true, false, false);
        get(HealthUrlProvider.healthEndpoint()).then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .header("Content-Type", containsString("application/json"))
                .body("status", is("UP"),
                        "checks", hasSize(2),
                        "checks.status", hasItems("UP"),
                        "checks.status", not(hasItems("DOWN")),
                        "checks.name", containsInAnyOrder("dummyLiveness", "dummyReadiness"));

        MetricsChecker.get(metricsRequest)
                .validateSimulationCounter(1)
                .validateInvocationsTotal(1)
                .validateRetryCallsSucceededNotTriedTotal(1);

        setConfigProperties(true, false, false, true);
        // TODO Java 11 Map<String, String> liveCheck = Map.of( "name", "dummyLiveness", "status", "UP");
        Map<String, String> liveCheck = Collections.unmodifiableMap(new HashMap<String, String>() {
            {
                put("name", "dummyLiveness");
                put("status", "UP");
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
                .header("Content-Type", containsString("application/json"))
                .body("status", is("DOWN"),
                        "checks", hasSize(2),
                        "checks", containsInAnyOrder(liveCheck, readyCheck));

        MetricsChecker.get(metricsRequest)
                .validateSimulationCounter(2)
                .validateInvocationsTotal(2)
                .validateRetryCallsSucceededNotTriedTotal(2);
    }

    /**
     * @tpTestDetails Multiple-component customer scenario - health check status is based on fail-safe CDI bean and MP Config
     *                property.
     * @tpPassCrit Change in MP Config properties are propagated to health checks correctly.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    final public void testLivenessEndpointUpToDown() throws Exception {
        setConfigProperties(true, true, false, false);
        get(HealthUrlProvider.liveEndpoint()).then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("status", is("UP"),
                        "checks", hasSize(1),
                        "checks.status", hasItems("UP"),
                        "checks.name", containsInAnyOrder("dummyLiveness"));

        setConfigProperties(false, true, false, true);
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
     *                property.
     * @tpPassCrit Change in MP Config properties are propagated to health checks correctly.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    final public void testReadinessEndpointUpToDown() throws Exception {
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

        setConfigProperties(true, false, false, true);
        get(HealthUrlProvider.readyEndpoint()).then()
                .statusCode(503)
                .contentType(ContentType.JSON)
                .body("status", is("DOWN"),
                        "checks", hasSize(1),
                        "checks.status", hasItems("DOWN"),
                        "checks.name", containsInAnyOrder("dummyReadiness"));

        MetricsChecker.get(metricsRequest)
                .validateSimulationCounter(2)
                .validateInvocationsTotal(2)
                .validateRetryCallsSucceededNotTriedTotal(2);
    }

    /**
     * @tpTestDetails Multiple-component customer scenario - health check status is based on fail-safe CDI bean and MP Config
     *                property. There is IOException since service is in maintenance but fallback method should be used.
     * @tpPassCrit Overall and the health check status are down and fallback method is invoked. MP Config change is propagated
     *             correctly.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    final public void testHealthEndpointDownToUpInMaintenance() throws Exception {
        setConfigProperties(true, true, true, false);
        // TODO Java 11 Map<String, String> liveCheck = Map.of( "name", "dummyLiveness", "status", "UP");
        Map<String, String> liveCheck = Collections.unmodifiableMap(new HashMap<String, String>() {
            {
                put("name", "dummyLiveness");
                put("status", "UP");
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

        setConfigProperties(true, true, false, true);
        // TODO Java11 readyCheck = Map.of("name", "dummyReadiness", "status", "UP");
        readyCheck = Collections.unmodifiableMap(new HashMap<String, String>() {
            {
                put("name", "dummyReadiness");
                put("status", "UP");
            }
        });
        get(HealthUrlProvider.healthEndpoint()).then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("status", is("UP"),
                        "checks", hasSize(2),
                        "checks", containsInAnyOrder(liveCheck, readyCheck));

        MetricsChecker.get(metricsRequest)
                .validateSimulationCounter(FailSafeDummyService.MAX_RETRIES + 2) // previous + 1
                .validateInvocationsTotal(1)
                .validateRetryRetriesTotal(FailSafeDummyService.MAX_RETRIES) // N retries
                .validateRetryCallsSucceededNotTriedTotal(1);
    }

    /**
     * @tpTestDetails Multiple-component customer scenario - health check status is based on fail-safe CDI bean and MP Config
     *                property. There is IOException since service is in maintenance but fallback method should be used.
     * @tpPassCrit Overall and the health check status are down and fallback method is invoked. MP Config change is propagated
     *             correctly.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    final public void testLivenessEndpointDownToUpInMaintenance() throws Exception {
        setConfigProperties(false, true, true, true);
        get(HealthUrlProvider.liveEndpoint()).then()
                .statusCode(503)
                .contentType(ContentType.JSON)
                .body("status", is("DOWN"),
                        "checks", hasSize(1),
                        "checks.status", hasItems("DOWN"),
                        "checks.name", containsInAnyOrder("dummyLiveness"));

        setConfigProperties(true, false, true, false);
        get(HealthUrlProvider.liveEndpoint()).then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("status", is("UP"),
                        "checks", hasSize(1),
                        "checks.status", hasItems("UP"),
                        "checks.name", containsInAnyOrder("dummyLiveness"));
    }

    /**
     * @tpTestDetails Multiple-component customer scenario - health check status is based on fail-safe CDI bean and MP Config
     *                property. There is IOException since service is in maintenance but fallback method should be used.
     * @tpPassCrit Overall and the health check status are down and fallback method is invoked. MP Config change is propagated
     *             correctly.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    final public void testReadinessEndpointDownToUpInMaintenance() throws Exception {
        setConfigProperties(true, true, true, false);
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

        setConfigProperties(false, true, false, true);
        get(HealthUrlProvider.readyEndpoint()).then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("status", is("UP"),
                        "checks", hasSize(1),
                        "checks.status", hasItems("UP"),
                        "checks.name", containsInAnyOrder("dummyReadiness"));

        MetricsChecker.get(metricsRequest)
                .validateSimulationCounter(FailSafeDummyService.MAX_RETRIES + 2) // previous + 1
                .validateInvocationsTotal(1)
                .validateRetryRetriesTotal(FailSafeDummyService.MAX_RETRIES) // N retries
                .validateRetryCallsSucceededNotTriedTotal(1);
    }
}
