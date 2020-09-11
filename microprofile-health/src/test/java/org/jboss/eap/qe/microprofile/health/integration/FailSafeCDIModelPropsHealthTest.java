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
 * MP FT integration will be tested relying on WildFly model.
 * MP Config properties are provided by props model option - values are stored in model object.
 * Purpose of the class is to configure MP Config. Superclass is responsible for test execution.
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({ DisableDefaultHealthProceduresSetupTask.class, MicroProfileFTSetupTask.class,
        FailSafeCDIModelPropsHealthTest.SetupTask.class })
public class FailSafeCDIModelPropsHealthTest extends FailSafeCDIHealthBaseTest {

    @Override
    protected void setConfigProperties(boolean live, boolean ready, boolean inMaintenance, boolean readyInMaintenance)
            throws Exception {
        try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
            client.execute(String.format(
                    "/subsystem=microprofile-config-smallrye/config-source=props:write-attribute(name=properties, value={%s=%s,%s=%s,%s=%s,%s=%s}",
                    FailSafeDummyService.LIVE_CONFIG_PROPERTY, live,
                    FailSafeDummyService.READY_CONFIG_PROPERTY, ready,
                    FailSafeDummyService.IN_MAINTENANCE_CONFIG_PROPERTY, inMaintenance,
                    FailSafeDummyService.READY_IN_MAINTENANCE_CONFIG_PROPERTY, readyInMaintenance)).assertSuccess();
            new Administration(client).reload();
        }
    }

    /**
     * Setup a microprofile-config-smallrye subsystem to obtain values from properties defined in the subsystem
     */
    public static class SetupTask implements MicroProfileServerSetupTask {
        @Override
        public void setup() throws Exception {
            try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
                client.execute("/subsystem=microprofile-config-smallrye/config-source=props:add").assertSuccess();
            }
        }

        @Override
        public void tearDown() throws Exception {
            try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
                client.execute("/subsystem=microprofile-config-smallrye/config-source=props:remove").assertSuccess();
            }
        }
    }
}
