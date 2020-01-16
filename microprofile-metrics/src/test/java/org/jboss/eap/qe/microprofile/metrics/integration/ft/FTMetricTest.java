package org.jboss.eap.qe.microprofile.metrics.integration.ft;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeoutException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.eap.qe.microprofile.tooling.server.ModuleUtil;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.ConfigurationException;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.ArquillianContainerProperties;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.ArquillianDescriptorWrapper;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;

import io.restassured.http.ContentType;

@Ignore("https://issues.redhat.com/browse/WFLY-12590")
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(FTMetricTest.SetupTask.class)
public class FTMetricTest {
    public static final String INCREMENT_CONFIG_PROPERTY = "dummy.increment";
    public static final String FAULT_CTL_CONFIG_PROPERTY = "dummy.corrupted";
    public static final String FAILSAFE_INCREMENT_CONFIG_PROPERTY = "dummy.failsafe.increment";

    @ArquillianResource
    URL deploymentUrl;

    private byte[] bytes;

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, FTMetricTest.class.getSimpleName() + ".war")
                .addPackage(FTCustomMetricApplication.class.getPackage())
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Before
    public void reload() throws ConfigurationException, InterruptedException, TimeoutException, IOException {
        try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
            new Administration(client).reload();
        }
    }

    @Before
    public void backup() throws IOException {
        bytes = Files.readAllBytes(SetupTask.propertyFilePath);
    }

    @After
    public void restore() throws IOException {
        Files.write(SetupTask.propertyFilePath, bytes);
    }

    /**
     * @tpTestDetails Multi-component customer scenario to verify custom counter metric is registered & exposed.
     *                The counter increment value depends on MP Config property values provided by a CDI bean
     *                The increment for the counter is provided by a fail-safe service. If one provider fails
     *                (controlled by MP Config property) the fail-safe service fallbacks to another provider. Values
     *                returned by providers are configured via MP Config.
     * @tpPassCrit Counter metric is incremented by configured value
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void incrementProviderTest() throws IOException, ConfigurationException {
        setMPConfig(false, 2, 3);
        performRequest();
        performRequest();
        testCustomMetricForValue(4);
        testInvocationsMetric(2);
        performRequest();
        performRequest();
        performRequest();
        testCustomMetricForValue(10);
        testInvocationsMetric(5);
    }

    /**
     * @tpTestDetails Multi-component customer scenario to verify custom counter metric is registered & exposed.
     *                The counter increment value depends on MP Config property values provided by a CDI bean
     *                The increment for the counter is provided by a fail-safe service. If one provider fails
     *                (controlled by MP Config property) the fail-safe service fallbacks to another provider. Values
     *                returned by providers are configured via MP Config.
     * @tpPassCrit Counter metric is incremented by configured value change dynamically
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void incrementFailSafeProviderTest() throws IOException, ConfigurationException {
        setMPConfig(false, 1, 3);
        performRequest();
        performRequest();
        testCustomMetricForValue(2);
        testInvocationsMetric(2);
        setMPConfig(true, 2, 4);
        performRequest();
        performRequest();
        performRequest();
        testCustomMetricForValue(14);
        testInvocationsMetric(5);
        testFallbackMetric(3);
        setMPConfig(false, 10, 6);
        performRequest();
        testCustomMetricForValue(24);
        testInvocationsMetric(6);
        testFallbackMetric(3);
    }

    private void setMPConfig(boolean providerCorrupted, int increment, int failSafeIncrement) throws IOException {
        String content = String.format("%s=%s%n%s=%s%n%s=%s",
                FAULT_CTL_CONFIG_PROPERTY, providerCorrupted,
                INCREMENT_CONFIG_PROPERTY, increment,
                FAILSAFE_INCREMENT_CONFIG_PROPERTY, failSafeIncrement);
        //      TODO Java 11 API way - Files.writeString(SetupTask.propertyFilePath, content);
        Files.write(SetupTask.propertyFilePath, content.getBytes(StandardCharsets.UTF_8));
    }

    private void testCustomMetricForValue(int value) throws ConfigurationException {
        testAppCounterMetric("ft-custom-metric", value);
    }

    private void testInvocationsMetric(int value) throws ConfigurationException {
        testAppCounterMetric(
                "ft." + FTCustomCounterIncrementProviderService.class.getName() + ".getIncrement.invocations.total", value);
    }

    private void testFallbackMetric(int value) throws ConfigurationException {
        testAppCounterMetric(
                "ft." + FTCustomCounterIncrementProviderService.class.getName() + ".getIncrement.fallback.calls.total", value);
    }

    private void testAppCounterMetric(String name, int value) throws ConfigurationException {
        ArquillianContainerProperties arqProps = new ArquillianContainerProperties(
                ArquillianDescriptorWrapper.getArquillianDescriptor());
        given()
                .baseUri("http://" + arqProps.getDefaultManagementAddress() + ":" + arqProps.getDefaultManagementPort()
                        + "/metrics")
                .accept(ContentType.JSON)
                .get()
                .then()
                .body("application", hasKey(name),
                        "application.'" + name + "'", equalTo(value));

    }

    private void performRequest() {
        get(deploymentUrl.toString()).then()
                .statusCode(200)
                .body(equalTo("Hello from custom metric fault-tolerant service!"));
    }

    /**
     * Setup a microprofile-config-smallrye subsystem to obtain values from {@link FTCustomConfigSource}.
     * Add MP FT subsystem.
     */
    static class SetupTask implements ServerSetupTask {
        private static final String TEST_MODULE_NAME = "test.ft-custom-config-source";
        private static final String PROPERTY_FILENAME = "ft-custom-metric.properties";
        static Path propertyFilePath = Paths.get(SetupTask.class.getResource(PROPERTY_FILENAME).getPath());
        private static final PathAddress FT_EXTENSION_ADDRESS = PathAddress.pathAddress().append(EXTENSION,
                "org.wildfly.extension.microprofile.fault-tolerance-smallrye");
        private static final PathAddress FT_SUBSYSTEM_ADDRESS = PathAddress.pathAddress().append(SUBSYSTEM,
                "microprofile-fault-tolerance-smallrye");

        @Override
        public void setup(ManagementClient managementClient, String s) throws Exception {
            try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
                client.execute(String.format("/system-property=%s:add(value=%s)", FTCustomConfigSource.FILEPATH_PROPERTY,
                        SetupTask.class.getResource(PROPERTY_FILENAME).getPath()));
                ModuleUtil.add(TEST_MODULE_NAME)
                        .setModuleXMLPath(SetupTask.class.getResource("module.xml").getPath())
                        .addResource("ft-config-source", FTCustomConfigSource.class)
                        .executeOn(client);
                client.execute(String.format(
                        "/subsystem=microprofile-config-smallrye/config-source=cs-from-class:add(class={module=%s, name=%s})",
                        TEST_MODULE_NAME, FTCustomConfigSource.class.getName()));
                client.execute(Util.createAddOperation(FT_EXTENSION_ADDRESS));
                client.execute(Util.createAddOperation(FT_SUBSYSTEM_ADDRESS));
            }
        }

        @Override
        public void tearDown(ManagementClient managementClient, String s) throws Exception {
            try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
                client.execute("/subsystem=microprofile-config-smallrye/config-source=cs-from-class:remove");
                ModuleUtil.remove(TEST_MODULE_NAME).executeOn(client);
                client.execute(Util.createRemoveOperation(FT_EXTENSION_ADDRESS));
                client.execute(Util.createRemoveOperation(FT_SUBSYSTEM_ADDRESS));
            }
        }
    }
}
