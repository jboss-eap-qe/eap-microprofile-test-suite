package org.jboss.eap.qe.microprofile.fault.tolerance.integration.metrics;

import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.containsString;

import java.net.URL;
import java.util.List;

import org.apache.http.HttpStatus;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.eap.qe.microprofile.common.setuptasks.MicroProfileFaultToleranceServerConfiguration;
import org.jboss.eap.qe.microprofile.common.setuptasks.MicroProfileTelemetryServerConfiguration;
import org.jboss.eap.qe.microprofile.common.setuptasks.MicrometerServerConfiguration;
import org.jboss.eap.qe.microprofile.common.setuptasks.OpenTelemetryServerConfiguration;
import org.jboss.eap.qe.microprofile.fault.tolerance.deployments.v10.HelloService;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.deployment.ConfigurationUtil;
import org.jboss.eap.qe.observability.containers.OpenTelemetryCollectorContainer;
import org.jboss.eap.qe.observability.prometheus.model.PrometheusMetric;
import org.jboss.eap.qe.ts.common.docker.Docker;
import org.jboss.eap.qe.ts.common.docker.junit.DockerRequiredTests;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

/**
 * Verify the server behavior from the end user PoV, based on whether various extensions and subsystems that
 * handle metrics generation and collection are <i>available</i> and <i>enabled</i>, or not.
 *
 * Uses a workflow that is implemented already in {@link org.jboss.eap.qe.microprofile.fault.tolerance.UndeployDeployTest},
 * i.e. several deployments are defined, and used via manual deploy conveniently.
 * In order to be able to inject the deployments URLs, though, Arquillian needs for the deployments to be deployed
 * initially, which is the reason for the first test in the sequence. Deployments are undeployed after each test.
 */
@RunAsClient
@RunWith(Arquillian.class)
@Category(DockerRequiredTests.class)
public class MultipleMetricsProvidersTest {

    private static final String FAULT_TOLERANCE_DEPLOYMENT = "FTDeployment";
    private static final String FAULT_TOLERANCE_DEPOYMENT_WITH_MP_TELEMETRY = "FTDeploymentWithMPTelemetryEnabled";
    private static final String FAULT_TOLERANCE_DEPOYMENT_WITH_MP_TELEMETRY_DISABLING_MICROMETER = "FTDeploymentWithMPTelemetryEnabledButDisablingMicrometer";
    private static final String TESTED_FT_METRIC = "ft_timeout_";
    private static final int REQUEST_COUNT = 5;

    @ArquillianResource
    private Deployer deployer;

    @BeforeClass
    public static void setup() throws Exception {
        // we need a Docker container for The OTel collector here, so throw an exception if a docker service is not available
        try {
            Docker.checkDockerPresent();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot verify Docker availability: " + e.getMessage());
        }
        // Enable FT
        MicroProfileFaultToleranceServerConfiguration.enableFaultTolerance();
    }

    /**
     * A deployment containing MP Fault Tolerance operations that generate metrics
     *
     * @return A ShrinkWrap {@link Archive> containing a deployment that contains MP Fault Tolerance operations
     */
    @Deployment(name = FAULT_TOLERANCE_DEPLOYMENT, managed = false)
    public static Archive<?> deployment() {
        String mpConfig = "otel.metric.export.interval=100\nTimeout/enabled=true";
        return ShrinkWrap.create(WebArchive.class, FAULT_TOLERANCE_DEPLOYMENT + ".war")
                .addPackages(true, HelloService.class.getPackage())
                .addAsManifestResource(ConfigurationUtil.BEANS_XML_FILE_LOCATION, "beans.xml")
                .addAsManifestResource(new StringAsset(mpConfig), "microprofile-config.properties");
    }

    /**
     * A deployment that enables MP Telemetry via the {@code otel.sdk.disabled=false} MP Config property.
     *
     * @return A ShrinkWrap {@link Archive> containing a deployment that enables MP Telemetry
     */
    @Deployment(name = FAULT_TOLERANCE_DEPOYMENT_WITH_MP_TELEMETRY, managed = false)
    public static Archive<?> deploymentWithMPTelemetryEnabled() {
        String mpConfig = "otel.metric.export.interval=100\notel.service.name=" + FAULT_TOLERANCE_DEPOYMENT_WITH_MP_TELEMETRY
                + "\notel.sdk.disabled=false\nTimeout/enabled=true";
        return ShrinkWrap.create(WebArchive.class, FAULT_TOLERANCE_DEPOYMENT_WITH_MP_TELEMETRY + ".war")
                .addPackages(true, HelloService.class.getPackage())
                .addAsManifestResource(ConfigurationUtil.BEANS_XML_FILE_LOCATION, "beans.xml")
                .addAsManifestResource(new StringAsset(mpConfig), "microprofile-config.properties");
    }

