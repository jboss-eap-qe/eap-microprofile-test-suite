package org.jboss.eap.qe.microprofile.lra;

import static org.jboss.eap.qe.microprofile.lra.LRAParticipantWithFaultTolerance1.PATH;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;
import org.jboss.logging.Logger;

@Path(PATH)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
public class LRAParticipantWithFaultTolerance1 extends LRAParticipant {

    private static final Logger LOGGER = Logger.getLogger(LRAParticipantWithFaultTolerance1.class);

    public static final String PATH = "lra-participant-with-fault-tolerance-1";
    public static final String RETRY_PATH = "work-with-retry";
    public static final String TIMEOUT_PATH = "work-with-timeout";
    public static final String RETRY_COUNT_PATH = "retry-count";
    public static final String FAIL_LRA = "fail-lra";
    public static final String RESET_COUNTER_PATH = "reset-counter";
    public static final String CIRCUIT_BREAKER_PATH = "work-with-circuit-breaker";
    public static final String FALLBACK_PATH = "work-with-fallback";
    public static final String LRA_PROPAGATION_BASE_URL_HEADER = "Lra-Propagation-Base-Url";

    private final AtomicInteger retryCount = new AtomicInteger(0);

    @LRA(value = LRA.Type.REQUIRED)
    @GET
    @Path(RETRY_PATH)
    @Retry(maxRetries = 3, delay = 100)
    public Response workWithRetry(@HeaderParam(LRA.LRA_HTTP_CONTEXT_HEADER) URI lraId,
            @HeaderParam(LRA.LRA_HTTP_RECOVERY_HEADER) URI recoveryId,
            @HeaderParam(LRAParticipantWithFaultTolerance1.LRA_PROPAGATION_BASE_URL_HEADER) URI lraPropagationBaseUrl,
            @QueryParam(LRAParticipantWithFaultTolerance1.FAIL_LRA) boolean failLRA) throws Exception {

        LOGGER.infof("Executing action of LRAParticipantWithFaultTolerance1 enlisted in LRA %s " +
                "that was assigned %s participant Id. Retried: %s", lraId, recoveryId, retryCount.incrementAndGet());

        // check lraId and recoveryId is consistent across retries otherwise return response to fail the test
        if (retryCount.get() > 1 && !lraId.equals(lraResult.getLraId())) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Wrong lraId across retries. Expected " + lraResult.getLraId() + " but got " + lraId).build();
        }

