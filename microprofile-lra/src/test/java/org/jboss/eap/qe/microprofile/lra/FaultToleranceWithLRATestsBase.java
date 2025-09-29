package org.jboss.eap.qe.microprofile.lra;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

import jakarta.ws.rs.core.Response;

import java.net.URL;
import java.time.Duration;

import org.jboss.eap.qe.microprofile.lra.model.LRAResult;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Base class that provides the methods to verify fault tolerance behavior with Long Running Actions (LRA).
 */
public abstract class FaultToleranceWithLRATestsBase {

    public static final String DEPLOYMENT_NAME = "microprofile-lra";

    @Before
    public void before() {
        resetCounter();
        resetParticipant1Result();
        resetParticipant2Result();
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
                .header(
                        LRAParticipantWithFaultTolerance1.LRA_PROPAGATION_BASE_URL_HEADER,
                        getLRAPropagationBaseUrl().toString())
                .queryParam(LRAParticipantWithFaultTolerance1.FAIL_LRA, true)
                .when()
                .get(getParticipant1URL(LRAParticipantWithFaultTolerance1.PATH,
                        LRAParticipantWithFaultTolerance1.RETRY_PATH))
                .then()
                .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())
                .body(containsString("Throwing exception to fail participant until maxRetry is reached"));

        // check that retry is higher than 1
        int numberOfRetries = Integer
                .parseInt(
                        get(getParticipant1URL(LRAParticipantWithFaultTolerance1.PATH,
                                LRAParticipantWithFaultTolerance1.RETRY_COUNT_PATH))
                                .body().print());
        Assert.assertTrue("Number of retries must be higher than 1 but it's " + numberOfRetries, numberOfRetries > 1);

        // check tx was compensated
        LRATestUtilities.verifyResult(getParticipant1Result(), false);
        LRATestUtilities.verifyResult(getParticipant2Result(), false);
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
                .header(
                        LRAParticipantWithFaultTolerance1.LRA_PROPAGATION_BASE_URL_HEADER,
                        getLRAPropagationBaseUrl().toString())
                .queryParam(LRAParticipantWithFaultTolerance1.FAIL_LRA, false)
                .when()
                .get(getParticipant1URL(LRAParticipantWithFaultTolerance1.PATH,
                        LRAParticipantWithFaultTolerance1.RETRY_PATH))
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body(containsString("Method with @Retry ended successfully"));

        // check that there was no retry (i.e. retry is equal to 1)
        int numberOfRetries = Integer
                .parseInt(
                        get(getParticipant1URL(LRAParticipantWithFaultTolerance1.PATH,
                                LRAParticipantWithFaultTolerance1.RETRY_COUNT_PATH))
                                .body().print());
        Assert.assertEquals("Number of retries must be one.", 1, numberOfRetries);

