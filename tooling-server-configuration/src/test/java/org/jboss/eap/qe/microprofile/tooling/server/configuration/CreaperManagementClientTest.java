package org.jboss.eap.qe.microprofile.tooling.server.configuration;

import org.jboss.arquillian.junit.Arquillian;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.ArquillianContainerProperties;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.ArquillianDescriptorWrapper;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Tests Creaper integration for CLI operations.
 * <p>
 * {@link ManagementClientProvider} uses {@link ArquillianContainerProperties} to retrieve default configuration from Arquillian context
 */
@RunWith(Arquillian.class)
public class CreaperManagementClientTest {

    static ArquillianContainerProperties arquillianContainerProperties = new ArquillianContainerProperties(ArquillianDescriptorWrapper.getArquillianDescriptor());

    @Test
    public void testReloadCommand() throws IOException, ConfigurationException, TimeoutException, InterruptedException {
        try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone(arquillianContainerProperties)) {
            Administration admin = new Administration(client);
            admin.reload();
        }
    }
}
