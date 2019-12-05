package org.jboss.eap.qe.microprofile.tooling.server.logwatch;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.ConfigurationException;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

/**
 * Set of tests for {@link ModelNodeLogReader} tool.
 */
@RunWith(Arquillian.class)
public class ModelNodeLogReaderTestCase {

    @Test
    @RunAsClient
    public void testLineMatchedClient() throws ConfigurationException, IOException, TimeoutException, InterruptedException {
        try (final OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
            final LogReader logReader = new ModelNodeLogReader(client, 10, true);

            new Administration(client).reload();

            Assert.assertTrue(logReader.wasLineLogged(Pattern.compile(".*WFLYSRV0025.*")));
        }
    }

    @Test
    @RunAsClient
    public void testLineNotMatchedClient() throws ConfigurationException, IOException {
        try (final OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
            final LogReader logReader = new ModelNodeLogReader(client, 10, true);

            Assert.assertFalse(logReader.wasLineLogged(Pattern.compile(".*Foooqux 42.*")));
        }
    }

}