        if (retryCount.get() > 1 && !recoveryId.equals(lraResult.getRecoveryId())) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Wrong recoveryId across retries. Expected " + lraResult.getRecoveryId() + " but got " + recoveryId)
                    .build();
        }

        lraResult.setLraId(lraId);
        lraResult.setRecoveryId(recoveryId);

        // call Participant 2 to propagate the LRA
        try (Client client = ClientBuilder.newClient()) {
            client.target(lraPropagationBaseUrl + LRAParticipantWithFaultTolerance2.PATH)
                    .request()
                    .get();
        }

        if (failLRA) {
            throw new Exception("Throwing exception to fail participant until maxRetry is reached.");
        }

        return Response.ok("Method with @Retry ended successfully.").build();
    }

    @LRA(value = LRA.Type.REQUIRED)
    @GET
    @Path(TIMEOUT_PATH)
    @Timeout(1000)
    public Response workWithRetry(@HeaderParam(LRA.LRA_HTTP_CONTEXT_HEADER) URI lraId,
            @HeaderParam(LRA.LRA_HTTP_RECOVERY_HEADER) URI recoveryId,
            @HeaderParam(LRAParticipantWithFaultTolerance1.LRA_PROPAGATION_BASE_URL_HEADER) URI lraPropagationBaseUrl)
            throws Exception {

        LOGGER.infof("Executing action of LRAParticipantWithFaultTolerance1 enlisted in LRA %s " +
                "that was assigned %s participant Id. Retried: %s", lraId, recoveryId, retryCount.incrementAndGet());

        lraResult.setLraId(lraId);
        lraResult.setRecoveryId(recoveryId);

        // call Participant 2 to propagate the LRA
        try (Client client = ClientBuilder.newClient()) {
            client.target(lraPropagationBaseUrl + LRAParticipantWithFaultTolerance2.PATH)
                    .request()
                    .get();
        }

        // Cause timeout of this method so exception is thrown and LRA is compensated
        Thread.sleep(Duration.ofSeconds(10).toMillis());

        return Response.ok("Method with @Timeout ended successfully.").build();
    }

    /**
     * If 2 or more calls from last 10 calls fail then circuit is open.
     * If circuit is open and 1 call is successful (after 1000 ms) delay then circuit is closed again.
     */
    @LRA(value = LRA.Type.REQUIRED)
    @GET
    @Path(CIRCUIT_BREAKER_PATH)
    @CircuitBreaker(requestVolumeThreshold = 10, failureRatio = 0.2, delay = 1000, successThreshold = 10)
    public Response workWithCircuitBreaker(@HeaderParam(LRA.LRA_HTTP_CONTEXT_HEADER) URI lraId,
            @HeaderParam(LRA.LRA_HTTP_RECOVERY_HEADER) URI recoveryId,
            @HeaderParam(LRAParticipantWithFaultTolerance1.LRA_PROPAGATION_BASE_URL_HEADER) URI lraPropagationBaseUrl,
            @QueryParam(LRAParticipantWithFaultTolerance1.FAIL_LRA) boolean failLRA) throws Exception {
        LOGGER.infof("Executing action of LRAParticipantWithFaultTolerance1 enlisted in LRA %s " +
                "that was assigned %s participant Id. Retried: %s", lraId, recoveryId, retryCount.incrementAndGet());

        lraResult.setLraId(lraId);
        lraResult.setRecoveryId(recoveryId);

        // call Participant 2 to propagate the LRA
        try (Client client = ClientBuilder.newClient()) {
            client.target(lraPropagationBaseUrl + LRAParticipantWithFaultTolerance2.PATH)
                    .request()
                    .get();
        }

        if (failLRA) {
            throw new Exception("Throwing exception to fail participant until circuit is open.");
        }

        return Response.ok("Method with @CircuitBreaker ended successfully.").build();
    }

    @LRA(value = LRA.Type.REQUIRED)
    @GET
    @Path(FALLBACK_PATH)
    @Fallback(fallbackMethod = "fallback")
    public Response workWithFallback(@HeaderParam(LRA.LRA_HTTP_CONTEXT_HEADER) URI lraId,
            @HeaderParam(LRA.LRA_HTTP_RECOVERY_HEADER) URI recoveryId,
            @HeaderParam(LRAParticipantWithFaultTolerance1.LRA_PROPAGATION_BASE_URL_HEADER) URI lraPropagationBaseUrl,
            @QueryParam(LRAParticipantWithFaultTolerance1.FAIL_LRA) boolean failLRA) throws Exception {

        LOGGER.infof("Executing action of LRAParticipantWithFaultTolerance1 enlisted in LRA %s " +
                "that was assigned %s participant Id. Retried: %s", lraId, recoveryId, retryCount.incrementAndGet());

        lraResult.setLraId(lraId);
        lraResult.setRecoveryId(recoveryId);

        // call Participant 2 to propagate the LRA
        try (Client client = ClientBuilder.newClient()) {
            client.target(lraPropagationBaseUrl + LRAParticipantWithFaultTolerance2.PATH)
                    .request()
                    .get();
        }
        if (failLRA) {
            throw new Exception("Throwing exception to fail participant so fallback method is called.");
        }
        return Response.ok("Method ended successfully without calling method in @Fallback.").build();
    }

    public Response fallback(@HeaderParam(LRA.LRA_HTTP_CONTEXT_HEADER) URI lraId,
            @HeaderParam(LRA.LRA_HTTP_RECOVERY_HEADER) URI recoveryId,
            @HeaderParam(LRAParticipantWithFaultTolerance1.LRA_PROPAGATION_BASE_URL_HEADER) URI lraPropagationBaseUrl,
            @QueryParam(LRAParticipantWithFaultTolerance1.FAIL_LRA) boolean failLRA) {
        // check lraId and recoveryId is the same
        if (!lraId.equals(lraResult.getLraId())) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Wrong lraId across retries. Expected " + lraResult.getLraId() + " but got " + lraId).build();
        }

        if (!recoveryId.equals(lraResult.getRecoveryId())) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Wrong recoveryId across retries. Expected " + lraResult.getRecoveryId() + " but got " + recoveryId)
                    .build();
        }
        lraResult.setLraId(lraId);
        lraResult.setRecoveryId(recoveryId);
        return Response.ok("Method defined in @Fallback ended successfully.").build();
    }

    @GET
    @Path(RETRY_COUNT_PATH)
    public int getRetryCount() {
        return retryCount.get();
    }

    @GET
    @Path(RESET_COUNTER_PATH)
    public void resetCounter() {
        retryCount.set(0);
    }
}
