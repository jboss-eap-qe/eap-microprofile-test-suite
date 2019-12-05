package org.jboss.eap.qe.microprofile.metrics.integration.config;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

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
import org.jboss.dmr.ModelNode;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.ConfigurationException;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;

/**
 * MP Config property is provided by props model option - values are stored in model object.
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(CustomMetricModelPropsTest.SetupTask.class)
public class CustomMetricModelPropsTest extends CustomMetricBaseTest {

    private static final PathAddress CONFIG_SOURCE_PROPS_ADDRESS = PathAddress.pathAddress()
            .append(SUBSYSTEM, "microprofile-config-smallrye")
            .append("config-source", "props");

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        WebArchive webArchive = ShrinkWrap.create(WebArchive.class, CustomMetricModelPropsTest.class.getSimpleName() + ".war")
                .addClasses(CustomCounterIncrementProvider.class, CustomCounterMetric.class, CustomMetricService.class,
                        CustomMetricApplication.class, CustomMetricAppInitializer.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return webArchive;
    }

    void setConfigProperties(int increment) throws IOException, ConfigurationException, TimeoutException, InterruptedException {
        ModelNode properties = new ModelNode().add(INCREMENT_CONFIG_PROPERTY, increment);
        Assert.assertEquals(ClientConstants.SUCCESS, managementClient.getControllerClient()
                .execute(Util.getWriteAttributeOperation(CONFIG_SOURCE_PROPS_ADDRESS, "properties", properties))
                .get(ClientConstants.OUTCOME)
                .asString());
        new Administration(ManagementClientProvider.onlineStandalone()).reload();
    }

    /**
     * Setup a microprofile-config-smallrye subsystem to obtain values from properties defined in the subsystem.
     */
    public static class SetupTask implements ServerSetupTask {

        @Override
        public void setup(ManagementClient managementClient, String s) throws Exception {
            Assert.assertEquals(ClientConstants.SUCCESS, managementClient
                    .getControllerClient()
                    .execute(Util.createAddOperation(CONFIG_SOURCE_PROPS_ADDRESS))
                    .get(ClientConstants.OUTCOME)
                    .asString());
        }

        @Override
        public void tearDown(ManagementClient managementClient, String s) throws Exception {
            Assert.assertEquals(ClientConstants.SUCCESS, managementClient
                    .getControllerClient()
                    .execute(Util.createRemoveOperation(CONFIG_SOURCE_PROPS_ADDRESS))
                    .get(ClientConstants.OUTCOME)
                    .asString());
        }
    }
}
