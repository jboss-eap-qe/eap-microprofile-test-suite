package org.jboss.eap.qe.microprofile.lra;

import static io.restassured.RestAssured.given;
import static org.jboss.eap.qe.microprofile.lra.LRAController.PROPAGATE_CHAIN;
import static org.jboss.eap.qe.microprofile.lra.LRAController.PROPAGATE_MANUAL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.URI;

import org.jboss.eap.qe.microprofile.lra.model.LRAResult;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;

import io.restassured.response.Response;

/**
 * Base class that provides the methods to verify LRA propagation with manual and chained participants.
 */
public abstract class LRAPropagationTestsBase {

    @Before
    public void beforeEach() {
        resetManualParticipantResult();
        resetChainParticipant1Result();
        resetChainParticipant2Result();
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
        LRATestUtilities.parseAsURI(lraIdString);

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
        URI lraId = LRATestUtilities.parseAsURI(lraIdString);

        callParticipant(lraIdString, PROPAGATE_MANUAL, false);

        endLRA(lraIdString, false);

        LRAResult lraManualParticipantResult = getManualParticipantResult();
        LRATestUtilities.verifyLRAResult(lraId, lraManualParticipantResult, true);
    }

    /**
     * @tpTestDetails Scenario where the LRA enlists two participants with a chained call.
     * @tpPassCrit LRA is started and closed successfully and both participants complete callbacks are invoked.
     * @tpSince EAP XP 5.0.0
     */
    @Test
    public void testLRASuccessChainPropagation() {
        String lraIdString = startLRA();
        URI lraId = LRATestUtilities.parseAsURI(lraIdString);

        callParticipant(lraIdString, PROPAGATE_CHAIN, false);

        endLRA(lraIdString, false);

        LRAResult lraChainParticipant1Result = getChainParticipant1Result();
        LRAResult lraChainParticipant2Result = getChainParticipant2Result();

        LRATestUtilities.verifyLRAResult(lraId, lraChainParticipant1Result, true);
        LRATestUtilities.verifyLRAResult(lraId, lraChainParticipant2Result, true);
    }

    /**
     * @tpTestDetails Scenario where the LRA enlists three participants - one directly and two with a chained call.
     * @tpPassCrit LRA is started and closed successfully and all three participants complete callbacks are invoked.
     * @tpSince EAP XP 5.0.0
     */
    @Test
    public void testLRASuccessManualAndChainPropagation() {
        String lraIdString = startLRA();
        URI lraId = LRATestUtilities.parseAsURI(lraIdString);

        callParticipant(lraIdString, PROPAGATE_MANUAL, false);
        callParticipant(lraIdString, PROPAGATE_CHAIN, false);

        endLRA(lraIdString, false);

        LRAResult lraManualParticipantResult = getManualParticipantResult();
        LRAResult lraChainParticipant1Result = getChainParticipant1Result();
        LRAResult lraChainParticipant2Result = getChainParticipant2Result();

        LRATestUtilities.verifyLRAResult(lraId, lraManualParticipantResult, true);
        LRATestUtilities.verifyLRAResult(lraId, lraChainParticipant1Result, true);
        LRATestUtilities.verifyLRAResult(lraId, lraChainParticipant2Result, true);
    }

    /**
     * @tpTestDetails Scenario where the LRA is started and cancelled with two different methods of the same resource.
     * @tpPassCrit LRA is started and cancelled without issues.
     * @tpSince EAP XP 5.0.0
     */
    @Test
    public void testLRAFailureNoPropagation() {
        String lraIdString = startLRA();
        LRATestUtilities.parseAsURI(lraIdString);

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
        URI lraId = LRATestUtilities.parseAsURI(lraIdString);

        callParticipant(lraIdString, PROPAGATE_MANUAL, false);

        endLRA(lraIdString, true);

        LRAResult lraManualParticipantResult = getManualParticipantResult();

        LRATestUtilities.verifyLRAResult(lraId, lraManualParticipantResult, false);
    }

    /**
     * @tpTestDetails Scenario where the LRA enlists two participants with a chained call and cancels.
     * @tpPassCrit LRA is started and cancelled and the two participants compensate callbacks are invoked.
     * @tpSince EAP XP 5.0.0
     */
    @Test
    public void testLRAFailureChainPropagation() {
        String lraIdString = startLRA();
        URI lraId = LRATestUtilities.parseAsURI(lraIdString);

        callParticipant(lraIdString, PROPAGATE_CHAIN, false);

        endLRA(lraIdString, true);

        LRAResult lraChainParticipant1Result = getChainParticipant1Result();
        LRAResult lraChainParticipant2Result = getChainParticipant2Result();

        LRATestUtilities.verifyLRAResult(lraId, lraChainParticipant1Result, false);
        LRATestUtilities.verifyLRAResult(lraId, lraChainParticipant2Result, false);
    }