        // check tx was completed
        LRATestUtilities.verifyResult(getParticipant1Result(), true);
        LRATestUtilities.verifyResult(getParticipant2Result(), true);
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
                .header(
                        LRAParticipantWithFaultTolerance1.LRA_PROPAGATION_BASE_URL_HEADER,
                        getLRAPropagationBaseUrl().toString())
                .queryParam(LRAParticipantWithFaultTolerance1.FAIL_LRA, false)
                .when()
                .get(getParticipant1URL(LRAParticipantWithFaultTolerance1.PATH,
                        LRAParticipantWithFaultTolerance1.TIMEOUT_PATH))
                .then()
                .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())
                .body(containsString("timed out"));

        // check tx was compensated
        LRATestUtilities.verifyResult(getParticipant1Result(), false);
        LRATestUtilities.verifyResult(getParticipant2Result(), false);
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
                .header(
                        LRAParticipantWithFaultTolerance1.LRA_PROPAGATION_BASE_URL_HEADER,
                        getLRAPropagationBaseUrl().toString())
                .queryParam(LRAParticipantWithFaultTolerance1.FAIL_LRA, false)
                .when()
                .get(getParticipant1URL(LRAParticipantWithFaultTolerance1.PATH,
                        LRAParticipantWithFaultTolerance1.CIRCUIT_BREAKER_PATH))
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body(containsString("Method with @CircuitBreaker ended successfully"));

        // now call it until the circuit is open
        int counter = 0;
        while (!given()
                .header(
                        LRAParticipantWithFaultTolerance1.LRA_PROPAGATION_BASE_URL_HEADER,
                        getLRAPropagationBaseUrl().toString())
                .queryParam(LRAParticipantWithFaultTolerance1.FAIL_LRA, true)
                .when()
                .get(getParticipant1URL(LRAParticipantWithFaultTolerance1.PATH,
                        LRAParticipantWithFaultTolerance1.CIRCUIT_BREAKER_PATH))
                .body().print().contains("circuit breaker is open")) {
            if (counter++ > 10) {
                Assert.fail("Circuit breaker must be open now.");
            }
        }

        // check last tx was compensated
        LRATestUtilities.verifyResult(getParticipant1Result(), false);
        LRATestUtilities.verifyResult(getParticipant2Result(), false);

        // After 1 sec circuit gets to half open state and then any new successful call will close it again. Call it until any of the calls succeeds.
        long timeout = Duration.ofSeconds(10).toMillis();
        long startTime = System.currentTimeMillis();
        while (given()
                .header(
                        LRAParticipantWithFaultTolerance1.LRA_PROPAGATION_BASE_URL_HEADER,
                        getLRAPropagationBaseUrl().toString())
                .queryParam(LRAParticipantWithFaultTolerance1.FAIL_LRA, false)
                .when()
                .get(getParticipant1URL(LRAParticipantWithFaultTolerance1.PATH,
                        LRAParticipantWithFaultTolerance1.CIRCUIT_BREAKER_PATH))
                .statusCode() != 200) {
            if (System.currentTimeMillis() - startTime > timeout) {
                Assert.fail("Circuit breaker did not close in " + timeout + "ms timeout even though it should.");
            }
            Thread.sleep(200);
        }

        // call once more to check circuit is closed and tx finishes as completed
        given()
                .header(
                        LRAParticipantWithFaultTolerance1.LRA_PROPAGATION_BASE_URL_HEADER,
                        getLRAPropagationBaseUrl().toString())
                .queryParam(LRAParticipantWithFaultTolerance1.FAIL_LRA, false)
                .when()
                .get(getParticipant1URL(LRAParticipantWithFaultTolerance1.PATH,
                        LRAParticipantWithFaultTolerance1.CIRCUIT_BREAKER_PATH))
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body(containsString("Method with @CircuitBreaker ended successfully"));

        // check last tx was completed
        LRATestUtilities.verifyResult(getParticipant1Result(), true);
        LRATestUtilities.verifyResult(getParticipant2Result(), true);
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
                .header(
                        LRAParticipantWithFaultTolerance1.LRA_PROPAGATION_BASE_URL_HEADER,
                        getLRAPropagationBaseUrl().toString())
                .queryParam(LRAParticipantWithFaultTolerance1.FAIL_LRA, true)
                .when()
                .get(getParticipant1URL(LRAParticipantWithFaultTolerance1.PATH,
                        LRAParticipantWithFaultTolerance1.FALLBACK_PATH))
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body(containsString("Method defined in @Fallback ended successfully"));

        // verify tx is completed
        LRATestUtilities.verifyResult(getParticipant1Result(), true);
        LRATestUtilities.verifyResult(getParticipant2Result(), true);
    }

    protected abstract URL getLRAPropagationBaseUrl();

    protected abstract String getParticipant1URL(String... paths);

    protected abstract LRAResult getParticipant1Result();

    protected abstract LRAResult getParticipant2Result();

    protected abstract void resetParticipant1Result();

    protected abstract void resetParticipant2Result();

    protected void resetCounter() {
        given()
                .when()
                .get(getParticipant1URL(LRAParticipantWithFaultTolerance1.PATH,
                        LRAParticipantWithFaultTolerance1.RESET_COUNTER_PATH))
                .then()
                .statusCode(Response.Status.NO_CONTENT.getStatusCode());
    }
}
