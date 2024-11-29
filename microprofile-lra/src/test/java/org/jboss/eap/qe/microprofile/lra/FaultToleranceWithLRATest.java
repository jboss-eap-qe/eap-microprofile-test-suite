package org.jboss.eap.qe.microprofile.lra;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import jakarta.ws.rs.core.Response;

import java.net.URL;
import java.time.Duration;
import java.util.Arrays;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.eap.qe.microprofile.common.setuptasks.FaultToleranceServerSetup;
import org.jboss.eap.qe.microprofile.lra.model.LRAResult;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.MicroProfileServerSetupTask;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.deployment.ConfigurationUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests fault tolerance behavior with Long Running Actions (LRA).
 */
@RunWith(Arquillian.class)
@ServerSetup({ EnableLRASetupTask.class, FaultToleranceServerSetup.class })
public class FaultToleranceWithLRATest {

    public static final String DEPLOYMENT_NAME = "microprofile-lra";

    @ArquillianResource()
    private URL baseApplicationUrl;

    @Deployment(testable = false)
    public static Archive<?> deployment() {
        return ShrinkWrap.create(WebArchive.class, DEPLOYMENT_NAME + ".war")
                .addPackages(true, LRAParticipantWithFaultTolerance1.class.getPackage())
                .addClasses(EnableLRASetupTask.class, MicroProfileServerSetupTask.class, ServerSetupTask.class)
                .addAsWebInfResource(ConfigurationUtil.BEANS_XML_FILE_LOCATION, "beans.xml");
    }

    @Before
    public void before() {
        resetCounter();
        resetParticipantResult(LRAParticipantWithFaultTolerance1.PATH);
        resetParticipantResult(LRAParticipantWithFaultTolerance2.PATH);
    }

    /**
     * Scenario: Tests the LRA with a retry mechanism in case of transient failures. Apply @Retry on a method that starts an LRA
     * transaction.
     * Simulate a failure in the method and verify that the method is retried within the context of the same LRA until the retry
     * limit is reached.
     * Test Steps:
     * Start an LRA and invoke a participant method annotated with @Retry.
     * Force the method to fail a configurable number of times (through exception).
     * Ensure the method is retried up to the maximum number of attempts.
     * Verify that the LRA context is maintained across retries.
     * Verify the final state of the LRA: should fail after all retries fail.
     */
    @Test
    public void testMaxRetryFailWithLRA() {
        // call participant with retry logic, it will retry until it fails (max retry reached)
        given()
                .queryParam(LRAParticipantWithFaultTolerance1.FAIL_LRA, true)
                .when()
                .get(getURL(LRAParticipantWithFaultTolerance1.PATH, LRAParticipantWithFaultTolerance1.RETRY_PATH))
                .then()
                .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())
                .body(containsString("Throwing exception to fail participant until maxRetry is reached"));

        // check that retry is higher than 1
        int numberOfRetries = Integer
                .parseInt(
                        get(getURL(LRAParticipantWithFaultTolerance1.PATH, LRAParticipantWithFaultTolerance1.RETRY_COUNT_PATH))
                                .body().print());
        Assert.assertTrue("Number of retries must be higher than 1 but it's " + numberOfRetries, numberOfRetries > 1);

