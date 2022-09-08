package org.jboss.eap.qe.microprofile.metrics.integration.config;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.MicroProfileServerSetupTask;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.deployment.ConfigurationUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;

/**
 * MP Config property is provided by props model option - values are stored in model object.
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(CustomMetricModelPropsTest.SetupTask.class)
public class CustomMetricModelPropsTest extends CustomMetricBaseTest {

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        WebArchive webArchive = ShrinkWrap.create(WebArchive.class, CustomMetricModelPropsTest.class.getSimpleName() + ".war")
                .addClasses(CustomCounterIncrementProvider.class, CustomCounterMetric.class, CustomMetricService.class,
                        CustomMetricApplication.class, CustomMetricAppInitializer.class)
                .addAsWebInfResource(new StringAsset(ConfigurationUtil.BEANS_XML_FILE_CONTENT), "beans.xml");
        return webArchive;
    }

    void setConfigProperties(int increment) throws Exception {
        try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
            client.execute(String.format(
                    "/subsystem=microprofile-config-smallrye/config-source=props:write-attribute(name=properties, value={%s=%s}",
                    INCREMENT_CONFIG_PROPERTY, increment))
                    .assertSuccess();
            new Administration(client).reload();
        }
    }

    /**
     * Setup a microprofile-config-smallrye subsystem to obtain values from properties defined in the subsystem.
     */
    static class SetupTask implements MicroProfileServerSetupTask {

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
