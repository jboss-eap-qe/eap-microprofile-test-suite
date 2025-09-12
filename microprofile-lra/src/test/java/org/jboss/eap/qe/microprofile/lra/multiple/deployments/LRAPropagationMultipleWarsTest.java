package org.jboss.eap.qe.microprofile.lra.multiple.deployments;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import java.net.URL;

import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.eap.qe.microprofile.lra.EnableLRASetupTask;
import org.jboss.eap.qe.microprofile.lra.LRAChainPropagationParticipant1;
import org.jboss.eap.qe.microprofile.lra.LRAChainPropagationParticipant2;
import org.jboss.eap.qe.microprofile.lra.LRAController;
import org.jboss.eap.qe.microprofile.lra.LRAManualPropagationParticipant;
import org.jboss.eap.qe.microprofile.lra.LRAParticipant;
import org.jboss.eap.qe.microprofile.lra.LRAPropagationTestsBase;
import org.jboss.eap.qe.microprofile.lra.LRATestUtilities;
import org.jboss.eap.qe.microprofile.lra.RestApplication;
import org.jboss.eap.qe.microprofile.lra.model.LRAResult;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.MicroProfileServerSetupTask;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.deployment.ConfigurationUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;

/**
 * Verifies LRA propagation with manual and chained participants, using multiple deployments, i.e.:
 * 1 coordinator - N participants
 */
@RunWith(Arquillian.class)
@ServerSetup({ EnableLRASetupTask.class })
public class LRAPropagationMultipleWarsTest extends LRAPropagationTestsBase {

    public static final String DEPLOYMENT_NAME = "microprofile-lra";
    public static final String DEPLOYMENT_COORDINATOR_NAME = DEPLOYMENT_NAME + "-controller";
    public static final String DEPLOYMENT_PARTICIPANT_MANUAL_NAME = DEPLOYMENT_NAME + "-mp";
    public static final String DEPLOYMENT_PARTICIPANT_1_NAME = DEPLOYMENT_NAME + "-p1";
    public static final String DEPLOYMENT_PARTICIPANT_2_NAME = DEPLOYMENT_NAME + "-p2";

    @Deployment(testable = false, name = DEPLOYMENT_COORDINATOR_NAME)
    public static Archive<?> coordinatorDeployment() {
        return ShrinkWrap.create(WebArchive.class, DEPLOYMENT_COORDINATOR_NAME + ".war")
                .addClasses(LRAController.class, RestApplication.class)
                .addClasses(EnableLRASetupTask.class, MicroProfileServerSetupTask.class, ServerSetupTask.class)
                .addAsWebInfResource(ConfigurationUtil.BEANS_XML_FILE_LOCATION, "beans.xml");
    }

    @Deployment(testable = false, name = DEPLOYMENT_PARTICIPANT_MANUAL_NAME)
    public static Archive<?> namualParticipantDeployment() {
        return ShrinkWrap.create(WebArchive.class, DEPLOYMENT_PARTICIPANT_MANUAL_NAME + ".war")
                .addClasses(LRAResult.class, LRAParticipant.class, LRAManualPropagationParticipant.class,
                        RestApplication.class)
                .addClasses(EnableLRASetupTask.class, MicroProfileServerSetupTask.class, ServerSetupTask.class)
                .addAsWebInfResource(ConfigurationUtil.BEANS_XML_FILE_LOCATION, "beans.xml");
    }

    @Deployment(testable = false, name = DEPLOYMENT_PARTICIPANT_1_NAME)
    public static Archive<?> participant1Deployment() {
        return ShrinkWrap.create(WebArchive.class, DEPLOYMENT_PARTICIPANT_1_NAME + ".war")
                .addClasses(LRAResult.class, LRAParticipant.class,
                        LRAChainPropagationParticipant1.class,
                        RestApplication.class)
                .addClasses(EnableLRASetupTask.class, MicroProfileServerSetupTask.class, ServerSetupTask.class)
                .addAsWebInfResource(ConfigurationUtil.BEANS_XML_FILE_LOCATION, "beans.xml");
    }

