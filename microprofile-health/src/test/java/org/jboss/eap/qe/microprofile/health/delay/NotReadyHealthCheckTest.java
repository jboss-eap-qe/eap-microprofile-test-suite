package org.jboss.eap.qe.microprofile.health.delay;

import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.eap.qe.microprofile.health.ManualTests;
import org.jboss.eap.qe.microprofile.health.tools.HealthUrlProvider;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.ConfigurationException;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.ArquillianContainerProperties;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.ArquillianDescriptorWrapper;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.core.online.CliException;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;

/**
 * Performs tests that aim to verify the MP Health check implementation in complex scenarios where delayed
 * readiness health checks could lead to misinterpretation of server/service status, see
 * https://issues.redhat.com/browse/WFLY-12952.
 * <p>
 * Please notice that this tests need -Dmp.health.empty.readiness.checks.status=DOWN in order to pass, see
 * https://doc-stage.usersys.redhat.com/documentation/en-us/red_hat_jboss_enterprise_application_platform/7.3/html-single/configuration_guide/index#global_statuses_undefined_probes
 */
@Category(ManualTests.class)
@RunWith(Arquillian.class)
public class NotReadyHealthCheckTest {

    @ArquillianResource
    ContainerController controller;

    private ExecutorService executorService;

    private static ArquillianContainerProperties arqProps;

