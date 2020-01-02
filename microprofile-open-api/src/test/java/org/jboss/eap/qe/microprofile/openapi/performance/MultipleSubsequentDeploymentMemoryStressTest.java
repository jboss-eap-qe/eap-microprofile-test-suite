package org.jboss.eap.qe.microprofile.openapi.performance;

import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;

import java.io.IOException;
import java.util.concurrent.Callable;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.ProviderApplication;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.RoutingServiceConstants;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.api.DistrictService;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.data.DistrictEntity;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.model.District;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.rest.DistrictsResource;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.services.InMemoryDistrictService;
import org.jboss.eap.qe.microprofile.openapi.performance.evaluation.IncreaseOverToleranceEvaluator;
import org.jboss.eap.qe.microprofile.tooling.performance.core.*;
import org.jboss.eap.qe.microprofile.tooling.performance.memory.JMXBasedMemoryGauge;
import org.jboss.eap.qe.microprofile.tooling.performance.memory.MemoryUsageRecord;
import org.jboss.eap.qe.microprofile.tooling.performance.protocol.MultipleRepeatableActionsProtocol;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.ConfigurationException;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.ArquillianContainerProperties;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.ArquillianDescriptorWrapper;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;

/**
 * Memory stress test cases for multiple subsequent re-deployments
 */
@RunWith(Arquillian.class)
@RunAsClient
public class MultipleSubsequentDeploymentMemoryStressTest {

    private final static String PROVIDER_DEPLOYMENT_NAME = "serviceProviderDeployment";
    private final static int POST_DEPLOYMENT_GRACEFUL_WAIT_TIME_IN_MSEC = 1000;
    private final static int OPENAPI_INCREASED_MEMORY_FOOTPRINT_TOLERANCE_PERCENT = 7;
    private final static int SIMPLE_REDEPLOY_ITERATIONS = 100;
    private final static int PROBING_INTERVAL_SIMPLE_REDEPLOY_ITERATIONS = SIMPLE_REDEPLOY_ITERATIONS;
    private final static int LINEAR_FIT_SLOPE_COMPARISON_REDEPLOY_ITERATIONS = 64;
    private final static int PROBING_INTERVAL_LINEAR_FIT_SLOPE_COMPARISON_REDEPLOY_ITERATIONS = 8;
    private final static long MEGABYTE = 1024 * 1024;
    private final static long MEMORY_INCREASE_TOLERANCE_IN_MB = 25 * MEGABYTE;

    private static ArquillianContainerProperties arquillianContainerProperties = new ArquillianContainerProperties(
            ArquillianDescriptorWrapper.getArquillianDescriptor());
    private static OnlineManagementClient onlineManagementClient;
    private static String openapiUrl;

    private static Gauge<MemoryUsageRecord> gauge;

    @ArquillianResource
    private Deployer deployer;

    @ArquillianResource
    private ContainerController contoller;

    @BeforeClass
    public static void setup()
            throws ConfigurationException, IOException {

        onlineManagementClient = ManagementClientProvider.onlineStandalone();

        openapiUrl = String.format("http://%s:%d/openapi",
                arquillianContainerProperties.getDefaultManagementAddress(),
                8080);

        gauge = new JMXBasedMemoryGauge(arquillianContainerProperties.getDefaultManagementAddress(),
                arquillianContainerProperties.getDefaultManagementPort());
    }

    @AfterClass
    public static void tearDown() throws IOException {
        onlineManagementClient.close();
    }

