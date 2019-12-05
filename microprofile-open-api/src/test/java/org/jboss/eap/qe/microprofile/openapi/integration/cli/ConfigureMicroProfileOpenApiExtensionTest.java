package org.jboss.eap.qe.microprofile.openapi.integration.cli;

import java.io.IOException;

import org.jboss.arquillian.junit.Arquillian;
import org.jboss.eap.qe.microprofile.openapi.OpenApiServerConfiguration;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.ConfigurationException;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.ArquillianContainerProperties;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.ArquillianDescriptorWrapper;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientHelper;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientRelatedException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;

/**
 * Test cases for MP OpenAPI feature subsystem configuration
 */
@RunWith(Arquillian.class)
public class ConfigureMicroProfileOpenApiExtensionTest {

    static ArquillianContainerProperties arquillianContainerProperties = new ArquillianContainerProperties(
            ArquillianDescriptorWrapper.getArquillianDescriptor());

    /**
     * @tpTestDetails Testing CLI operations to configure subsystem (add and removal)
     * @tpPassCrit Uses {@link ManagementClientHelper} API to verify MP OpenAPI feature subsystem does not exist and
     *             then executes commands needed for its initialization and finalization.
     * @tpSince EAP 7.4.0.CD19
     *
     * @throws ManagementClientRelatedException Wraps exceptions thrown by the internal operation executed by
     *         {@link OnlineManagementClient} API
     * @throws IOException
     * @throws ConfigurationException Wraps exceptions thrown by the internal operation executed by
     *         {@link ArquillianContainerProperties} API
     */
    @Test
    public void testAddAndRemoveExtensionAndSubsystem()
            throws ManagementClientRelatedException, IOException, ConfigurationException {
        //  MP OpenAPI up & down
        try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone(arquillianContainerProperties)) {
            if (!OpenApiServerConfiguration.openapiSubsystemExists(client)) {
                OpenApiServerConfiguration.enableOpenApi(client);
                OpenApiServerConfiguration.disableOpenApi(client);
            }
        }
    }
}
