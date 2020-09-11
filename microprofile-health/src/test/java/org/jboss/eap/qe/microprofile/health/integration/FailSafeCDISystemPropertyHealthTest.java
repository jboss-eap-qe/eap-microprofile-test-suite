package org.jboss.eap.qe.microprofile.health.integration;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.eap.qe.microprofile.health.DisableDefaultHealthProceduresSetupTask;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.MicroProfileServerSetupTask;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;

/**
 * MP FT integration will be tested relying on system properties.
 * MP Config properties are defined as system properties.
 * Purpose of the class is to configure MP Config. Superclass is responsible for test execution.
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({ DisableDefaultHealthProceduresSetupTask.class, MicroProfileFTSetupTask.class,
        FailSafeCDISystemPropertyHealthTest.SetupTask.class })
public class FailSafeCDISystemPropertyHealthTest extends FailSafeCDIHealthBaseTest {

    @Override
    protected void setConfigProperties(boolean live, boolean ready, boolean inMaintenance, boolean readyInMaintenance)
            throws Exception {
        try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
            client.execute(String.format("/system-property=%s:write-attribute(name=value, value=%s)",
                    FailSafeDummyService.LIVE_CONFIG_PROPERTY, live)).assertSuccess();
            client.execute(String.format("/system-property=%s:write-attribute(name=value, value=%s)",
                    FailSafeDummyService.READY_CONFIG_PROPERTY, ready)).assertSuccess();
            client.execute(String.format("/system-property=%s:write-attribute(name=value, value=%s)",
                    FailSafeDummyService.IN_MAINTENANCE_CONFIG_PROPERTY, inMaintenance)).assertSuccess();
            client.execute(String.format("/system-property=%s:write-attribute(name=value, value=%s)",
                    FailSafeDummyService.READY_IN_MAINTENANCE_CONFIG_PROPERTY, readyInMaintenance)).assertSuccess();
            new Administration(client).reload();
        }
    }

    /**
     * Add system properties for MP Config
     */
    public static class SetupTask implements MicroProfileServerSetupTask {

        @Override
        public void setup() throws Exception {
            try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
                client.execute(String.format("/system-property=%s:add", FailSafeDummyService.LIVE_CONFIG_PROPERTY))
                        .assertSuccess();
                client.execute(String.format("/system-property=%s:add", FailSafeDummyService.READY_CONFIG_PROPERTY))
                        .assertSuccess();
                client.execute(String.format("/system-property=%s:add", FailSafeDummyService.IN_MAINTENANCE_CONFIG_PROPERTY))
                        .assertSuccess();
                client.execute(
                        String.format("/system-property=%s:add", FailSafeDummyService.READY_IN_MAINTENANCE_CONFIG_PROPERTY))
                        .assertSuccess();
            }
        }

        @Override
        public void tearDown() throws Exception {
            try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
                client.execute(String.format("/system-property=%s:remove", FailSafeDummyService.LIVE_CONFIG_PROPERTY))
                        .assertSuccess();
                client.execute(String.format("/system-property=%s:remove", FailSafeDummyService.READY_CONFIG_PROPERTY))
                        .assertSuccess();
                client.execute(String.format("/system-property=%s:remove", FailSafeDummyService.IN_MAINTENANCE_CONFIG_PROPERTY))
                        .assertSuccess();
                client.execute(
                        String.format("/system-property=%s:remove", FailSafeDummyService.READY_IN_MAINTENANCE_CONFIG_PROPERTY))
                        .assertSuccess();
            }
        }
    }
}