    /**
     * @tpTestDetails Scenario where the LRA enlists three participants - one directly and two with a chained call and cancels.
     * @tpPassCrit LRA is started and cancelled and all three participants compensate callbacks are invoked.
     * @tpSince EAP XP 5.0.0
     */
    @Test
    public void testLRAFailureManualAndChainPropagation() {
        String lraIdString = startLRA();
        URI lraId = LRATestUtilities.parseAsURI(lraIdString);

        callParticipant(lraIdString, PROPAGATE_MANUAL, false);
        callParticipant(lraIdString, PROPAGATE_CHAIN, false);

        endLRA(lraIdString, true);

        LRAResult lraManualParticipantResult = getManualParticipantResult();
        LRAResult lraChainParticipant1Result = getChainParticipant1Result();
        LRAResult lraChainParticipant2Result = getChainParticipant2Result();

        LRATestUtilities.verifyLRAResult(lraId, lraManualParticipantResult, false);
        LRATestUtilities.verifyLRAResult(lraId, lraChainParticipant1Result, false);
        LRATestUtilities.verifyLRAResult(lraId, lraChainParticipant2Result, false);
    }

    /**
     * @tpTestDetails Scenario where the LRA is cancelled by a directly invoked participant.
     * @tpPassCrit LRA is started and cancelled by the participant and the participant's compensate callback is called.
     * @tpSince EAP XP 5.0.0
     */
    @Test
    public void testLRAParticipantFailureManual() {
        String lraIdString = startLRA();
        URI lraId = LRATestUtilities.parseAsURI(lraIdString);

        // this call will fail the manual participant and whole LRA
        callParticipant(lraIdString, PROPAGATE_MANUAL, true);

        LRAResult lraManualParticipantResult = getManualParticipantResult();

        LRATestUtilities.verifyLRAResult(lraId, lraManualParticipantResult, false);
    }

    /**
     * @tpTestDetails Scenario where the LRA is cancelled by a second participant in chain call.
     * @tpPassCrit LRA is started and cancelled by the last participant in the chain and all compensate callbacks are called.
     * @tpSince EAP XP 5.0.0
     */
    @Test
    public void testLRAParticipantFailureChain() {
        String lraIdString = startLRA();
        URI lraId = LRATestUtilities.parseAsURI(lraIdString);

        // this call will fail the second participant in chain and whole LRA
        callParticipant(lraIdString, PROPAGATE_CHAIN, true);

        LRAResult lraChainParticipant1Result = getChainParticipant1Result();
        LRAResult lraChainParticipant2Result = getChainParticipant2Result();

        System.out.println("lraChainParticipant1Result = " + lraChainParticipant1Result);
        System.out.println("lraChainParticipant2Result = " + lraChainParticipant2Result);

        LRATestUtilities.verifyLRAResult(lraId, lraChainParticipant1Result, false);
        LRATestUtilities.verifyLRAResult(lraId, lraChainParticipant2Result, false);
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
        URI lraId = LRATestUtilities.parseAsURI(lraIdString);

        // Fail the LRA with manual participant
        callParticipant(lraIdString, PROPAGATE_MANUAL, true);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // New registrations shouldn't be allowed, Narayana responds with 404 if LRA is not found
        verifyNewRegistrationsNotAllowed(lraIdString);

        LRAResult lraManualParticipantResult = getManualParticipantResult();
        LRAResult lraChainParticipant1Result = getChainParticipant1Result();
        LRAResult lraChainParticipant2Result = getChainParticipant2Result();

        LRATestUtilities.verifyLRAResult(lraId, lraManualParticipantResult, false);
        assertNull(lraChainParticipant1Result.getLraId());
        assertNull(lraChainParticipant2Result.getLraId());
    }

    protected abstract String startLRA();

    protected abstract void endLRA(String lraIdString, boolean b);

    protected abstract void callParticipant(String lraIdString, String propagateManual, boolean b);

    protected abstract LRAResult getManualParticipantResult();

    protected abstract LRAResult getChainParticipant1Result();

    protected abstract LRAResult getChainParticipant2Result();

    protected abstract void verifyNewRegistrationsNotAllowed(String lraIdString);

    protected abstract void resetChainParticipant2Result();

    protected abstract void resetChainParticipant1Result();

    protected abstract void resetManualParticipantResult();
}
