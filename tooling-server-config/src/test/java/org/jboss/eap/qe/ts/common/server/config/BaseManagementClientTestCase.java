package org.jboss.eap.qe.ts.common.server.config;

import org.hamcrest.Matchers;
import org.wildfly.extras.creaper.core.CommandFailedException;
import org.wildfly.extras.creaper.core.online.CliException;
import org.wildfly.extras.creaper.core.online.ModelNodeResult;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.TimeoutException;

import static io.restassured.RestAssured.get;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

/**
 * Base implementation for management client test cases
 */
public abstract class BaseManagementClientTestCase {

    public static final String WILDFLY_DEFAULT_BIND_ADDRESS = "127.0.0.1";
    public static final int WILDFLY_DEFAULT_EXPOSED_HTTP_PORT = 8080;
    public static final int WILDFLY_DEFAULT_EXPOSED_MANAGEMENT_PORT = 9990;

    protected static boolean portOpened(int port) {
        try {
            new Socket(WILDFLY_DEFAULT_BIND_ADDRESS, port).close();
        } catch (Exception ex) {
            return false;
        }
        return true;
    }

    protected abstract String getWildflyName();

    protected abstract ManagementClientFactory getManagementClientFactory();

    protected String getWildflyRootDirectory() {
        return System.getProperty("jboss.home");
    }

    protected abstract String getWildflyConfigurationFile();

    protected void doTestManagementPortMapping() {
        assertThat(getWildflyName() + " container is not listening on port " + WILDFLY_DEFAULT_EXPOSED_MANAGEMENT_PORT,
                portOpened(WILDFLY_DEFAULT_EXPOSED_MANAGEMENT_PORT), is(true));
    }

    protected void doTestWelcomePageAvailable() {
        get("http://" + WILDFLY_DEFAULT_BIND_ADDRESS + ":" + WILDFLY_DEFAULT_EXPOSED_HTTP_PORT).then()
                .body(containsString("Welcome to WildFly"));
    }

    protected void doTestOnlineManagementClientAvailable() throws IOException {
        assertThat(getWildflyName() + " management client is not available ",
                getManagementClientFactory().createOnline(), Matchers.notNullValue());
    }

    protected void doTestOnlineManagementClientReturnsWhoami() throws IOException, CliException {
        OnlineManagementClient client = getManagementClientFactory().createOnline();
        ModelNodeResult result = client.execute(":whoami");
        result.assertSuccess();
        System.out.println(result.get("result", "identity", "username"));
    }

    protected void doTestOnlineAdministrationManagementClientReload() throws IOException, InterruptedException, TimeoutException {
        OnlineManagementClient client = getManagementClientFactory().createOnline();
        Administration admin = new Administration(client);
        admin.reload();
    }

    public void doTestOnlineManagementClientAddingAndRemovingDatasource() throws IOException, CommandFailedException, CliException {
        OnlineManagementClient client = getManagementClientFactory().createOnline();
        client.apply(new SetupDatasource());
        //
        ModelNodeResult result = client.execute("/subsystem=datasources:read-resource");
        result.assertSuccess();
        System.out.println(result.get("result"));
        //
        client.apply(new RemoveDatasource());
    }

    protected void doTestOfflineManagementClientAvailable(String rootDirectory, String configurationFile) throws IOException {
        assertThat(getWildflyName() + " offline management client is not available ",
                getManagementClientFactory().createOffline(rootDirectory, configurationFile), Matchers.notNullValue());
    }
}