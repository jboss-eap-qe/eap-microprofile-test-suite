package org.jboss.eap.qe.microprofile.lra;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.jboss.eap.qe.microprofile.lra.LRAController.PROPAGATE_CHAIN;
import static org.jboss.eap.qe.microprofile.lra.LRAController.PROPAGATE_MANUAL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;

import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.eap.qe.microprofile.lra.model.LRAResult;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.MicroProfileServerSetupTask;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.deployment.ConfigurationUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;

import io.restassured.response.Response;

@RunWith(Arquillian.class)
@ServerSetup({ EnableLRASetupTask.class })
public class LRAPropagationTest {

    @ArquillianResource
    private URL baseApplicationUrl;

    @Deployment(testable = false)
    public static Archive<?> deployment() {
        return ShrinkWrap.create(WebArchive.class, LRAPropagationTest.class.getSimpleName() + ".war")
                .addPackages(true, LRAController.class.getPackage())
                .addClasses(EnableLRASetupTask.class, MicroProfileServerSetupTask.class, ServerSetupTask.class)
                .addAsWebInfResource(ConfigurationUtil.BEANS_XML_FILE_LOCATION, "beans.xml");
    }

    @Before
    public void beforeEach() {
        resetParticipantResult(LRAManualPropagationParticipant.PATH);
        resetParticipantResult(LRAChainPropagationParticipant1.PATH);
        resetParticipantResult(LRAChainPropagationParticipant2.PATH);
    }