    @Deployment(name = PROVIDER_DEPLOYMENT_NAME, managed = false, testable = false)
    public static Archive<?> serviceProviderDeployment() {
        WebArchive deployment = ShrinkWrap.create(
                WebArchive.class, PROVIDER_DEPLOYMENT_NAME + ".war")
                .addClasses(ProviderApplication.class)
                .addClasses(
                        District.class,
                        DistrictEntity.class,
                        DistrictService.class,
                        InMemoryDistrictService.class,
                        DistrictsResource.class,
                        RoutingServiceConstants.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return deployment;
    }

    private Void executeSimpleRedeployActions() throws InterruptedException {

        deployer.deploy(PROVIDER_DEPLOYMENT_NAME);
        Thread.sleep(POST_DEPLOYMENT_GRACEFUL_WAIT_TIME_IN_MSEC);
        deployer.undeploy(PROVIDER_DEPLOYMENT_NAME);

        return null;
    }

    private Void executeRedeploymentControlActions() throws InterruptedException {

        deployer.deploy(PROVIDER_DEPLOYMENT_NAME);
        Thread.sleep(POST_DEPLOYMENT_GRACEFUL_WAIT_TIME_IN_MSEC);

        get(openapiUrl)
                .then()
                .statusCode(404);

        deployer.undeploy(PROVIDER_DEPLOYMENT_NAME);

        return null;
    }

    private Void executeRedeploymentTestActions() throws InterruptedException {

        deployer.deploy(PROVIDER_DEPLOYMENT_NAME);
        Thread.sleep(POST_DEPLOYMENT_GRACEFUL_WAIT_TIME_IN_MSEC);

        get(openapiUrl)
                .then()
                .statusCode(200)
                .body(not(empty()));

        deployer.undeploy(PROVIDER_DEPLOYMENT_NAME);

        return null;
    }

    /**
     * @tpTestDetails Test to verify that a number of subsequent deployments doesn't cause memory leaks.
     *                This stress test executes the multiple subsequent re-deployments protocol and measures memory
     *                footprint at the beginning and at the end.
     * @tpPassCrit Final value does not exceed initial value by more than
     *             {@link MultipleSubsequentDeploymentMemoryStressTest#MEMORY_INCREASE_TOLERANCE_IN_MB}
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @InSequence(1)
    public void testSimpleSeveralRedeployProtocol() throws StressTestException {

        //  we have one tester which is going to take care of this stress test, it registers measurements
        //  as instances of MemoryUsageRecord and uses a gauge that accepts this data type
        StressTester<MemoryUsageRecord, Gauge<MemoryUsageRecord>> tester = new StressTester(gauge);

        //  this evaluator is intended to assess whether final value is showing an increase bigger than the
        //  accepted tolerance when compared to initial value
        IncreaseOverToleranceEvaluator evaluator = new IncreaseOverToleranceEvaluator(MEMORY_INCREASE_TOLERANCE_IN_MB);

        //  let's start with the test session: it defines a MultipleSubsequentDeploymentsProtocol to execute
        //  SIMPLE_REDEPLOY_ITERATIONS redeploy actions and will probe for memory footprint each
        //  PROBING_INTERVAL_SIMPLE_REDEPLOY_ITERATIONS attempts
        StressTestProtocol simpleRedeployProtocol = new MultipleRepeatableActionsProtocol(
                SIMPLE_REDEPLOY_ITERATIONS,
                PROBING_INTERVAL_SIMPLE_REDEPLOY_ITERATIONS,
                new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        return executeSimpleRedeployActions();
                    }
                });
        //  start the container
        contoller.start("jboss");

        //  initial value
        try {
            tester.probe();
        } catch (MeasurementException e) {
            throw new StressTestException(e);
        }

        //  let the tester execute its test session following the protocol
        tester.executeSession(simpleRedeployProtocol);
        //  stop the container
        contoller.stop("jboss");

        //   report control values to the evaluator
        evaluator.setInitialValue(tester.getCollectedValues().get(0).getHeapSpaceUsed());
        evaluator.setFinalValue(tester.getCollectedValues().get(tester.getCollectedValues().size() - 1).getHeapSpaceUsed());

        //  let's evaluate results for this initial stress test session using IncreaseOverToleranceEvaluator
        IncreaseOverToleranceEvaluator.Outcome outcome = evaluator.evaluate();
        Long initialValue = outcome.getInitialValue(), finalValue = outcome.getFinalValue();
        Assert.assertTrue(
                String.format(
                        "Memory consumption increase exceeds tolerance: (%s - %s) = %s > %s",
                        initialValue, finalValue, finalValue - initialValue, MEMORY_INCREASE_TOLERANCE_IN_MB,
                        OPENAPI_INCREASED_MEMORY_FOOTPRINT_TOLERANCE_PERCENT),
                outcome.isPassed());
    }
}
