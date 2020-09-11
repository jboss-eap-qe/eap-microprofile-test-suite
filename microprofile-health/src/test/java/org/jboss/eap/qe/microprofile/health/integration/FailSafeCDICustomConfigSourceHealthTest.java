package org.jboss.eap.qe.microprofile.health.integration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.eap.qe.microprofile.health.DisableDefaultHealthProceduresSetupTask;
import org.jboss.eap.qe.microprofile.tooling.server.ModuleUtil;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.MicroProfileServerSetupTask;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;

/**
 * MP FT integration will be tested relying on custom module.
 * MP Config property is provided by {@link CustomConfigSource}.
 * Purpose of the class is to configure MP Config. Superclass is responsible for test execution.
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({ DisableDefaultHealthProceduresSetupTask.class, MicroProfileFTSetupTask.class,
        FailSafeCDICustomConfigSourceHealthTest.SetupTask.class })
public class FailSafeCDICustomConfigSourceHealthTest extends FailSafeCDIHealthDynamicBaseTest {
    private static final String PROPERTY_FILENAME = "health.properties";
    Path propertyFile = Paths.get(FailSafeCDICustomConfigSourceHealthTest.class.getResource(PROPERTY_FILENAME).getPath());
    private byte[] bytes;

    @Before
    public void backup() throws IOException {
        bytes = Files.readAllBytes(propertyFile);
    }

    @After
    public void restore() throws IOException {
        Files.write(propertyFile, bytes);
    }

    @Override
    protected void setConfigProperties(boolean live, boolean ready, boolean inMaintenance, boolean readyInMaintenance)
            throws Exception {
        String content = String.format("%s=%s\n%s=%s\n%s=%s\n%s=%s",
                FailSafeDummyService.LIVE_CONFIG_PROPERTY, live,
                FailSafeDummyService.READY_CONFIG_PROPERTY, ready,
                FailSafeDummyService.READY_IN_MAINTENANCE_CONFIG_PROPERTY, readyInMaintenance,
                FailSafeDummyService.IN_MAINTENANCE_CONFIG_PROPERTY, inMaintenance);
        // TODO Java 11 API way - Files.writeString(propertyFilePath, INCREMENT_CONFIG_PROPERTY + "=" + increment);
        Files.write(propertyFile, content.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Setup a microprofile-config-smallrye subsystem to obtain values from {@link CustomConfigSource}
     */
    static class SetupTask implements MicroProfileServerSetupTask {
        private static final String TEST_MODULE_NAME = "test.custom-config-source";

        @Override
        public void setup() throws Exception {
            try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
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
        }

        @Override
        public void tearDown() throws Exception {
            try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
                client.execute(String.format("/system-property=%s:remove", CustomConfigSource.PROPERTIES_FILE_PATH))
                        .assertSuccess();
                client.execute("/subsystem=microprofile-config-smallrye/config-source=cs-from-class:remove").assertSuccess();
                ModuleUtil.remove(TEST_MODULE_NAME).executeOn(client);
            }
        }
    }
}
