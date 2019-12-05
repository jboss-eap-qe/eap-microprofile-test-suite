package org.jboss.eap.qe.microprofile.metrics.integration.config;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.ConfigurationException;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.runner.RunWith;
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
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return webArchive;
    }

    void setConfigProperties(int increment) throws IOException, ConfigurationException, TimeoutException, InterruptedException {
        Assert.assertEquals(ClientConstants.SUCCESS, managementClient.getControllerClient()
                .execute(Util.getWriteAttributeOperation(SYSTEM_PROPERTY_ADDRESS, VALUE, increment))
                .get(ClientConstants.OUTCOME)
                .asString());
        new Administration(ManagementClientProvider.onlineStandalone()).reload();
    }

    /**
     * Add system properties for MP Config
     */
    public static class SetupTask implements ServerSetupTask {

        @Override
        public void setup(ManagementClient managementClient, String s) throws Exception {
            Assert.assertEquals(ClientConstants.SUCCESS, managementClient
                    .getControllerClient()
                    .execute(Util.createAddOperation(SYSTEM_PROPERTY_ADDRESS))
                    .get(ClientConstants.OUTCOME)
                    .asString());
        }

        @Override
        public void tearDown(ManagementClient managementClient, String s) throws Exception {
            Assert.assertEquals(ClientConstants.SUCCESS, managementClient
                    .getControllerClient()
                    .execute(Util.createRemoveOperation(SYSTEM_PROPERTY_ADDRESS))
                    .get(ClientConstants.OUTCOME)
                    .asString());
        }
    }
}
