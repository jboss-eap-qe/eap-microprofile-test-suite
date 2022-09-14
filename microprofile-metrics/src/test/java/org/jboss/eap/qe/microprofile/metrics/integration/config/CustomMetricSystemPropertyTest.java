package org.jboss.eap.qe.microprofile.metrics.integration.config;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.controller.PathAddress;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.MicroProfileServerSetupTask;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.deployment.ConfigurationUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;

/**
 * MP Config property are defined as system properties.
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(CustomMetricSystemPropertyTest.SetupTask.class)
public class CustomMetricSystemPropertyTest extends CustomMetricBaseTest {

    private static final PathAddress SYSTEM_PROPERTY_ADDRESS = PathAddress.pathAddress()
            .append(SYSTEM_PROPERTY, INCREMENT_CONFIG_PROPERTY);

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        WebArchive webArchive = ShrinkWrap
                .create(WebArchive.class, CustomMetricSystemPropertyTest.class.getSimpleName() + ".war")
                .addClasses(CustomCounterIncrementProvider.class, CustomCounterMetric.class, CustomMetricService.class,
                        CustomMetricApplication.class, CustomMetricAppInitializer.class)
                .addAsWebInfResource(ConfigurationUtil.BEANS_XML_FILE_LOCATION, "beans.xml");
        return webArchive;
    }

    void setConfigProperties(int increment) throws Exception {
        try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {

            client.execute(String.format("/system-property=%s:write-attribute(name=value, value=%s)",
                    INCREMENT_CONFIG_PROPERTY, increment))
                    .assertSuccess();
            new Administration(client).reload();
        }
    }

    /**
     * Add system properties for MP Config
     */
    static class SetupTask implements MicroProfileServerSetupTask {

        @Override
        public void setup() throws Exception {
            try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
                client.execute(String.format("/system-property=%s:add", INCREMENT_CONFIG_PROPERTY)).assertSuccess();
            }
        }

        @Override
        public void tearDown() throws Exception {
            try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
                client.execute(String.format("/system-property=%s:remove", INCREMENT_CONFIG_PROPERTY)).assertSuccess();
            }
        }
    }
}
