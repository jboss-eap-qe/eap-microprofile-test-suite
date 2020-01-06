package org.jboss.eap.qe.microprofile.health.integration;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;

/**
 * MP Config property are defined as system properties.
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({ MicroProfileFTSetupTask.class, FailSafeCDISystemPropertyHealthTest.SetupTask.class })
public class FailSafeCDISystemPropertyHealthTest extends FailSafeCDIHealthBaseTest {

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        WebArchive webArchive = ShrinkWrap
                .create(WebArchive.class, FailSafeCDISystemPropertyHealthTest.class.getSimpleName() + ".war")
                .addClasses(FailSafeDummyService.class, CDIBasedLivenessHealthCheck.class, CDIBasedReadinessHealthCheck.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return webArchive;
    }

    @Override
    protected void setConfigProperties(boolean live, boolean ready, boolean inMaintanance, boolean readyInMainenance)
            throws Exception {
        try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
            client.execute(String.format("/system-property=%s:write-attribute(name=value, value=%s)",
                    FailSafeDummyService.LIVE_CONFIG_PROPERTY, live)).assertSuccess();
            client.execute(String.format("/system-property=%s:write-attribute(name=value, value=%s)",
                    FailSafeDummyService.READY_CONFIG_PROPERTY, ready)).assertSuccess();
            client.execute(String.format("/system-property=%s:write-attribute(name=value, value=%s)",
                    FailSafeDummyService.IN_MAINTENANCE_CONFIG_PROPERTY, inMaintanance)).assertSuccess();
            client.execute(String.format("/system-property=%s:write-attribute(name=value, value=%s)",
                    FailSafeDummyService.READY_IN_MAINTENANCE_CONFIG_PROPERTY, readyInMainenance)).assertSuccess();
            new Administration(client).reload();
        }
    }

    /**
     * Add system properties for MP Config
     */
    public static class SetupTask implements ServerSetupTask {

        @Override
        public void setup(ManagementClient managementClient, String s) throws Exception {
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
        public void tearDown(ManagementClient managementClient, String s) throws Exception {
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