    /**
     * A deployment that enables MP Telemetry via the {@code otel.sdk.disabled=false} MP Config property, and
     * disabling MP Fault Tolerance metrics collection using Micrometer via the
     * {@code smallrye.faulttolerance.micrometer.disabled}
     * MP Config property.
     *
     * @return A ShrinkWrap {@link Archive> containing a deployment that enables MP Telemetry
     */
    @Deployment(name = FAULT_TOLERANCE_DEPOYMENT_WITH_MP_TELEMETRY_DISABLING_MICROMETER, managed = false)
    public static Archive<?> deploymentWithMPTelemetryEnabledDisablingMicrometer() {
        String mpConfig = "otel.metric.export.interval=100\notel.service.name="
                + FAULT_TOLERANCE_DEPOYMENT_WITH_MP_TELEMETRY_DISABLING_MICROMETER
                + "\notel.sdk.disabled=false\nTimeout/enabled=true\nsmallrye.faulttolerance.micrometer.disabled=true";
        return ShrinkWrap.create(WebArchive.class, FAULT_TOLERANCE_DEPOYMENT_WITH_MP_TELEMETRY_DISABLING_MICROMETER + ".war")
                .addPackages(true, HelloService.class.getPackage())
                .addAsManifestResource(ConfigurationUtil.BEANS_XML_FILE_LOCATION, "beans.xml")
                .addAsManifestResource(new StringAsset(mpConfig), "microprofile-config.properties");
    }

    /**
     * Deploy all deployments, so it's possible to get their URLs in other tests. This is limitation of Arquillian
     * when deployments are deployed manually (in tests) and thus their URL is not known in time when test is started.
     * The only way how to workaround this issue is to deploy all deployments in first test which allows to inject
     * deployment URL as params in other test methods.
     */
    @Test
    @InSequence(0)
    public void deployAll() throws Exception {
        // we enable Micrometer and/or MP Telemetry/OpenTelemetry selectively
        MicroProfileTelemetryServerConfiguration.disableMicroProfileTelemetry();
        OpenTelemetryServerConfiguration.disableOpenTelemetry();
        MicrometerServerConfiguration.disableMicrometer();
        // Deploy/undeploy everything to let Arquillian inject URLs... (workaround for multiple manual deployment tests)
        deployer.deploy(FAULT_TOLERANCE_DEPLOYMENT);
        deployer.deploy(FAULT_TOLERANCE_DEPOYMENT_WITH_MP_TELEMETRY);
        deployer.deploy(FAULT_TOLERANCE_DEPOYMENT_WITH_MP_TELEMETRY_DISABLING_MICROMETER);

        deployer.undeploy(FAULT_TOLERANCE_DEPLOYMENT);
        deployer.undeploy(FAULT_TOLERANCE_DEPOYMENT_WITH_MP_TELEMETRY);
        deployer.undeploy(FAULT_TOLERANCE_DEPOYMENT_WITH_MP_TELEMETRY_DISABLING_MICROMETER);
    }

