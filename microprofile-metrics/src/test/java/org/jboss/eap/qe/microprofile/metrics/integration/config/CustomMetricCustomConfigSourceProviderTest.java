package org.jboss.eap.qe.microprofile.metrics.integration.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.eap.qe.microprofile.tooling.server.ModuleUtil;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.MicroProfileServerSetupTask;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.deployment.ConfigurationUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;

/**
 * MP Config property is provided by ConfigSource class provided by custom provider - CustomConfigSourceProvider.
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(CustomMetricCustomConfigSourceProviderTest.SetupTask.class)
public class CustomMetricCustomConfigSourceProviderTest extends CustomMetricDynamicBaseTest {
    private static final String PROPERTY_FILENAME = "custom-metric.properties";

    private byte[] bytes;

    private Path propertyFilePath = Paths.get(
            CustomMetricCustomConfigSourceProviderTest.class.getResource(PROPERTY_FILENAME).getPath());

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        WebArchive webArchive = ShrinkWrap
                .create(WebArchive.class, CustomMetricCustomConfigSourceProviderTest.class.getSimpleName() + ".war")
                .addClasses(CustomCounterIncrementProvider.class, CustomCounterMetric.class, CustomMetricService.class,
                        CustomMetricApplication.class, CustomMetricAppInitializer.class)
                .addAsWebInfResource(new StringAsset(ConfigurationUtil.BEANS_XML_FILE_CONTENT), "beans.xml");
        return webArchive;
    }

    @Before
    public void backup() throws IOException {
        bytes = Files.readAllBytes(propertyFilePath);
    }

    @After
    public void restore() throws IOException {
        Files.write(propertyFilePath, bytes);
    }

    void setConfigProperties(int increment) throws IOException {
        //      TODO Java 11 API way - Files.writeString(propertyFilePath, INCREMENT_CONFIG_PROPERTY + "=" + increment);
        Files.write(propertyFilePath, (INCREMENT_CONFIG_PROPERTY + "=" + increment).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Setup a microprofile-config-smallrye subsystem to obtain values from {@link CustomConfigSource} provided by
     * {@link CustomConfigSourceProvider}
     */
    static class SetupTask implements MicroProfileServerSetupTask {
        private static final String TEST_MODULE_NAME = "test.custom-config-source-provider";

        @Override
        public void setup() throws Exception {
            try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
                client.execute(String.format("/system-property=%s:add(value=%s)", CustomConfigSource.FILEPATH_PROPERTY,
                        SetupTask.class.getResource(PROPERTY_FILENAME).getPath()));
                ModuleUtil.add(TEST_MODULE_NAME)
                        .setModuleXMLPath(SetupTask.class.getResource("configSourceProviderModule.xml").getPath())
                        .addResource("config-source-provider", CustomConfigSource.class, CustomConfigSourceProvider.class)
                        .executeOn(client);
                client.execute(String.format(
                        "/subsystem=microprofile-config-smallrye/config-source-provider=cs-from-provider:add(class={module=%s, name=%s})",
                        TEST_MODULE_NAME, CustomConfigSourceProvider.class.getName())).assertSuccess();
            }
        }

        @Override
        public void tearDown() throws Exception {
            try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
                client.execute(String.format("/system-property=%s:remove", CustomConfigSource.FILEPATH_PROPERTY))
                        .assertSuccess();
                client.execute("/subsystem=microprofile-config-smallrye/config-source-provider=cs-from-provider:remove")
                        .assertSuccess();
                ModuleUtil.remove(TEST_MODULE_NAME).executeOn(client);
                // reload server in order to apply changes
                new Administration(client).reload();
            }
        }
    }
}
