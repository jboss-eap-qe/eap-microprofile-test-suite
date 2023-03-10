package org.jboss.eap.qe.microprofile.fault.tolerance;

import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.containsString;

import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.eap.qe.microprofile.fault.tolerance.deployments.v10.HelloService;
import org.jboss.eap.qe.microprofile.fault.tolerance.util.MicroProfileFaultToleranceServerConfiguration;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.ConfigurationException;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.ArquillianContainerProperties;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.ArquillianDescriptorWrapper;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.deployment.ConfigurationUtil;
import org.jboss.eap.qe.microprofile.tooling.server.log.LogChecker;
import org.jboss.eap.qe.microprofile.tooling.server.log.ModelNodeLogChecker;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;

/**
 * Tests deploy/undeploy of MP FT deployments (WAR archives)
 * Note that this is test for multiple deployments which is currently unsupported feature in Wildfly/EAP.
 */
@RunAsClient
@RunWith(Arquillian.class)
public class UndeployDeployTest {

    private static final String FIRST_DEPLOYMENT = "UndeployDeployTest-first-deployment";
    private static final String SECOND_DEPLOYMENT = "UndeployDeployTest-second-deployment";
    private static final String NO_MP_FT_DEPLOYMENT = "no-mp-ft-deployment";

    @ArquillianResource
    private Deployer deployer;

    @Deployment(name = FIRST_DEPLOYMENT, managed = false)
    public static Archive<?> createFirstDeployment() {
        String mpConfig = "Timeout/enabled=true";

        return ShrinkWrap.create(WebArchive.class, FIRST_DEPLOYMENT + ".war")
                .addPackages(true, HelloService.class.getPackage())
                .addAsManifestResource(ConfigurationUtil.BEANS_XML_FILE_LOCATION, "beans.xml")
                .addAsManifestResource(new StringAsset(mpConfig), "microprofile-config.properties");
    }

    @Deployment(name = SECOND_DEPLOYMENT, managed = false)
    public static Archive<?> createSecondDeployment() {
        String mpConfig = "Timeout/enabled=false";

        return ShrinkWrap.create(WebArchive.class, SECOND_DEPLOYMENT + ".war")
                .addPackages(true, HelloService.class.getPackage())
                .addAsManifestResource(ConfigurationUtil.BEANS_XML_FILE_LOCATION, "beans.xml")
                .addAsManifestResource(new StringAsset(mpConfig), "microprofile-config.properties");
    }

    @Deployment(name = NO_MP_FT_DEPLOYMENT, managed = false, testable = false)
    public static Archive<?> createNonMPFTDeployment() {
        return ShrinkWrap.create(WebArchive.class, NO_MP_FT_DEPLOYMENT + ".war")
                .addPackage(org.jboss.eap.qe.microprofile.fault.tolerance.deployments.nofaulttolerance.HelloService.class
                        .getPackage())
                .addAsManifestResource(ConfigurationUtil.BEANS_XML_FILE_LOCATION, "beans.xml");
    }

    @BeforeClass
    public static void setup() throws Exception {
        MicroProfileFaultToleranceServerConfiguration.enableFaultTolerance();
    }

    /**
     * Deploy all deployments so it's possible to get their URLs in other tests. This is limitation of Arquillian
     * when deployments are deployed manually (in tests) and thus their URL is not known in time when test is started.
     * The only way how to workaround this issue is to deploy all deployments in first test which allows to inject
     * deployment URL as params in other test methods.
     */
    @Test
    @InSequence(1)
    public void deployAll() {
        deployer.deploy(FIRST_DEPLOYMENT);
        deployer.deploy(SECOND_DEPLOYMENT);
        deployer.deploy(NO_MP_FT_DEPLOYMENT);
    }

