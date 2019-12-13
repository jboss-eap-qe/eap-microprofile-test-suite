package org.jboss.eap.qe.microprofile.metrics.integration.config;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.eap.qe.microprofile.tooling.server.ModuleUtil;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;

/**
 * MP Config property is provided by ConfigSource class provided by custom provider - CustomConfigSourceProvider.
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(CustomMetricCustomConfigSourceProviderTest.SetupTask.class)
public class CustomMetricCustomConfigSourceProviderTest extends CustomMetricDynamicBaseTest {
    private static final String PROPERTY_FILENAME = "custom-metric.properties";

    private byte[] bytes;

    private File propertyFile = new File(
            CustomMetricCustomConfigSourceProviderTest.class.getResource(PROPERTY_FILENAME).getPath());

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        WebArchive webArchive = ShrinkWrap
                .create(WebArchive.class, CustomMetricCustomConfigSourceProviderTest.class.getSimpleName() + ".war")
                .addClasses(CustomCounterIncrementProvider.class, CustomCounterMetric.class, CustomMetricService.class,
                        CustomMetricApplication.class, CustomMetricAppInitializer.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return webArchive;
    }

    @Before
    public void backup() throws IOException {
        bytes = FileUtils.readFileToByteArray(propertyFile);
    }

    @After
    public void restore() throws IOException {
        FileUtils.writeByteArrayToFile(propertyFile, bytes);
    }

    void setConfigProperties(int increment) throws IOException {
        FileUtils.writeStringToFile(propertyFile, INCREMENT_CONFIG_PROPERTY + "=" + Integer.toString(increment));
    }

    /**
     * Setup a microprofile-config-smallrye subsystem to obtain values from {@link CustomConfigSource} provided by
     * {@link CustomConfigSourceProvider}
     */
    static class SetupTask implements ServerSetupTask {
        private static final String TEST_MODULE_NAME = "test.custom-config-source-provider";

        @Override
        public void setup(ManagementClient managementClient, String s) throws Exception {
            try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
                client.execute(String.format("/system-property=%s:add(value=%s)", CustomConfigSource.FILEPATH_PROPERTY,
                        SetupTask.class.getResource(PROPERTY_FILENAME).getPath()));
                ModuleUtil.add(TEST_MODULE_NAME)
                        .setModuleXMLPath(SetupTask.class.getResource("configSourceProviderModule.xml").getPath())
                        .addResource("config-source-provider", CustomConfigSource.class, CustomConfigSourceProvider.class)
                        .executeOn(client);
                client.execute(String.format(
                        "/subsystem=microprofile-config-smallrye/config-source-provider=cs-from-provider:add(class={module=%s, name=%s})",
                        TEST_MODULE_NAME, CustomConfigSourceProvider.class.getName()));
            }
        }

        @Override
        public void tearDown(ManagementClient managementClient, String s) throws Exception {
            try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
                client.execute(String.format("/system-property=%s:remove", CustomConfigSource.FILEPATH_PROPERTY));
                client.execute("/subsystem=microprofile-config-smallrye/config-source-provider=cs-from-provider:remove");
                ModuleUtil.remove(TEST_MODULE_NAME).executeOn(client);
            }
        }
    }
}
