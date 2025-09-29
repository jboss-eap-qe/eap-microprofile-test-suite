package org.jboss.eap.qe.microprofile.lra;

import java.net.URL;

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
import org.junit.runner.RunWith;

/**
 * Tests fault tolerance behavior with Long Running Actions (LRA).
 */
@RunWith(Arquillian.class)
@ServerSetup({ EnableLRASetupTask.class, FaultToleranceServerSetup.class })
public class FaultToleranceWithLRATest extends FaultToleranceWithLRATestsBase {

    @Deployment(testable = false)
    public static Archive<?> deployment() {
        return ShrinkWrap.create(WebArchive.class, DEPLOYMENT_NAME + ".war")
                .addClasses(RestApplication.class, LRAParticipant.class, LRAParticipantWithFaultTolerance1.class,
                        LRAParticipantWithFaultTolerance2.class, LRAResult.class)
                .addClasses(EnableLRASetupTask.class, MicroProfileServerSetupTask.class, ServerSetupTask.class)
                .addAsWebInfResource(ConfigurationUtil.BEANS_XML_FILE_LOCATION, "beans.xml");
    }

    @ArquillianResource()
    private URL baseApplicationUrl;

    @Override
    protected URL getLRAPropagationBaseUrl() {
        return baseApplicationUrl;
    }

    @Override
    protected String getParticipant1URL(String... paths) {
        return LRATestUtilities.getURL(baseApplicationUrl, paths);
    }

    private LRAResult getParticipantResult(String participantPath) {
        return LRATestUtilities.getParticipantResult(baseApplicationUrl, participantPath);
    }

    @Override
    protected LRAResult getParticipant1Result() {
        return getParticipantResult(LRAParticipantWithFaultTolerance1.PATH);
    }

    @Override
    protected LRAResult getParticipant2Result() {
        return getParticipantResult(LRAParticipantWithFaultTolerance2.PATH);
    }

    private void resetParticipantResult(String participantPath) {
        LRATestUtilities.resetParticipantResult(baseApplicationUrl, participantPath);
    }

    @Override
    protected void resetParticipant1Result() {
        resetParticipantResult(LRAParticipantWithFaultTolerance1.PATH);
    }

    @Override
    protected void resetParticipant2Result() {
        resetParticipantResult(LRAParticipantWithFaultTolerance2.PATH);
    }
}