    /**
     * @tpTestDetails Enable MP FT in server configuration and deploy application which does not use MP FT.
     * @tpPassCrit MP FT was not activated.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @InSequence(20)
    public void testFaultToleranceSubsystemNotActivatedByNonMPFTDeployment(
            @ArquillianResource @OperateOnDeployment(NO_MP_FT_DEPLOYMENT) URL noMpFtDeploymentUlr) throws Exception {
        deployer.deploy(NO_MP_FT_DEPLOYMENT);
        get(noMpFtDeploymentUlr + "?operation=ping")
                .then()
                .assertThat()
                .body(containsString("Pong from HelloService"));

        try (final OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
            final LogChecker logChecker = new ModelNodeLogChecker(client, 10, true);
            Assert.assertFalse(logChecker.logContains("MicroProfile: Fault Tolerance activated"));
        }
        deployer.undeploy(NO_MP_FT_DEPLOYMENT);
    }

    /**
     * @tpTestDetails Deploy first MP FT application which initializes MP FT then deploy second MP FT
     *                application which has different MP FT configuration.
     * @tpPassCrit MP FT configuration is different for both deployments.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @InSequence(20)
    public void testSecondDeploymentDoesChangeConfiguration(
            @ArquillianResource @OperateOnDeployment(FIRST_DEPLOYMENT) URL firstDeploymentUlr,
            @ArquillianResource @OperateOnDeployment(SECOND_DEPLOYMENT) URL secondDeploymentUlr) {
        deployer.deploy(FIRST_DEPLOYMENT);
        deployer.deploy(SECOND_DEPLOYMENT);

        get(firstDeploymentUlr + "?operation=timeout&context=foobar&fail=true").then()
                .assertThat()
                .body(containsString("Fallback Hello, context = foobar"));
        // timeout is not working because 2nd deployment has disabled it
        get(secondDeploymentUlr + "?operation=timeout&context=foobar&fail=true").then()
                .assertThat()
                .body(containsString("Hello from @Timeout method, context = foobar"));

        deployer.undeploy(FIRST_DEPLOYMENT);
        deployer.undeploy(SECOND_DEPLOYMENT);
    }

    /**
     * @tpTestDetails Deploy first MP FT application which initializes MP FT then deploy second MP FT
     *                application which changes has different MP FT configuration. Undeploy second application.
     * @tpPassCrit MP FT configuration was NOT changed.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @InSequence(20)
    public void testUndeploySecondDeploymentDoesNotChangeMpFtConfiguration(
            @ArquillianResource @OperateOnDeployment(FIRST_DEPLOYMENT) URL firstDeploymentUlr,
            @ArquillianResource @OperateOnDeployment(SECOND_DEPLOYMENT) URL secondDeploymentUlr) {
        deployer.deploy(FIRST_DEPLOYMENT);
        deployer.deploy(SECOND_DEPLOYMENT);

        get(firstDeploymentUlr + "?operation=timeout&context=foobar&fail=true").then()
                .assertThat()
                .body(containsString("Fallback Hello, context = foobar"));

        deployer.undeploy(SECOND_DEPLOYMENT);

        get(firstDeploymentUlr + "?operation=timeout&context=foobar&fail=true").then()
                .assertThat()
                .body(containsString("Fallback Hello, context = foobar"));
        deployer.undeploy(SECOND_DEPLOYMENT);
    }

    /**
     * @tpTestDetails Deploy the first and then second MP FT application.
     *                Both of them are the same (same classes/methods).
     * @tpPassCrit Verify that the number of total timed out calls is 1, i.e. the one resulting from the request sent
     *             to the first deployment, where Timeout is enabled. The method also verifies that the total number of
     *             non-applied
     *             fallback calls is set to 1, i.e. the one originated from the request sent to the second deployment, where
     *             Timeout has been disabled.
     *
     *             <p>
     *             Since MP FT 3.0 FT Metrics have been moved to the base scope and hence have different semantic, e.g.:
     *             {@code application_ft_org_jboss_eap_qe_microprofile_fault_tolerance_deployments_v10_HelloService_timeout_invocations_total}
     *             which was counting the total number of invocations to method annotated with {@code Timeout} doesn't exist any
     *             more
     *             </p>
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @InSequence(10)
    public void testFaultToleranceMetricsAreTracedWithSameDeployments(
            @ArquillianResource @OperateOnDeployment(FIRST_DEPLOYMENT) URL firstDeploymentUlr,
            @ArquillianResource @OperateOnDeployment(SECOND_DEPLOYMENT) URL secondDeploymentUlr) throws ConfigurationException {
        deployer.deploy(FIRST_DEPLOYMENT);
        deployer.deploy(SECOND_DEPLOYMENT);

        get(firstDeploymentUlr + "?operation=timeout&context=foobar&fail=true").then()
                .assertThat()
                .body(containsString("Fallback Hello, context = foobar"));
        // timeout is not working because 2nd deployment has disabled it
        get(secondDeploymentUlr + "?operation=timeout&context=foobar&fail=true").then()
                .assertThat()
                .body(containsString("Hello from @Timeout method, context = foobar"));

        ArquillianContainerProperties arqProperties = new ArquillianContainerProperties(
                ArquillianDescriptorWrapper.getArquillianDescriptor());
        get("http://" + arqProperties.getDefaultManagementAddress() + ":" + arqProperties.getDefaultManagementPort()
                + "/metrics").then()
                .assertThat()
                .body(containsString(
                        "base_ft_timeout_calls_total{method=\"org.jboss.eap.qe.microprofile.fault.tolerance.deployments.v10.HelloService.timeout\",timedOut=\"true\"} 1.0"))
                .and()
                .body(containsString(
                        "base_ft_invocations_total{fallback=\"applied\",method=\"org.jboss.eap.qe.microprofile.fault.tolerance.deployments.v10.HelloService.timeout\",result=\"valueReturned\"} 1.0"))
                .and()
                .body(containsString(
                        "base_ft_invocations_total{fallback=\"notApplied\",method=\"org.jboss.eap.qe.microprofile.fault.tolerance.deployments.v10.HelloService.timeout\",result=\"valueReturned\"} 1.0"));
        ;

        deployer.undeploy(FIRST_DEPLOYMENT);
        deployer.undeploy(SECOND_DEPLOYMENT);
    }

    /**
     * @tpTestDetails Deploy MP FT application which initializes MP FT, undeploy and deploy different MP FT
     *                application which changes MP FT configuration.
     * @tpPassCrit MP FT configuration was changed.
     */
    @Test
    @InSequence(20)
    public void testUndeployDeployChangesFaultToleranceConfiguration(
            @ArquillianResource @OperateOnDeployment(FIRST_DEPLOYMENT) URL firstDeploymentUlr,
            @ArquillianResource @OperateOnDeployment(SECOND_DEPLOYMENT) URL secondDeploymentUlr) {
        deployer.deploy(FIRST_DEPLOYMENT);
        get(firstDeploymentUlr + "?operation=timeout&context=foobar&fail=true").then()
                .assertThat()
                .body(containsString("Fallback Hello, context = foobar"));
        deployer.undeploy(FIRST_DEPLOYMENT);

        // make sure it's undeployed
        get(firstDeploymentUlr + "?operation=timeout&context=foobar&fail=true").then()
                .assertThat()
                .statusCode(404);

        deployer.deploy(SECOND_DEPLOYMENT);
        get(secondDeploymentUlr + "?operation=timeout&context=foobar&fail=true").then()
                .assertThat()
                .body(containsString("Hello from @Timeout method, context = foobar"));
        deployer.undeploy(SECOND_DEPLOYMENT);
    }

    /**
     * The purpose of this @After method is to undeploy all deployments if they are present (there is no error
     * if such deployment does not exist = was not deployed) and clean environment for next test, no matter what happens.
     */
    @After
    public void undeploy() {
        deployer.undeploy(FIRST_DEPLOYMENT);
        deployer.undeploy(SECOND_DEPLOYMENT);
        deployer.undeploy(NO_MP_FT_DEPLOYMENT);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        MicroProfileFaultToleranceServerConfiguration.disableFaultTolerance();
    }
}
