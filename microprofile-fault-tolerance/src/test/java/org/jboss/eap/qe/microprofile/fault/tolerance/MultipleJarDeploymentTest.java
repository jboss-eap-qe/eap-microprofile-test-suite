package org.jboss.eap.qe.microprofile.fault.tolerance;

import static org.hamcrest.CoreMatchers.containsString;

import jakarta.inject.Inject;

import org.eclipse.microprofile.faulttolerance.FallbackHandler;
import org.hamcrest.MatcherAssert;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.eap.qe.microprofile.fault.tolerance.deployments.v10.HelloFallback;
import org.jboss.eap.qe.microprofile.fault.tolerance.deployments.v10.HelloService;
import org.jboss.eap.qe.microprofile.fault.tolerance.deployments.v10.MyContext;
import org.jboss.eap.qe.microprofile.fault.tolerance.util.FaultToleranceServerSetup;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.deployment.ConfigurationUtil;
import org.jboss.eap.qe.microprofile.tooling.server.log.LogChecker;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests multiple deploy/undeploy of MP FT deployments (just JAR archives). This in in-container test which injects
 * MP FT service and invokes operations on it.
 * Note that this is test for multiple deployments which is currently unsupported feature in Wildfly/EAP.
 */
@RunWith(Arquillian.class)
@ServerSetup(FaultToleranceServerSetup.class) // Enables/Disables fault tolerance extension/subsystem for Arquillian in-container tests
public class MultipleJarDeploymentTest {

    @Inject
    private HelloService helloService;

    private static final String FIRST_DEPLOYMENT_JAR = "first-deployment-jar";
    private static final String SECOND_DEPLOYMENT_JAR = "second-deployment-jar";

    @Deployment(name = FIRST_DEPLOYMENT_JAR, order = 1, testable = false)
    public static Archive<?> createFirstJarDeployment() {
        String mpConfig = "Timeout/enabled=false";

        return ShrinkWrap.create(JavaArchive.class, FIRST_DEPLOYMENT_JAR + ".jar")
                .addClasses(HelloService.class, MyContext.class, HelloFallback.class, FallbackHandler.class)
                .addAsManifestResource(ConfigurationUtil.BEANS_XML_FILE_LOCATION, "beans.xml")
                .addAsManifestResource(new StringAsset(mpConfig), "microprofile-config.properties");
    }

    @Deployment(name = SECOND_DEPLOYMENT_JAR, order = 2)
    public static Archive<?> createSecondJarDeployment() {
        String mpConfig = "Timeout/enabled=true";

        return ShrinkWrap.create(JavaArchive.class, SECOND_DEPLOYMENT_JAR + ".jar")
                .addClasses(HelloService.class, MyContext.class, HelloFallback.class, FallbackHandler.class)
                .addClasses(MultipleJarDeploymentTest.class, LogChecker.class)
                .addAsManifestResource(ConfigurationUtil.BEANS_XML_FILE_LOCATION, "beans.xml")
                .addAsManifestResource(new StringAsset(mpConfig), "microprofile-config.properties");
    }

    /**
     * @tpTestDetails Deploy first and then second MP FT application. Verify they're working.
     * @tpPassCrit Verify that second deployment uses its own MP FT configuration (with enabled @Timeout)
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @OperateOnDeployment(SECOND_DEPLOYMENT_JAR) // enrich this deployment by test
    public void testFaultToleranceTimeoutOnSecondDeployment() throws Exception {
        MatcherAssert.assertThat("Microprofile Fault Tolerance does not work on 2nd deployment.",
                helloService.timeout(true), containsString("Fallback Hello, context = foobar"));
    }
}