    /**
     * Test to verify that no MicroProfile FaultTolerance related metrics are visible via the OTel collector Prometheus
     * endpoint, when both the Micrometer and MicroProfile Telemetry extensions are not <i>available</i>.
     *
     * @param deploymentUrl Base {@link URL} of the app deployment
     * @throws Exception When something fails during server configuration
     */
    @Test
    @InSequence(10)
    public void noMetricsAreCollectedWhenMetricsExtensionsAreNotAvailable(
            @ArquillianResource @OperateOnDeployment(FAULT_TOLERANCE_DEPLOYMENT) URL deploymentUrl) throws Exception {
        // Remove the microprofile-telemetry and opentelemetry extensions
        MicroProfileTelemetryServerConfiguration.disableMicroProfileTelemetry();
        OpenTelemetryServerConfiguration.disableOpenTelemetry();
        // And be sure Micrometer is not available as well
        MicrometerServerConfiguration.disableMicrometer();
        // start the OTel collector container
        OpenTelemetryCollectorContainer otelCollector = OpenTelemetryCollectorContainer.getNewInstance();
        otelCollector.start();
        try {
            // deploy
            deployer.deploy(FAULT_TOLERANCE_DEPLOYMENT);
            try {
                // call the app operation for some times
                for (int i = 0; i < REQUEST_COUNT; i++) {
                    get(deploymentUrl + "?operation=timeout&context=foobar&fail=true").then()
                            .assertThat()
                            .statusCode(HttpStatus.SC_OK)
                            .body(containsString("Fallback Hello, context = foobar"));
                }
                // give it some time to actually be able and report some metrics via the Pmetheus URL
                Thread.sleep(1_000);
                // fetch the collected metrics in prometheus format
                List<PrometheusMetric> metrics = otelCollector.fetchMetrics(TESTED_FT_METRIC);
                Assert.assertTrue(
                        TESTED_FT_METRIC + " metric found, which is not expected when both MP Telemetry and Micrometer "
                                +
                                "extensions are not available.",
                        metrics.stream().noneMatch(m -> m.getKey().startsWith(TESTED_FT_METRIC)));
            } finally {
                deployer.undeploy(FAULT_TOLERANCE_DEPLOYMENT);
            }
        } finally {
            otelCollector.stop();
        }
    }

    /**
     * Test to verify that MP Fault Tolerance metrics are collected when Micrometer alone is available and configured
     *
     * @param deploymentUrl Base {@link URL} of the app deployment
     * @throws Exception When something fails during server configuration
     */
    @Test
    @InSequence(20)
    public void metricsAreCollectedWhenOnlyMicrometerExtensionIsAvailable(
            @ArquillianResource @OperateOnDeployment(FAULT_TOLERANCE_DEPLOYMENT) URL deploymentUrl) throws Exception {
        // Remove the microprofile-telemetry and opentelemetry extensions
        MicroProfileTelemetryServerConfiguration.disableMicroProfileTelemetry();
        OpenTelemetryServerConfiguration.disableOpenTelemetry();
        // start the OTel collector container
        final OpenTelemetryCollectorContainer otelCollector = OpenTelemetryCollectorContainer.getNewInstance();
        otelCollector.start();
        try {
            // And be sure Micrometer is available instead
            MicrometerServerConfiguration.enableMicrometer(otelCollector.getOtlpHttpEndpoint());
            try {
                // deploy
                deployer.deploy(FAULT_TOLERANCE_DEPLOYMENT);
                try {
                    // call the app operation for some times
                    for (int i = 0; i < REQUEST_COUNT; i++) {
                        get(deploymentUrl + "?operation=timeout&context=foobar&fail=true").then()
                                .assertThat()
                                .statusCode(HttpStatus.SC_OK)
                                .body(containsString("Fallback Hello, context = foobar"));
                    }
                    // give it some time to actually be able and report some metrics via the Pmetheus URL
                    Thread.sleep(1_000);
                    // fetch the collected metrics in prometheus format
                    List<PrometheusMetric> metrics = otelCollector.fetchMetrics(TESTED_FT_METRIC);
                    Assert.assertFalse(
                            TESTED_FT_METRIC
                                    + " metrics not found, which is not expected when the Micrometer extension is available, "
                                    +
                                    "i.e. FT metrics should be collected.",
                            metrics.isEmpty());
                } finally {
                    deployer.undeploy(FAULT_TOLERANCE_DEPLOYMENT);
                }
            } finally {
                MicrometerServerConfiguration.disableMicrometer();
            }
        } finally {
            otelCollector.stop();
        }
    }

