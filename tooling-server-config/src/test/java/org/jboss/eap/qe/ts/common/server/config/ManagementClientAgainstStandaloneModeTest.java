package org.jboss.eap.qe.ts.common.server.config;

import org.junit.ClassRule;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.wildfly.extras.creaper.core.CommandFailedException;
import org.wildfly.extras.creaper.core.online.CliException;
import org.wildfly.extras.creaper.core.online.ModelNodeResult;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;

/**
 * Test case to verify {@link BaseManagementClientFactory} features - together with Creaper implementations for both
 * on-line and off-line management clients - against Wildfly running in standalone mode.
 */
public class ManagementClientAgainstStandaloneModeTest extends BaseManagementClientTestCase {
    private static final String WILDFLY_NAME = "standalone-wildfly";

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
        return "standalone.xml";
    }

    @ClassRule
    public static ExternalizedManagemntClientResource<StandaloneManagementClientFactory> managementClientFactoryResource =
            new ExternalizedManagemntClientResource<>(
                    new StandaloneManagementClientFactory.Builder()
                        .host(WILDFLY_DEFAULT_BIND_ADDRESS)
                        .port(9990)
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
    public void testOnlineManagementClientAddAndRemoveOpenApiExtensionAndSubsystem() throws IOException, CliException {
        OnlineManagementClient client = getManagementClientFactory().createOnline();
        ModelNodeResult result = client.execute(
                "/extension=org.wildfly.extension.microprofile.openapi-smallrye:add");
        result.assertSuccess();
        result = client.execute(
                "/:composite(steps=[{\"operation\" => \"add\",\"address\" => [(\"subsystem\" => \"microprofile-openapi-smallrye\")]}])");
        result.assertSuccess();
        result = client.execute(
                "/:composite(steps=[{\"operation\" => \"remove\",\"address\" => [(\"subsystem\" => \"microprofile-openapi-smallrye\")]}])");
        result.assertSuccess();
        result = client.execute(
                "/extension=org.wildfly.extension.microprofile.openapi-smallrye:remove");
        result.assertSuccess();
    }

    @Test
    public void testOnlineManagementClientAddingAndRemovingDatasource() throws IOException, CommandFailedException, CliException {
        doTestOnlineManagementClientAddingAndRemovingDatasource();
    }

    @Test
    public void testOnlineManagementClientListLogFiles() throws IOException, CliException {
        OnlineManagementClient client = getManagementClientFactory().createOnline();
        ModelNodeResult result = client.execute("/subsystem=logging:list-log-files");
        result.assertSuccess();
        System.out.println(result.get("result"));
    }

    @Test
    public void testOfflineManagementClientAvailable() throws IOException {
        doTestOfflineManagementClientAvailable(getWildflyRootDirectory(), getWildflyConfigurationFile());
    }
}