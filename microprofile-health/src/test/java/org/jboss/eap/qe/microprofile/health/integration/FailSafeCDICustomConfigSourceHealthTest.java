package org.jboss.eap.qe.microprofile.health.integration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
 * MP Config property is provided by {@link CustomConfigSource}.
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({ MicroProfileFTSetupTask.class, FailSafeCDICustomConfigSourceHealthTest.SetupTask.class })
public class FailSafeCDICustomConfigSourceHealthTest extends FailSafeCDIHealthDynamicBaseTest {
    private static final String PROPERTY_FILENAME = "health.properties";
    Path propertyFile = Paths.get(FailSafeCDICustomConfigSourceHealthTest.class.getResource(PROPERTY_FILENAME).getPath());
    private byte[] bytes;

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return ShrinkWrap
                .create(WebArchive.class, FailSafeCDICustomConfigSourceHealthTest.class.getSimpleName() + ".war")
                .addClasses(FailSafeDummyService.class, CDIBasedLivenessHealthCheck.class, CDIBasedReadinessHealthCheck.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Before
    public void backup() throws IOException {
        bytes = Files.readAllBytes(propertyFile);
    }

    @After
    public void restore() throws IOException {
        Files.write(propertyFile, bytes);
    }

    @Override
    protected void setConfigProperties(boolean live, boolean ready, boolean inMaintanance, boolean readyInMainenance)
            throws Exception {
        String content = String.format("%s=%s\n%s=%s\n%s=%s\n%s=%s",
                FailSafeDummyService.LIVE_CONFIG_PROPERTY, live,
                FailSafeDummyService.READY_CONFIG_PROPERTY, ready,
                FailSafeDummyService.READY_IN_MAINTENANCE_CONFIG_PROPERTY, readyInMainenance,
                FailSafeDummyService.IN_MAINTENANCE_CONFIG_PROPERTY, inMaintanance);
        // TODO Java 11 API way - Files.writeString(propertyFilePath, INCREMENT_CONFIG_PROPERTY + "=" + increment);
        Files.write(propertyFile, content.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Setup a microprofile-config-smallrye subsystem to obtain values from {@link CustomConfigSource}
     */
    static class SetupTask implements ServerSetupTask {
        private static final String TEST_MODULE_NAME = "test.custom-config-source";

        @Override
        public void setup(ManagementClient managementClient, String s) throws Exception {
            OnlineManagementClient client = ManagementClientProvider.onlineStandalone(managementClient);
            client.execute(String.format("/system-property=%s:add(value=%s)", CustomConfigSource.PROPERTIES_FILE_PATH,
                    SetupTask.class.getResource(PROPERTY_FILENAME).getFile())).assertSuccess();
            ModuleUtil.add(TEST_MODULE_NAME)
                    .setModuleXMLPath(SetupTask.class.getResource("configSourceModule.xml").getPath())
                    .addResource("config-source", CustomConfigSource.class)
                    .executeOn(client);
            client.execute(String.format(
                    "/subsystem=microprofile-config-smallrye/config-source=cs-from-class:add(class={module=%s, name=%s})",
                    TEST_MODULE_NAME, CustomConfigSource.class.getName())).assertSuccess();
        }

        @Override
        public void tearDown(ManagementClient managementClient, String s) throws Exception {
            OnlineManagementClient client = ManagementClientProvider.onlineStandalone(managementClient);
            client.execute(String.format("/system-property=%s:remove", CustomConfigSource.PROPERTIES_FILE_PATH))
                    .assertSuccess();
            client.execute("/subsystem=microprofile-config-smallrye/config-source=cs-from-class:remove").assertSuccess();
            ModuleUtil.remove(TEST_MODULE_NAME).executeOn(client);
        }
    }
}
