package org.jboss.eap.qe.microprofile.lra;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import java.net.URL;

import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.eap.qe.microprofile.lra.model.LRAResult;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.MicroProfileServerSetupTask;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.deployment.ConfigurationUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;

/**
 * Verifies LRA propagation with manual and chained participants, using a single deployment
 */
@RunWith(Arquillian.class)
@ServerSetup({ EnableLRASetupTask.class })
public class LRAPropagationTest extends LRAPropagationTestsBase {

    @ArquillianResource
    private URL baseApplicationUrl;

    @Deployment(testable = false)
    public static Archive<?> deployment() {
        return ShrinkWrap.create(WebArchive.class, LRAPropagationTest.class.getSimpleName() + ".war")
                .addClasses(RestApplication.class,
                        LRAController.class,
                        LRAParticipant.class,
                        LRAManualPropagationParticipant.class,
                        LRAChainPropagationParticipant1.class,
                        LRAChainPropagationParticipant2.class,
                        LRAResult.class)
                .addClasses(EnableLRASetupTask.class, MicroProfileServerSetupTask.class, ServerSetupTask.class)
                .addAsWebInfResource(ConfigurationUtil.BEANS_XML_FILE_LOCATION, "beans.xml");
    }

    private String getURL(String... paths) {
        return LRATestUtilities.getURL(baseApplicationUrl, paths);
    }

    @Override
    protected String startLRA() {
        return given()
                .header(LRAController.MANUAL_PARTICIPANT_URI, baseApplicationUrl.toString())
                .header(LRAController.CHAIN_PARTICIPANT_1_URI, baseApplicationUrl.toString())
                .header(LRAController.CHAIN_PARTICIPANT_2_URI, baseApplicationUrl.toString())
                .when()
                .get(getURL(LRAController.LRA_PATH))
                .then()
                .statusCode(200)
                .extract().asString();
    }

    @Override
    protected void endLRA(String lraIdString, boolean failLRA) {
        given()
                .queryParam(LRAController.FAIL_LRA, failLRA)
                .header(LRA.LRA_HTTP_CONTEXT_HEADER, lraIdString)
                .when().get(getURL(LRAController.LRA_PATH, LRAController.END_LRA_PATH))
                .then()
                .statusCode(failLRA ? 400 : 200)
                .body(is(lraIdString));
    }

    @Override
    protected void callParticipant(String lraIdString, String path, boolean fail) {
        LRATestUtilities.callParticipant(baseApplicationUrl, lraIdString, path, fail);
    }

    private LRAResult getParticipantResult(String participantPath) {
        return LRATestUtilities.getParticipantResult(baseApplicationUrl, participantPath);
    }

    @Override
    protected LRAResult getManualParticipantResult() {
        return getParticipantResult(LRAManualPropagationParticipant.PATH);
    }

    @Override
    protected LRAResult getChainParticipant1Result() {
        return getParticipantResult(LRAChainPropagationParticipant1.PATH);
    }

    @Override
    protected LRAResult getChainParticipant2Result() {
        return getParticipantResult(LRAChainPropagationParticipant2.PATH);
    }

    private void resetParticipantResult(String participantPath) {
        LRATestUtilities.resetParticipantResult(baseApplicationUrl, participantPath);
    }

    @Override
    protected void resetManualParticipantResult() {
        resetParticipantResult(LRAManualPropagationParticipant.PATH);
    }

    @Override
    protected void resetChainParticipant1Result() {
        resetParticipantResult(LRAChainPropagationParticipant1.PATH);
    }

    @Override
    protected void resetChainParticipant2Result() {
        resetParticipantResult(LRAChainPropagationParticipant2.PATH);
    }

    @Override
    protected void verifyNewRegistrationsNotAllowed(String lraIdString) {
        LRATestUtilities.verifyNewRegistrationsNotAllowed(baseApplicationUrl, lraIdString);
    }
}