    /**
     * Test to verify that MP Fault Tolerance metrics are NOT collected when MicroProfile Telemetry alone is available
     * but not enabled, i.e. via the {@code otel.sdk.disabled=false} MP Config property
     *
     * @param deploymentUrl Base {@link URL} of the app deployment
     * @throws Exception When something fails during server configuration
     */
    @Test
    @InSequence(30)
    public void noMetricsAreCollectedWhenOnlyMPTelemetryIsAvailableButNotEnabled(
            @ArquillianResource @OperateOnDeployment(FAULT_TOLERANCE_DEPLOYMENT) URL deploymentUrl) throws Exception {
        // Remove the Micrometer extension...
        MicrometerServerConfiguration.disableMicrometer();
        // ... start the OTel collector container...
        final OpenTelemetryCollectorContainer otelCollector = OpenTelemetryCollectorContainer.getNewInstance();
        otelCollector.start();
        try {
            // ... and make the opentelemetry extension available...
            OpenTelemetryServerConfiguration.enableOpenTelemetry();
            try {
                OpenTelemetryServerConfiguration.addOpenTelemetryCollectorConfiguration(otelCollector.getOtlpGrpcEndpoint());
                // ... and finally the microProfile-telemetry extension available
                MicroProfileTelemetryServerConfiguration.enableMicroProfileTelemetry();
                try {
                    // deploy an app that DOES NOT enable MP Telemetry
                    deployer.deploy(FAULT_TOLERANCE_DEPLOYMENT);
                    try {
                        // call the app operation for some times
                        for (int i = 0; i < REQUEST_COUNT; i++) {
                            get(deploymentUrl + "?operation=timeout&context=foobar&fail=true").then()
                                    .assertThat()
                                    .statusCode(HttpStatus.SC_OK)
                                    .body(containsString("Fallback Hello, context = foobar"));
                        }
                        // give it some time to actually be able and report some metrics via the Pmetheus URL
                        Thread.sleep(1_000);
                        // fetch the collected metrics in prometheus format
                        List<PrometheusMetric> metrics = otelCollector.fetchMetrics(TESTED_FT_METRIC);
                        Assert.assertTrue(
                                TESTED_FT_METRIC + " metrics found, which is not expected when only the MP Telemetry extension "
                                        +
                                        "is available but NOT enabled at application level.",
                                metrics.stream().noneMatch(m -> m.getKey().startsWith(TESTED_FT_METRIC)));
                    } finally {
                        deployer.undeploy(FAULT_TOLERANCE_DEPLOYMENT);
                    }
                } finally {
                    MicroProfileTelemetryServerConfiguration.disableMicroProfileTelemetry();
                }
            } finally {
                OpenTelemetryServerConfiguration.disableOpenTelemetry();
            }
        } finally {
            otelCollector.stop();
        }
    }

    /**
     * Test to verify that MP Fault Tolerance metrics are collected when MicroProfile Telemetry alone is available
     * AND enabled, i.e. via the {@code otel.sdk.disabled=false} MP Config property
     *
     * @param deploymentUrl Base {@link URL} of the app deployment
     * @throws Exception When something fails during server configuration
     */
    @Test
    @InSequence(40)
    public void metricsAreCollectedWhenOnlyMPTelemetryExtensionIsAvailableAndEnabled(
            @ArquillianResource @OperateOnDeployment(FAULT_TOLERANCE_DEPOYMENT_WITH_MP_TELEMETRY) URL deploymentUrl)
            throws Exception {
        // Remove the Micrometer extension
        MicrometerServerConfiguration.disableMicrometer();
        // start the OTel collector container
        OpenTelemetryCollectorContainer otelCollector = OpenTelemetryCollectorContainer.getNewInstance();
        otelCollector.start();
        try {
            // ... and make the opentelemetry extension available...
            OpenTelemetryServerConfiguration.enableOpenTelemetry();
            try {
                OpenTelemetryServerConfiguration
                        .addOpenTelemetryCollectorConfiguration(otelCollector.getOtlpGrpcEndpoint());
                // ... and finally make the microprofile-telemetry extension available
                MicroProfileTelemetryServerConfiguration.enableMicroProfileTelemetry();
                try {
                    // deploy an app that enables MP Telemetry
                    deployer.deploy(FAULT_TOLERANCE_DEPOYMENT_WITH_MP_TELEMETRY);
                    try {
                        // call the app operation for some times
                        for (int i = 0; i < REQUEST_COUNT; i++) {
                            get(deploymentUrl + "?operation=timeout&context=foobar&fail=true").then()
                                    .assertThat()
                                    .statusCode(HttpStatus.SC_OK)
                                    .body(containsString("Fallback Hello, context = foobar"));
                        }
                        // give it some time to actually be able and report some metrics via the Pmetheus URL
                        Thread.sleep(1_000);
                        // fetch the collected metrics in prometheus format
                        List<PrometheusMetric> metrics = otelCollector.fetchMetrics(TESTED_FT_METRIC);
                        Assert.assertFalse(
                                TESTED_FT_METRIC
                                        + " metrics not found, which is not expected when the MP Telemetry extension is available "
                                        +
                                        "and enabled at the application level, i.e. FT metrics should be collected.",
                                metrics.isEmpty());
                    } finally {
                        deployer.undeploy(FAULT_TOLERANCE_DEPOYMENT_WITH_MP_TELEMETRY);
                    }
                } finally {
                    MicroProfileTelemetryServerConfiguration.disableMicroProfileTelemetry();
                }
            } finally {
                OpenTelemetryServerConfiguration.disableOpenTelemetry();
            }
        } finally {
            otelCollector.stop();
        }
    }