    /**
     * Here the server is started and then stopped just to set the value for empty-readiness-checks-status
     * attribute
     *
     * @throws ConfigurationException
     * @throws IOException
     * @throws CliException
     * @throws TimeoutException
     * @throws InterruptedException
     */
    private void setInitialHealthCheckStatus()
            throws ConfigurationException, IOException, CliException, TimeoutException, InterruptedException {
        controller.start(ManualTests.ARQUILLIAN_CONTAINER);
        try {
            try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone(arqProps)) {
                client.execute(
                        "/subsystem=microprofile-health-smallrye:write-attribute(name=empty-readiness-checks-status,value=DOWN)")
                        .assertSuccess();
                new Administration(client).reload();
            }
        } finally {
            controller.stop(ManualTests.ARQUILLIAN_CONTAINER);
        }
    }

    /**
     * Here the server is started and then stopped just to reset the value for empty-readiness-checks-status
     * attribute
     *
     * @throws ConfigurationException
     * @throws IOException
     * @throws CliException
     * @throws TimeoutException
     * @throws InterruptedException
     */
    private void resetInitialHealthCheckStatus()
            throws ConfigurationException, IOException, CliException, TimeoutException, InterruptedException {
        controller.start(ManualTests.ARQUILLIAN_CONTAINER);
        try {

            try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone(arqProps)) {
                client.execute(
                        "/subsystem=microprofile-health-smallrye:write-attribute(name=empty-readiness-checks-status,value=${env.MP_HEALTH_EMPTY_READINESS_CHECKS_STATUS:UP})")
                        .assertSuccess();
                new Administration(client).reload();
            }
        } finally {
            controller.stop(ManualTests.ARQUILLIAN_CONTAINER);
        }
    }

    /**
     * Builds a deployment that contains just a readiness health check which has a constructor that simulates
     * a delay in returning the accurate readiness status (e.g.: like one testing a database connection availability
     * could do)
     *
     * @return A {@link File} instance that refers to the ZIP archive that will be copied to Wildfly {@code deployments}
     *         directory
     */
    private static File delayedDownReadinessCheckDeployment() {
        File deployment;
        try {
            Path tempDirectory = Files.createTempDirectory(null);
            deployment = new File(tempDirectory.toFile(), NotReadyHealthCheckTest.class.getSimpleName() + ".war");
        } catch (IOException e) {
            e.printStackTrace();
            deployment = new File(NotReadyHealthCheckTest.class.getSimpleName() + ".war");
        }

        ShrinkWrap.create(WebArchive.class, NotReadyHealthCheckTest.class.getSimpleName() + ".war")
                .addClasses(DelayedReadinessHealthCheck.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .as(ZipExporter.class)
                .exportTo(deployment, true);

        return deployment;
    }

    /**
     * Builds a deployment that contains just a liveness - thus <b>no readiness</b> - health check which has a
     * constructor that simulates a delay in returning the accurate liveness status (e.g.: like one acting as an AMQ
     * backup could do)
     *
     * @return A {@link File} instance that refers to the ZIP archive that will be copied to Wildfly {@code deployments}
     *         directory
     */
    private static File unexpectedReadinessChecksDeployment() {
        File deployment;
        try {
            Path tempDirectory = Files.createTempDirectory(null);
            deployment = new File(tempDirectory.toFile(), DelayedLivenessHealthCheck.class.getSimpleName() + ".war");
        } catch (IOException e) {
            e.printStackTrace();
            deployment = new File(DelayedLivenessHealthCheck.class.getSimpleName() + ".war");
        }

        ShrinkWrap.create(WebArchive.class, DelayedLivenessHealthCheck.class.getSimpleName() + ".war")
                .addClasses(DelayedLivenessHealthCheck.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .as(ZipExporter.class)
                .exportTo(deployment, true);

        return deployment;
    }

    @Before
    public void createExecutorService() {
        executorService = Executors.newSingleThreadExecutor();
    }

    @After
    public void shutDownExecutorService() {
        executorService.shutdownNow();
    }

    /**
     * This test is executed as the first one in order simulate a {@code @BeforeClass} annotated method and to
     * start/stop the Wildfly container manually and configure the initial readiness health check status.
     *
     * This is necessary because an instance of {@link OnlineManagementClient} is needed in order to act on a
     * server instance which is started manually by the {@link ContainerController} resource.
     * For the above reasons, a {@code @BeforeClass} annotated method cannot be used as - being it {@code static} - no
     * access would be possible to the above mentioned field.
     *
     * @throws InterruptedException
     * @throws TimeoutException
     * @throws CliException
     * @throws ConfigurationException
     * @throws IOException
     */
    @Test
    @InSequence(1)
    public void before() throws InterruptedException, TimeoutException, CliException, ConfigurationException, IOException {
        arqProps = new ArquillianContainerProperties(
                ArquillianDescriptorWrapper.getArquillianDescriptor(), ManualTests.ARQUILLIAN_CONTAINER);
        setInitialHealthCheckStatus();
    }

    /**
     * This test is executed as the last one in order simulate an {@code @AfterClass} annotated method and to
     * start/stop the Wildfly container manually and configure the initial readiness health check status.
     *
     * This is necessary because an instance of {@link OnlineManagementClient} is needed in order to act on a
     * server instance which is started manually by the {@link ContainerController} resource.
     * For the above reasons, a {@code @BeforeClass} annotated method cannot be used as - being it {@code static} - no
     * access would be possible to the above mentioned field.
     *
     * @throws InterruptedException
     * @throws TimeoutException
     * @throws CliException
     * @throws ConfigurationException
     * @throws IOException
     */
    @Test
    @InSequence(4)
    public void after() throws InterruptedException, TimeoutException, CliException, ConfigurationException, IOException {
        resetInitialHealthCheckStatus();
    }

    /**
     * @tpTestDetails Deploys a delayed <b>readiness</b> health check archive and <i>then</i> manually starts Wildfly to
     *                verify that the behavior of the server would be compliant with the MP Health spec, specifically when
     *                setting
     *                {@code MP_HEALTH_EMPTY_READINESS_CHECKS_STATUS} environment variable to {@code DOWN} - i.e. the server
     *                will
     *                not respond with the expected readiness probe (DOWN with a custom check in this case) until the related
     *                health
     *                check is actually ready.
     * @tpPassCrit The first readiness probe must return HTTP 503 with a {@code DOWN} status and then - once the
     *             readiness health check is actually ready - it must return HTTP 200 and an {@code UP} status.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @InSequence(2)
    public void delayedConstructorTest()
            throws IOException, ConfigurationException, InterruptedException, TimeoutException, ExecutionException {

        File source = delayedDownReadinessCheckDeployment();
        File dest = new File(System.getProperty("container.base.dir.manual.mode") + "/deployments/" + source.getName());
        Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        try {
            ReadinessChecker readinessChecker = new ReadinessChecker(arqProps);
            Future<Boolean> readinessCheckerFuture = executorService.submit(readinessChecker);

            controller.start(ManualTests.ARQUILLIAN_CONTAINER);
            try {
                get(HealthUrlProvider.readyEndpoint(arqProps)).then()
                        .statusCode(503)
                        .body("status", is("DOWN"),
                                "checks.name", containsInAnyOrder(DelayedReadinessHealthCheck.NAME, "deployments-status",
                                        "boot-errors", "server-state"));
            } finally {
                controller.stop(ManualTests.ARQUILLIAN_CONTAINER);
            }

            readinessChecker.stop();
            Assert.assertTrue(readinessCheckerFuture.get(5, TimeUnit.SECONDS));

            ReadinessStatesValidator.of(readinessChecker)
                    .finishedProperly()
                    .containSequence(
                            ReadinessState.START(),
                            ReadinessState.UNABLE_TO_CONNECT(),
                            ReadinessState.DOWN_NO_CHECK(),
                            ReadinessState.DOWN_WITH_CHECK());
        } finally {
            Files.delete(dest.toPath());
        }
    }

    /**
     * @tpTestDetails Deploys a delayed <b>liveness</b> health check archive and <i>then</i> manually starts Wildfly to
     *                verify that the behavior of the server would be compliant with the MP Health spec, specifically when
     *                setting
     *                {@code MP_HEALTH_EMPTY_READINESS_CHECKS_STATUS} environment variable to {@code DOWN} - i.e. the server
     *                will
     *                not respond with the default {@code UP} readiness probe until the deployment
     *                scan is
     *                completed and no readiness checks are found.
     * @tpPassCrit The first readiness probe must return HTTP 503 with a {@code DOWN} status and then - once the
     *             readiness health check is actually ready - it must return HTTP 200 and an {@code UP} status.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @InSequence(3)
    public void unexpectedHealthChecksTest()
            throws IOException, ConfigurationException, InterruptedException, TimeoutException, ExecutionException {

        File source = unexpectedReadinessChecksDeployment();
        File dest = new File(System.getProperty("container.base.dir.manual.mode") + "/deployments/" + source.getName());
        Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        try {
            ReadinessChecker readinessChecker = new ReadinessChecker(arqProps);

            Future<Boolean> readinessCheckerFuture = executorService.submit(readinessChecker);

            controller.start(ManualTests.ARQUILLIAN_CONTAINER);
            try {
                get(HealthUrlProvider.readyEndpoint(arqProps)).then()
                        .statusCode(200)
                        .body("status", is("UP"),
                                "checks.name", containsInAnyOrder(
                                        "ready-deployment." + DelayedLivenessHealthCheck.class.getSimpleName() + ".war",
                                        "deployments-status", "boot-errors", "server-state"));
            } finally {
                controller.stop(ManualTests.ARQUILLIAN_CONTAINER);
            }

            readinessChecker.stop();
            Assert.assertTrue(readinessCheckerFuture.get(5, TimeUnit.SECONDS));

            ReadinessStatesValidator.of(readinessChecker)
                    .finishedProperly()
                    .containSequence(
                            ReadinessState.START(),
                            ReadinessState.UNABLE_TO_CONNECT(),
                            ReadinessState.DOWN_NO_CHECK(),
                            ReadinessState.UP_WITH_DEFAULT_CHECK());
        } finally {
            Files.delete(dest.toPath());
        }
    }
}