    @Deployment(testable = false, name = DEPLOYMENT_PARTICIPANT_2_NAME)
    public static Archive<?> participant2Deployment() {
        return ShrinkWrap.create(WebArchive.class, DEPLOYMENT_PARTICIPANT_2_NAME + ".war")
                .addClasses(LRAResult.class, LRAParticipant.class,
                        LRAChainPropagationParticipant2.class,
                        RestApplication.class)
                .addClasses(EnableLRASetupTask.class, MicroProfileServerSetupTask.class, ServerSetupTask.class)
                .addAsWebInfResource(ConfigurationUtil.BEANS_XML_FILE_LOCATION, "beans.xml");
    }

    @ArquillianResource
    @OperateOnDeployment(DEPLOYMENT_COORDINATOR_NAME)
    URL lraCoordinatorUrl;

    @ArquillianResource
    @OperateOnDeployment(DEPLOYMENT_PARTICIPANT_MANUAL_NAME)
    URL manualParticipantUrl;

    @ArquillianResource
    @OperateOnDeployment(DEPLOYMENT_PARTICIPANT_1_NAME)
    URL participant1Url;

    @ArquillianResource
    @OperateOnDeployment(DEPLOYMENT_PARTICIPANT_2_NAME)
    URL participant2Url;

    @Override
    protected String startLRA() {
        return given()
                .header(LRAController.MANUAL_PARTICIPANT_URI, manualParticipantUrl.toString())
                .header(LRAController.CHAIN_PARTICIPANT_1_URI, participant1Url.toString())
                .header(LRAController.CHAIN_PARTICIPANT_2_URI, participant2Url.toString())
                .when()
                .get(LRATestUtilities.getURL(lraCoordinatorUrl, LRAController.LRA_PATH))
                .then()
                .statusCode(200)
                .extract().asString();
    }

    @Override
    protected void endLRA(String lraIdString, boolean failLRA) {
        given()
                .queryParam(LRAController.FAIL_LRA, failLRA)
                .header(LRA.LRA_HTTP_CONTEXT_HEADER, lraIdString)
                .when().get(LRATestUtilities.getURL(lraCoordinatorUrl, LRAController.LRA_PATH, LRAController.END_LRA_PATH))
                .then()
                .statusCode(failLRA ? 400 : 200)
                .body(is(lraIdString));
    }

    @Override
    protected void callParticipant(String lraIdString, String path, boolean fail) {
        LRATestUtilities.callParticipant(lraCoordinatorUrl, lraIdString, path, fail);
    }

    @Override
    protected LRAResult getManualParticipantResult() {
        return LRATestUtilities.getParticipantResult(manualParticipantUrl,
                LRAManualPropagationParticipant.PATH);
    }

    @Override
    protected LRAResult getChainParticipant1Result() {
        return LRATestUtilities.getParticipantResult(participant1Url,
                LRAChainPropagationParticipant1.PATH);
    }

    @Override
    protected LRAResult getChainParticipant2Result() {
        return LRATestUtilities.getParticipantResult(participant2Url,
                LRAChainPropagationParticipant2.PATH);
    }

    @Override
    protected void resetManualParticipantResult() {
        LRATestUtilities.resetParticipantResult(manualParticipantUrl, LRAManualPropagationParticipant.PATH);
    }

    @Override
    protected void resetChainParticipant1Result() {
        LRATestUtilities.resetParticipantResult(participant1Url, LRAChainPropagationParticipant1.PATH);
    }

    @Override
    protected void resetChainParticipant2Result() {
        LRATestUtilities.resetParticipantResult(participant2Url, LRAChainPropagationParticipant2.PATH);
    }

    @Override
    protected void verifyNewRegistrationsNotAllowed(String lraIdString) {
        LRATestUtilities.verifyNewRegistrationsNotAllowed(lraCoordinatorUrl, lraIdString);
    }
}