    /**
     * Test to verify that MP Fault Tolerance metrics are collected uniquely when both MicroProfile Telemetry is
     * and Micrometer subsystems are available, but MP Telemetry is enabled, i.e. via the {@code otel.sdk.disabled=false}
     * MP Config property, while Micrometer is not (via the {@code smallrye.faulttolerance.micrometer.disabled=true}
     * MP Config property).
     *
     * @param deploymentUrl Base {@link URL} of the app deployment
     * @throws Exception When something fails during server configuration
     */
    @Test
    @InSequence(50)
    public void metricsAreCollectedWhenBothExtensionsAreAvailableAndOnlyMPTelIsEnabled(
            @ArquillianResource @OperateOnDeployment(FAULT_TOLERANCE_DEPOYMENT_WITH_MP_TELEMETRY_DISABLING_MICROMETER) URL deploymentUrl)
            throws Exception {
        // start the OTel collector container
        OpenTelemetryCollectorContainer otelCollector = OpenTelemetryCollectorContainer.getNewInstance();
        otelCollector.start();
        try {
            // ... and make the opentelemetry extension available...
            OpenTelemetryServerConfiguration.enableOpenTelemetry();
            try {
                OpenTelemetryServerConfiguration
                        .addOpenTelemetryCollectorConfiguration(otelCollector.getOtlpGrpcEndpoint());
                // ... then make the microprofile-telemetry extension available
                MicroProfileTelemetryServerConfiguration.enableMicroProfileTelemetry();
                try {
                    // And be sure Micrometer is available too
                    MicrometerServerConfiguration.enableMicrometer(otelCollector.getOtlpHttpEndpoint());
                    try {
                        // deploy an app that enables MP Telemetry, and disables the Micrometer Fault Tolerance metrics collection
                        // instead, see
                        deployer.deploy(FAULT_TOLERANCE_DEPOYMENT_WITH_MP_TELEMETRY_DISABLING_MICROMETER);
                        try {
                            // call the app operation for some times
                            for (int i = 0; i < REQUEST_COUNT; i++) {
                                get(deploymentUrl + "?operation=timeout&context=foobar&fail=true").then()
                                        .assertThat()
                                        .statusCode(HttpStatus.SC_OK)
                                        .body(containsString("Fallback Hello, context = foobar"));
                            }
                            // give it some time to actually be able and report some metrics via the Pmetheus URL
                            Thread.sleep(1_000);
                            // fetch the collected metrics in prometheus format
                            List<PrometheusMetric> metrics = otelCollector.fetchMetrics(TESTED_FT_METRIC);
                            Assert.assertFalse(
                                    TESTED_FT_METRIC
                                            + " metrics not found, which is not expected when the Micrometer extension is available, i.e. FT metrics should be collected.",
                                    metrics.isEmpty());
                            Assert.assertEquals(
                                    "Duplicate metrics were found, which is not expected when both the Micrometer and MP Telemetry extension "
                                            +
                                            "are available but the deployment is explicitly enabling MP Telemetry while disabling Micrometer metrics collection instead.",
                                    metrics.size(), metrics.stream().distinct().count());
                        } finally {
                            deployer.undeploy(FAULT_TOLERANCE_DEPOYMENT_WITH_MP_TELEMETRY_DISABLING_MICROMETER);
                        }
                    } finally {
                        MicrometerServerConfiguration.disableMicrometer();
                    }
                } finally {
                    MicroProfileTelemetryServerConfiguration.disableMicroProfileTelemetry();
                }
            } finally {
                OpenTelemetryServerConfiguration.disableOpenTelemetry();
            }
        } finally {
            otelCollector.stop();
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // disable FT
        MicroProfileFaultToleranceServerConfiguration.disableFaultTolerance();
    }
}
