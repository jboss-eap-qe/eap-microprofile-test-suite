package org.jboss.eap.qe.microprofile.tooling.server.log;

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
 * Set of tests for {@link ModelNodeLogChecker} tool.
 */
@RunWith(Arquillian.class)
public class ModelNodeLogCheckerTestCase {

    @Test
    @RunAsClient
    public void testLineMatchedPatternClient() throws ConfigurationException, IOException, TimeoutException, InterruptedException {
        try (final OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
            final LogChecker logChecker = new ModelNodeLogChecker(client, 10, true);

            new Administration(client).reload();

            Assert.assertTrue(logChecker.logMatches(Pattern.compile(".*WFLYSRV0025.*")));
        }
    }

    @Test
    @RunAsClient
    public void testLineNotMatchedPatternClient() throws ConfigurationException, IOException {
        try (final OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
            final LogChecker logChecker = new ModelNodeLogChecker(client, 10, true);

            Assert.assertFalse(logChecker.logMatches(Pattern.compile(".*Foooqux 42.*")));
        }
    }

    @Test
    @RunAsClient
    public void testLineContainedSubstringClient() throws ConfigurationException, IOException, TimeoutException, InterruptedException {
        try (final OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
            final LogChecker logChecker = new ModelNodeLogChecker(client, 10, true);

            new Administration(client).reload();

            Assert.assertTrue(logChecker.logContains("WFLYSRV0025"));
        }
    }

    @Test
    @RunAsClient
    public void testLineNotContainedSubstringClient() throws ConfigurationException, IOException {
        try (final OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
            final LogChecker logChecker = new ModelNodeLogChecker(client, 10, true);

            Assert.assertFalse(logChecker.logContains("Foooqux 42"));
        }
    }

}