        // check tx was compensated
        verifyResult(getParticipantResult(LRAParticipantWithFaultTolerance1.PATH), false);
        verifyResult(getParticipantResult(LRAParticipantWithFaultTolerance2.PATH), false);
    }

    /**
     * Scenario: Tests the LRA with a retry mechanism in case of transient failures. Apply @Retry on a method that starts an LRA
     * transaction.
     * Do NOT simulate a failure in the method and verify that the method finished successfully.
     * Test Steps:
     * Start an LRA and invoke a participant method annotated with @Retry without causing any failure.
     * Verify the final state of the LRA: it should be completed successfully.
     */
    @Test
    public void testRetryWithLRA() {
        // call participant with retry logic, however it will not fail on max retry
        given()
                .queryParam(LRAParticipantWithFaultTolerance1.FAIL_LRA, false)
                .when()
                .get(getURL(LRAParticipantWithFaultTolerance1.PATH, LRAParticipantWithFaultTolerance1.RETRY_PATH))
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body(containsString("Method with @Retry ended successfully"));

        // check that there was no retry (i.e. retry is equal to 1)
        int numberOfRetries = Integer
                .parseInt(
                        get(getURL(LRAParticipantWithFaultTolerance1.PATH, LRAParticipantWithFaultTolerance1.RETRY_COUNT_PATH))
                                .body().print());
        Assert.assertEquals("Number of retries must be one.", 1, numberOfRetries);

        // check tx was completed
        verifyResult(getParticipantResult(LRAParticipantWithFaultTolerance1.PATH), true);
        verifyResult(getParticipantResult(LRAParticipantWithFaultTolerance2.PATH), true);
    }

    /**
     * Scenario: Test LRA behavior when an operation on method annotated by @Timeout exceeds a set timeout.
     * Test Steps:
     * Start an LRA and invoke a participant method annotated with @Timeout.
     * Introduce an artificial delay in the method (using Thread.sleep()).
     * Set the @Timeout limit to a value shorter than the introduced delay to trigger a timeout causing LRA transaction to fail.
     * Verify that the LRA has transitioned to a compensating state, ensuring any partial work is compensated.
     */
    @Test
    public void testTimeoutWithLRA() {
        // call participant with @timeout logic and wait for it to fail on timeout
        given()
                .queryParam(LRAParticipantWithFaultTolerance1.FAIL_LRA, false)
                .when()
                .get(getURL(LRAParticipantWithFaultTolerance1.PATH, LRAParticipantWithFaultTolerance1.TIMEOUT_PATH))
                .then()
                .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())
                .body(containsString("timed out"));

        // check tx was compensated
        verifyResult(getParticipantResult(LRAParticipantWithFaultTolerance1.PATH), false);
        verifyResult(getParticipantResult(LRAParticipantWithFaultTolerance2.PATH), false);
    }

    /**
     * Scenario: Test LRA participation with a circuit breaker pattern to prevent overload. Use @CircuitBreaker to temporarily
     * prevent
     * a method from being executed if it consistently fails within an LRA context.
     * Test Steps:
     * Start an LRA and invoke a participant method annotated with @CircuitBreaker.
     * Simulate repeated failures to trigger the circuit breaker’s open state.
     * Verify that subsequent LRA requests to this method are blocked by the open circuit breaker and LRA transactions are
     * compensated.
     * Test the half-open state by allowing one method invocation and ensuring it closes the circuit.
     * Verify that the LRA completes when the circuit breaker is closed again.
     */
    @Test
    public void testCircuitBreakerWithLRA() throws Exception {
        // call participant with circuit breaker logic, first try it works
        given()
                .queryParam(LRAParticipantWithFaultTolerance1.FAIL_LRA, false)
                .when()
                .get(getURL(LRAParticipantWithFaultTolerance1.PATH, LRAParticipantWithFaultTolerance1.CIRCUIT_BREAKER_PATH))
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body(containsString("Method with @CircuitBreaker ended successfully"));

        // now call it until the circuit is open
        int counter = 0;
        while (!given()
                .queryParam(LRAParticipantWithFaultTolerance1.FAIL_LRA, true)
                .when()
                .get(getURL(LRAParticipantWithFaultTolerance1.PATH, LRAParticipantWithFaultTolerance1.CIRCUIT_BREAKER_PATH))
                .body().print().contains("circuit breaker is open")) {
            if (counter++ > 10) {
                Assert.fail("Circuit breaker must be open now.");
            }
        }

        // check last tx was compensated
        verifyResult(getParticipantResult(LRAParticipantWithFaultTolerance1.PATH), false);
        verifyResult(getParticipantResult(LRAParticipantWithFaultTolerance2.PATH), false);

        // After 1 sec circuit gets to half open state and then any new successful call will close it again. Call it until any of the calls succeeds.
        long timeout = Duration.ofSeconds(10).toMillis();
        long startTime = System.currentTimeMillis();
        while (given()
                .queryParam(LRAParticipantWithFaultTolerance1.FAIL_LRA, false)
                .when()
                .get(getURL(LRAParticipantWithFaultTolerance1.PATH, LRAParticipantWithFaultTolerance1.CIRCUIT_BREAKER_PATH))
                .statusCode() != 200) {
            if (System.currentTimeMillis() - startTime > timeout) {
                Assert.fail("Circuit breaker did not close in " + timeout + "ms timeout even though it should.");
            }
            Thread.sleep(200);
        }

        // call once more to check circuit is closed and tx finishes as completed
        given()
                .queryParam(LRAParticipantWithFaultTolerance1.FAIL_LRA, false)
                .when()
                .get(getURL(LRAParticipantWithFaultTolerance1.PATH, LRAParticipantWithFaultTolerance1.CIRCUIT_BREAKER_PATH))
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body(containsString("Method with @CircuitBreaker ended successfully"));

        // check last tx was completed
        verifyResult(getParticipantResult(LRAParticipantWithFaultTolerance1.PATH), true);
        verifyResult(getParticipantResult(LRAParticipantWithFaultTolerance2.PATH), true);
    }

    /**
     * Scenario: Test LRA functionality with fallback actions when primary LRA participant methods fail.
     * Use @Fallback to specify an alternative method in case a participant method fails within the context of an LRA.
     * Test Steps:
     * Start an LRA and invoke a method with @Fallback.
     * Force the method to throw an exception, triggering the fallback method.
     * Verify that the fallback method is called within the same LRA context.
     * Verify that the LRA final state completed.
     */
    @Test
    public void testFallbackWithLRA() {
        // call rest endpoint with having fallback method and check that fallback method was called
        // inside this call there will be checked that method and its fallback method are called with the same tx context
        given()
                .queryParam(LRAParticipantWithFaultTolerance1.FAIL_LRA, true)
                .when()
                .get(getURL(LRAParticipantWithFaultTolerance1.PATH, LRAParticipantWithFaultTolerance1.FALLBACK_PATH))
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body(containsString("Method defined in @Fallback ended successfully"));

        // verify tx is completed
        verifyResult(getParticipantResult(LRAParticipantWithFaultTolerance1.PATH), true);
        verifyResult(getParticipantResult(LRAParticipantWithFaultTolerance2.PATH), true);
    }

    private String getURL(String... paths) {
        String baseUrlString = baseApplicationUrl.toString();
        StringBuilder sb = new StringBuilder(baseUrlString.substring(0, baseUrlString.length() - 1));
        Arrays.stream(paths).forEach(path -> sb.append(path.startsWith("/") ? path : "/" + path));
        return sb.toString();
    }

    private LRAResult getParticipantResult(String participantPath) {
        return given()
                .when().get(getURL(participantPath, LRAParticipant.RESULT_PATH))
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract().as(LRAResult.class);
    }

    private void resetParticipantResult(String participantPath) {
        given()
                .when().put(getURL(participantPath, LRAParticipant.RESET_PATH))
                .then()
                .statusCode(Response.Status.NO_CONTENT.getStatusCode());
    }

    private void resetCounter() {
        given()
                .when()
                .get(getURL(LRAParticipantWithFaultTolerance1.PATH, LRAParticipantWithFaultTolerance1.RESET_COUNTER_PATH))
                .then()
                .statusCode(Response.Status.NO_CONTENT.getStatusCode());
    }

    private void verifyResult(LRAResult lraResult, boolean shouldBeCompleted) {
        if (shouldBeCompleted) {
            assertTrue("LRA must be completed but instead it was compensated (rolled back).",
                    lraResult.isCompleted());
        } else {
            assertFalse("LRA was completed but instead it should be compensated (rolled back).",
                    lraResult.isCompleted());
        }
    }
}
