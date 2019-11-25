package org.jboss.eap.qe.ts.common.server.config;

import org.junit.ClassRule;
import org.junit.Test;
import org.wildfly.extras.creaper.core.CommandFailedException;
import org.wildfly.extras.creaper.core.online.CliException;
import org.wildfly.extras.creaper.core.online.ModelNodeResult;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.operations.Address;
import org.wildfly.extras.creaper.core.online.operations.Operations;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Test case to verify {@link BaseManagementClientFactory} features - together with Creaper implementations for both
 * on-line and off-line management clients - against Wildfly running in standalone mode.
 */
public class ManagementClientAgainstDomainModeTest extends BaseManagementClientTestCase {

    private static final String WILDFLY_NAME = "domain-wildfly";

    @Override
    protected String getWildflyName() {
        return WILDFLY_NAME;
    }

    @Override
    protected ManagementClientFactory getManagementClientFactory() {
        return managementClientFactoryResource.unwrap();
    }

    @Override
    protected String getWildflyConfigurationFile()  {
        return "domain.xml";
    }

    @ClassRule
    public static ExternalizedManagemntClientResource<DomainManagementClientFactory> managementClientFactoryResource =
        new ExternalizedManagemntClientResource<>(
                new DomainManagementClientFactory.Builder()
                        .host(WILDFLY_DEFAULT_BIND_ADDRESS)
                        .port(9990)
                        .defaultProfile("default")
                        .defaultHost("master")
                        .build(WILDFLY_NAME)
        );

    @Test
    public void testManagementPortMapping() {
        doTestManagementPortMapping();
    }

    @Test
    public void testWelcomePageAvailable() {
        doTestWelcomePageAvailable();
    }

    @Test
    public void testOnlineManagementClientAvailable() throws IOException {
        doTestOnlineManagementClientAvailable();
    }

    @Test
    public void testOnlineManagementClientReturnsWhoami() throws IOException, CliException {
        doTestOnlineManagementClientReturnsWhoami();
    }

    @Test
    public void testOnlineAdministrationManagementClientReload() throws IOException, InterruptedException, TimeoutException {
        doTestOnlineAdministrationManagementClientReload();
    }

    @Test
    public void testOnlineManagementClientAddAndRemoveOpenApiExtensionAndSubsystem() throws IOException {
        OnlineManagementClient client = getManagementClientFactory().createOnline();
        Operations ops = new Operations(client);
        ModelNodeResult result = ops.add(Address.root()
                .and("extension", "org.wildfly.extension.microprofile.openapi-smallrye"));
        result.assertSuccess();
        result = ops.remove(Address.root().and("extension", "org.wildfly.extension.microprofile.openapi-smallrye"));
        result.assertSuccess();
    }

    @Test
    public void testOnlineManagementClientAddingAndRemovingDatasource() throws IOException, CommandFailedException, CliException {
        doTestOnlineManagementClientAddingAndRemovingDatasource();
    }

    @Test
    public void testOfflineManagementClientAvailable() throws IOException {
        doTestOfflineManagementClientAvailable(getWildflyRootDirectory(), getWildflyConfigurationFile());
    }
}