    @After
    public void afterEach() throws Exception {
        try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
            String lraCoordinatorURL = client
                    .execute("/subsystem=microprofile-lra-participant:read-attribute(name=lra-coordinator-url)")
                    .stringValue();

            Response response = given().when().get(lraCoordinatorURL);
            assertEquals(200, response.statusCode());
            String result = response.asString();
            assertTrue("Some LRAs didn't end: " + result, result.equals("[]"));
        }
    }

    /**
     * @tpTestDetails Scenario where the LRA is started and ended by the same resource in different methods.
     * @tpPassCrit LRA is successfully started and the lraId is returned with which it can be closed.
     * @tpSince EAP XP 5.0.0
     */
    @Test
    public void testLRASuccessNoPropagation() {
        String lraIdString = startLRA();
        parseAsURI(lraIdString);

        endLRA(lraIdString, false);
    }

    /**
     * @tpTestDetails Scenario where the LRA enlists one participant with direct call.
     * @tpPassCrit LRA is started and closed successfully and the participant complete callback is invoked.
     * @tpSince EAP XP 5.0.0
     */
    @Test
    public void testLRASuccessManualPropagation() {
        String lraIdString = startLRA();
        URI lraId = parseAsURI(lraIdString);

        callParticipant(lraIdString, PROPAGATE_MANUAL, false);

        endLRA(lraIdString, false);

        LRAResult lraManualParticipantResult = getParticipantResult(LRAManualPropagationParticipant.PATH);
        verifyLRAResult(lraId, lraManualParticipantResult, true);
    }

    /**
     * @tpTestDetails Scenario where the LRA enlists two participants with a chained call.
     * @tpPassCrit LRA is started and closed successfully and both participants complete callbacks are invoked.
     * @tpSince EAP XP 5.0.0
     */
    @Test
    public void testLRASuccessChainPropagation() {
        String lraIdString = startLRA();
        URI lraId = parseAsURI(lraIdString);

        callParticipant(lraIdString, PROPAGATE_CHAIN, false);

        endLRA(lraIdString, false);

        LRAResult lraChainParticipant1Result = getParticipantResult(LRAChainPropagationParticipant1.PATH);
        LRAResult lraChainParticipant2Result = getParticipantResult(LRAChainPropagationParticipant2.PATH);

        verifyLRAResult(lraId, lraChainParticipant1Result, true);
        verifyLRAResult(lraId, lraChainParticipant2Result, true);
    }

    /**
     * @tpTestDetails Scenario where the LRA enlists three participants - one directly and two with a chained call.
     * @tpPassCrit LRA is started and closed successfully and all three participants complete callbacks are invoked.
     * @tpSince EAP XP 5.0.0
     */
    @Test
    public void testLRASuccessManualAndChainPropagation() {
        String lraIdString = startLRA();
        URI lraId = parseAsURI(lraIdString);

        callParticipant(lraIdString, PROPAGATE_MANUAL, false);
        callParticipant(lraIdString, PROPAGATE_CHAIN, false);

        endLRA(lraIdString, false);

        LRAResult lraManualParticipantResult = getParticipantResult(LRAManualPropagationParticipant.PATH);
        LRAResult lraChainParticipant1Result = getParticipantResult(LRAChainPropagationParticipant1.PATH);
        LRAResult lraChainParticipant2Result = getParticipantResult(LRAChainPropagationParticipant2.PATH);

        verifyLRAResult(lraId, lraManualParticipantResult, true);
        verifyLRAResult(lraId, lraChainParticipant1Result, true);
        verifyLRAResult(lraId, lraChainParticipant2Result, true);
    }

    /**
     * @tpTestDetails Scenario where the LRA is started and cancelled with two different methods of the same resource.
     * @tpPassCrit LRA is started and cancelled without issues.
     * @tpSince EAP XP 5.0.0
     */
    @Test
    public void testLRAFailureNoPropagation() {
        String lraIdString = startLRA();
        parseAsURI(lraIdString);

        endLRA(lraIdString, true);
    }

    /**
     * @tpTestDetails Scenario where the LRA enlists one participant with a direct call and cancels.
     * @tpPassCrit LRA is started and cancelled and the participant compensate callback is invoked.
     * @tpSince EAP XP 5.0.0
     */
    @Test
    public void testLRAFailureManualPropagation() {
        String lraIdString = startLRA();
        URI lraId = parseAsURI(lraIdString);

        callParticipant(lraIdString, PROPAGATE_MANUAL, false);

        endLRA(lraIdString, true);

        LRAResult lraManualParticipantResult = getParticipantResult(LRAManualPropagationParticipant.PATH);

        verifyLRAResult(lraId, lraManualParticipantResult, false);
    }

    /**
     * @tpTestDetails Scenario where the LRA enlists two participants with a chained call and cancels.
     * @tpPassCrit LRA is started and cancelled and the two participants compensate callbacks are invoked.
     * @tpSince EAP XP 5.0.0
     */
    @Test
    public void testLRAFailureChainPropagation() {
        String lraIdString = startLRA();
        URI lraId = parseAsURI(lraIdString);

        callParticipant(lraIdString, PROPAGATE_CHAIN, false);

        endLRA(lraIdString, true);

        LRAResult lraChainParticipant1Result = getParticipantResult(LRAChainPropagationParticipant1.PATH);
        LRAResult lraChainParticipant2Result = getParticipantResult(LRAChainPropagationParticipant2.PATH);

        verifyLRAResult(lraId, lraChainParticipant1Result, false);
        verifyLRAResult(lraId, lraChainParticipant2Result, false);
    }

    /**
     * @tpTestDetails Scenario where the LRA enlists three participants - one directly and two with a chained call and cancels.
     * @tpPassCrit LRA is started and cancelled and all three participants compensate callbacks are invoked.
     * @tpSince EAP XP 5.0.0
     */
    @Test
    public void testLRAFailureManualAndChainPropagation() {
        String lraIdString = startLRA();
        URI lraId = parseAsURI(lraIdString);

        callParticipant(lraIdString, PROPAGATE_MANUAL, false);
        callParticipant(lraIdString, PROPAGATE_CHAIN, false);

        endLRA(lraIdString, true);

        LRAResult lraManualParticipantResult = getParticipantResult(LRAManualPropagationParticipant.PATH);
        LRAResult lraChainParticipant1Result = getParticipantResult(LRAChainPropagationParticipant1.PATH);
        LRAResult lraChainParticipant2Result = getParticipantResult(LRAChainPropagationParticipant2.PATH);

        verifyLRAResult(lraId, lraManualParticipantResult, false);
        verifyLRAResult(lraId, lraChainParticipant1Result, false);
        verifyLRAResult(lraId, lraChainParticipant2Result, false);
    }

    /**
     * @tpTestDetails Scenario where the LRA is cancelled by a directly invoked participant.
     * @tpPassCrit LRA is started and cancelled by the participant and the participant's compensate callback is called.
     * @tpSince EAP XP 5.0.0
     */
    @Test
    public void testLRAParticipantFailureManual() {
        String lraIdString = startLRA();
        URI lraId = parseAsURI(lraIdString);

        // this call will fail the manual participant and whole LRA
        callParticipant(lraIdString, PROPAGATE_MANUAL, true);

        LRAResult lraManualParticipantResult = getParticipantResult(LRAManualPropagationParticipant.PATH);

        verifyLRAResult(lraId, lraManualParticipantResult, false);
    }

    /**
     * @tpTestDetails Scenario where the LRA is cancelled by a second participant in chain call.
     * @tpPassCrit LRA is started and cancelled by the last participant in the chain and all compensate callbacks are called.
     * @tpSince EAP XP 5.0.0
     */
    @Test
    public void testLRAParticipantFailureChain() {
        String lraIdString = startLRA();
        URI lraId = parseAsURI(lraIdString);

        // this call will fail the second participant in chain and whole LRA
        callParticipant(lraIdString, PROPAGATE_CHAIN, true);

        LRAResult lraChainParticipant1Result = getParticipantResult(LRAChainPropagationParticipant1.PATH);
        LRAResult lraChainParticipant2Result = getParticipantResult(LRAChainPropagationParticipant2.PATH);

        System.out.println("lraChainParticipant1Result = " + lraChainParticipant1Result);
        System.out.println("lraChainParticipant2Result = " + lraChainParticipant2Result);

        verifyLRAResult(lraId, lraChainParticipant1Result, false);
        verifyLRAResult(lraId, lraChainParticipant2Result, false);
    }

    /**
     * @tpTestDetails Scenario where the LRA is cancelled by a directly called participant and then another participant tries to
     *                enlist.
     * @tpPassCrit LRA is started and cancelled by the participant and any subsequent enlistments are rejected.
     * @tpSince EAP XP 5.0.0
     */
    @Test
    public void testLRAParticipantManualFailureDoesntAllowNewParticipantRegistrations() {
        String lraIdString = startLRA();
        URI lraId = parseAsURI(lraIdString);

        // Fail the LRA with manual participant
        callParticipant(lraIdString, PROPAGATE_MANUAL, true);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // New registrations shouldn't be allowed, Narayana responds with 404 if LRA is not found
        given().header(LRA.LRA_HTTP_CONTEXT_HEADER, lraIdString)
                .when().get(getURL(LRAController.LRA_PATH, PROPAGATE_CHAIN))
                .then().statusCode(404);

        LRAResult lraManualParticipantResult = getParticipantResult(LRAManualPropagationParticipant.PATH);
        LRAResult lraChainParticipant1Result = getParticipantResult(LRAChainPropagationParticipant1.PATH);
        LRAResult lraChainParticipant2Result = getParticipantResult(LRAChainPropagationParticipant2.PATH);

        verifyLRAResult(lraId, lraManualParticipantResult, false);
        assertNull(lraChainParticipant1Result.getLraId());
        assertNull(lraChainParticipant2Result.getLraId());
    }

    private String getURL(String... paths) {
        String baseUrlString = baseApplicationUrl.toString();
        StringBuilder sb = new StringBuilder(baseUrlString.substring(0, baseUrlString.length() - 1));
        Arrays.stream(paths).forEach(path -> sb.append(path.startsWith("/") ? path : "/" + path));
        return sb.toString();
    }

    private static URI parseAsURI(String lraIdString) {
        assertNotNull(lraIdString);
        URI lraId = null;
        try {
            lraId = new URI(lraIdString);
        } catch (URISyntaxException e) {
            fail("Returned lraId is not parsable to URI: " + e.getMessage());
        }
        return lraId;
    }

    private String startLRA() {
        return given()
                .header(LRAController.BASE_URI, baseApplicationUrl.toString())
                .when()
                .get(getURL(LRAController.LRA_PATH))
                .then()
                .statusCode(200)
                .extract().asString();
    }

    private void endLRA(String lraIdString, boolean failLRA) {
        given()
                .queryParam(LRAController.FAIL_LRA, failLRA)
                .header(LRA.LRA_HTTP_CONTEXT_HEADER, lraIdString)
                .when().get(getURL(LRAController.LRA_PATH, LRAController.END_LRA_PATH))
                .then()
                .statusCode(failLRA ? 400 : 200)
                .body(is(lraIdString));
    }

    private void callParticipant(String lraIdString, String path, boolean fail) {
        given().header(LRA.LRA_HTTP_CONTEXT_HEADER, lraIdString)
                .queryParam(LRAController.FAIL_LRA, fail)
                .when().get(getURL(LRAController.LRA_PATH, path))
                .then().statusCode(204);
    }

    private LRAResult getParticipantResult(String participantPath) {
        return given()
                .when().get(baseApplicationUrl + participantPath + LRAParticipant.RESULT_PATH)
                .then()
                .statusCode(200)
                .extract().as(LRAResult.class);
    }

    private void resetParticipantResult(String participantPath) {
        given()
                .when().put(baseApplicationUrl + participantPath + LRAParticipant.RESET_PATH)
                .then()
                .statusCode(204);
    }

    private static void verifyLRAResult(URI lraId, LRAResult result, boolean completed) {
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
}
