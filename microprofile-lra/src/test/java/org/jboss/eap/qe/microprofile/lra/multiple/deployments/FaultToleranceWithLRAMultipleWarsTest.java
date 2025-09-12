package org.jboss.eap.qe.microprofile.lra.multiple.deployments;

import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.eap.qe.microprofile.common.setuptasks.FaultToleranceServerSetup;
import org.jboss.eap.qe.microprofile.lra.EnableLRASetupTask;
import org.jboss.eap.qe.microprofile.lra.FaultToleranceWithLRATestsBase;
import org.jboss.eap.qe.microprofile.lra.LRAParticipant;
import org.jboss.eap.qe.microprofile.lra.LRAParticipantWithFaultTolerance1;
import org.jboss.eap.qe.microprofile.lra.LRAParticipantWithFaultTolerance2;
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
 * Tests fault tolerance behavior with Long Running Actions (LRA), having coordinator and participants in dedicated
 * deployments (1 coordinator, N participants).
 */
@RunWith(Arquillian.class)
@ServerSetup({ EnableLRASetupTask.class, FaultToleranceServerSetup.class })
public class FaultToleranceWithLRAMultipleWarsTest extends FaultToleranceWithLRATestsBase {

    public static final String DEPLOYMENT_CONTROLLER_NAME = DEPLOYMENT_NAME + "-controller";
    public static final String DEPLOYMENT_PARTICIPANT_2_NAME = DEPLOYMENT_NAME + "-p2";

    @Deployment(testable = false, name = DEPLOYMENT_CONTROLLER_NAME)
    public static Archive<?> participant1Deployment() {
        return ShrinkWrap.create(WebArchive.class, DEPLOYMENT_CONTROLLER_NAME + ".war")
                .addClasses(RestApplication.class, LRAParticipant.class, LRAParticipantWithFaultTolerance1.class,
                        LRAResult.class)
                .addClasses(EnableLRASetupTask.class, MicroProfileServerSetupTask.class, ServerSetupTask.class)
                .addAsWebInfResource(ConfigurationUtil.BEANS_XML_FILE_LOCATION, "beans.xml");
    }

    @Deployment(testable = false, name = DEPLOYMENT_PARTICIPANT_2_NAME)
    public static Archive<?> participant2Deployment() {
        return ShrinkWrap.create(WebArchive.class, DEPLOYMENT_PARTICIPANT_2_NAME + ".war")
                .addClasses(RestApplication.class, LRAParticipant.class, LRAParticipantWithFaultTolerance2.class,
                        LRAResult.class)
                .addClasses(EnableLRASetupTask.class, MicroProfileServerSetupTask.class, ServerSetupTask.class)
                .addAsWebInfResource(ConfigurationUtil.BEANS_XML_FILE_LOCATION, "beans.xml");
    }

    @ArquillianResource
    @OperateOnDeployment(DEPLOYMENT_CONTROLLER_NAME)
    URL participant1BaseUrl;

    @ArquillianResource
    @OperateOnDeployment(DEPLOYMENT_PARTICIPANT_2_NAME)
    URL participant2BaseUrl;

    @Override
    protected URL getLRAPropagationBaseUrl() {
        return participant2BaseUrl;
    }

    @Override
    protected String getParticipant1URL(String... paths) {
        return LRATestUtilities.getURL(participant1BaseUrl, paths);
    }

    @Override
    protected LRAResult getParticipant1Result() {
        return LRATestUtilities.getParticipantResult(participant1BaseUrl,
                LRAParticipantWithFaultTolerance1.PATH);
    }

    @Override
    protected LRAResult getParticipant2Result() {
        return LRATestUtilities.getParticipantResult(participant2BaseUrl,
                LRAParticipantWithFaultTolerance2.PATH);
    }

    @Override
    protected void resetParticipant1Result() {
        LRATestUtilities.resetParticipantResult(participant1BaseUrl, LRAParticipantWithFaultTolerance1.PATH);
    }

    @Override
    protected void resetParticipant2Result() {
        LRATestUtilities.resetParticipantResult(participant2BaseUrl, LRAParticipantWithFaultTolerance2.PATH);
    }
}
