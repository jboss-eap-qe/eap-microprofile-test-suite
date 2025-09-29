package org.jboss.eap.qe.microprofile.lra;

import static io.restassured.RestAssured.given;
import static org.jboss.eap.qe.microprofile.lra.LRAController.PROPAGATE_CHAIN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;

import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;
import org.jboss.eap.qe.microprofile.lra.model.LRAResult;

/**
 * Provides utility methods used by the LRA test cases.
 */
public class LRATestUtilities {

    /**
     * Validates a LRA result.
     *
     * @param lraId The {@link URI} instance identifying the tested LRA
     * @param result The {@link LRAResult} instance representing the tested LRA result
     * @param completed Whether the tested LRA was expected to be completed or compensated
     */
    public static void verifyLRAResult(URI lraId, LRAResult result, boolean completed) {
        assertEquals("LRA propagated incorrect lraId", lraId, result.getLraId());
        assertTrue(String.format("LRA should have invoke participant %s callback", completed ? "Complete" : "Compensate"),
                completed ? result.isCompleted() : !result.isCompleted());
        assertEquals("LRA didn't propagate correct lraId to the participant finish callback", lraId,
                result.getFinishLraId());
        assertEquals("LRA propagated incorrect recoveryId to finish callback",
                result.getRecoveryId(), result.getFinishRecoveryId());
        assertEquals("LRA didn't propagate correct lraId to the participant afterLRA callback", lraId,
                result.getAfterLraId());
    }

    /**
     * Validates a LRA result.
     *
     * @param lraResult The {@link LRAResult} instance representing the tested LRA result
     * @param shouldBeCompleted Whether the tested LRA was expected to be completed
     */
    public static void verifyResult(LRAResult lraResult, boolean shouldBeCompleted) {
        if (shouldBeCompleted) {
            assertTrue("LRA must be completed but instead it was compensated (rolled back).",
                    lraResult.isCompleted());
        } else {
            assertFalse("LRA was completed but instead it should be compensated (rolled back).",
                    lraResult.isCompleted());
        }
    }

    /**
     * Verify that a subsequent registration of the participant shouldn't be allowed. Narayana responds with 404 if
     * an LRA is not found.
     *
     * @param baseURL The {@link URL} instance representing the coordinator base URL
     * @param lraIdString The string representing the tested LRA {@link URI}
     */
    public static void verifyNewRegistrationsNotAllowed(final URL baseURL, String lraIdString) {
        given().header(LRA.LRA_HTTP_CONTEXT_HEADER, lraIdString)
                .when().get(LRATestUtilities.getURL(baseURL, LRAController.LRA_PATH, PROPAGATE_CHAIN))
                .then().statusCode(404);
    }

    /**
     * Calls a LRA participant REST API to reset the participant result, i.e. the {@link LRAResult} that will be
     * returned by the participant.
     *
     * @param participantBaseUrl The {@link URL} instance representing the participant base URL
     * @param participantPath The participant specific path
     */
    public static void resetParticipantResult(final URL participantBaseUrl, String participantPath) {
        given()
                .when().put(participantBaseUrl + participantPath + LRAParticipant.RESET_PATH)
                .then()
                .statusCode(204);
    }

    /**
     * Calls a LRA participant REST API to get the LRA result, once it is either completed or compensated.
     *
     * @param participantBaseUrl The {@link URL} instance representing the participant base URL
     * @param participantPath The participant specific path
     *
     * @return A {@link LRAResult} instance identifying the participant result once a LRA is either completed or
     *         compensated.
     */
    public static LRAResult getParticipantResult(final URL participantBaseUrl, String participantPath) {
        return given()
                .when().get(participantBaseUrl + participantPath + LRAParticipant.RESULT_PATH)
                .then()
                .statusCode(200)
                .extract().as(LRAResult.class);
    }

    /**
     * Given a base {@link URL} and a variable number of relative paths, builds a full URL to call a given REST API
     *
     * @param baseUrl The {@link URL} instance representing a base URL
     * @param paths A variable number of relative paths
     * @return A string representing the full URL to call a given REST API
     */
    public static String getURL(final URL baseUrl, String... paths) {
        String baseUrlString = baseUrl.toString();
        StringBuilder sb = new StringBuilder(baseUrlString.substring(0, baseUrlString.length() - 1));
        Arrays.stream(paths).forEach(path -> sb.append(path.startsWith("/") ? path : "/" + path));
        return sb.toString();
    }

    /**
     * Calls a participant to join a LRA.
     *
     * @param participantBaseUrl The {@link URL} instance representing the participant base URL
     * @param lraIdString The string representing the tested LRA {@link URI}
     * @param path The participant specific path
     * @param fail Whether the participant should fail the LRA
     */
    public static void callParticipant(final URL participantBaseUrl, String lraIdString, String path, boolean fail) {
        given().header(LRA.LRA_HTTP_CONTEXT_HEADER, lraIdString)
                .queryParam(LRAController.FAIL_LRA, fail)
                .when().get(LRATestUtilities.getURL(participantBaseUrl, LRAController.LRA_PATH, path))
                .then().statusCode(204);
    }

    /**
     * Parses a string identifying a LRA into an {@link URI} instance
     *
     * @param lraIdString String identifying a LRA into an {@link URI} instance
     * @return A {@link URI} instance identifying a given LRA
     */
    public static URI parseAsURI(String lraIdString) {
        assertNotNull(lraIdString);
        URI lraId = null;
        try {
            lraId = new URI(lraIdString);
        } catch (URISyntaxException e) {
            fail("Returned lraId is not parsable to URI: " + e.getMessage());
        }
        return lraId;
    }
}
