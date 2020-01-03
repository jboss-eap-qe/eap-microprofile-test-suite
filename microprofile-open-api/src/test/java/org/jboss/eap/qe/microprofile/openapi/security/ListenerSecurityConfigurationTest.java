package org.jboss.eap.qe.microprofile.openapi.security;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.not;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeoutException;

import javax.ws.rs.core.MediaType;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.eap.qe.microprofile.openapi.OpenApiServerConfiguration;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.ProviderApplication;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.RoutingServiceConstants;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.api.DistrictService;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.data.DistrictEntity;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.model.District;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.rest.DistrictsResource;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.services.InMemoryDistrictService;
import org.jboss.eap.qe.microprofile.openapi.model.OpenApiFilter;
import org.jboss.eap.qe.microprofile.openapi.model.OpenApiModelReader;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.ArquillianContainerProperties;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.ArquillianDescriptorWrapper;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.jboss.eap.qe.microprofile.tooling.server.log.ModelNodeLogChecker;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.core.online.CliException;
import org.wildfly.extras.creaper.core.online.ModelNodeResult;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.operations.Address;
import org.wildfly.extras.creaper.core.online.operations.Batch;
import org.wildfly.extras.creaper.core.online.operations.Operations;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;

/**
 * Test cases for security scenarios involving HTTP/HTTPS listeners
 */
@RunWith(Arquillian.class)
@ServerSetup({ ListenerSecurityConfigurationTest.OpenApiExtensionSetup.class })
@RunAsClient
public class ListenerSecurityConfigurationTest {

    private final static String DEPLOYMENT_NAME = ListenerSecurityConfigurationTest.class.getSimpleName();

    private static ArquillianContainerProperties arquillianContainerProperties = new ArquillianContainerProperties(
            ArquillianDescriptorWrapper.getArquillianDescriptor());
    private static int configuredHTTPPort, configuredHTTPSPort;
    private static OnlineManagementClient onlineManagementClient;
    private static Path keyStoreFile;
    private static String jbossHome;

    @Deployment(testable = false)
    public static WebArchive createServicesProviderDeployment() {
        WebArchive deployment = ShrinkWrap.create(
                WebArchive.class,
                String.format("%s.war", DEPLOYMENT_NAME))
                .addClasses(
                        ProviderApplication.class,
                        District.class,
                        DistrictEntity.class,
                        DistrictService.class,
                        InMemoryDistrictService.class,
                        DistrictsResource.class,
                        RoutingServiceConstants.class,
                        OpenApiModelReader.class,
                        OpenApiFilter.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return deployment;
    }

    static class OpenApiExtensionSetup implements ServerSetupTask {

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            jbossHome = arquillianContainerProperties.getContainerProperty("jboss", "jbossHome");
            onlineManagementClient = ManagementClientProvider.onlineStandalone();
            //  configured server ports for HTTP and HTTPS bindings, offset is taken into account
            configuredHTTPPort = OpenApiServerConfiguration.getHTTPPort(onlineManagementClient);
            configuredHTTPSPort = OpenApiServerConfiguration.getHTTPSPort(onlineManagementClient);
            //  MP OpenAPI up
            OpenApiServerConfiguration.enableOpenApi(onlineManagementClient);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            //  MP OpenAPI down
            try {
                OpenApiServerConfiguration.disableOpenApi(onlineManagementClient);
            } finally {
                onlineManagementClient.close();
            }
        }
    }

    private static void enableHTTPSListener()
            throws IOException, TimeoutException, InterruptedException, CliException {

        String keystorePath = jbossHome + "/keystore.jks";

        ModelNodeResult result = onlineManagementClient.execute(
                String.format(
                        "/subsystem=elytron/key-store=httpsGenKS:add(path=%s,credential-reference={clear-text=secret},type=JKS",
                        keystorePath));
        result.assertSuccess();

        result = onlineManagementClient.execute(
                "/subsystem=elytron/key-store=httpsGenKS:generate-key-pair(" +
                        "alias=localhost,algorithm=RSA,key-size=1024,validity=365," +
                        "credential-reference={clear-text=secret},distinguished-name=\"CN=localhost\")");
        result.assertSuccess();

        result = onlineManagementClient.execute("/subsystem=elytron/key-store=httpsGenKS:store()");
        result.assertSuccess();

        // keep a reference to keystore.jks in order to flush it away once we're done
        if (!Files.exists(Paths.get(keystorePath))) {
            throw new IllegalStateException(keystorePath + " not found");
        }
        keyStoreFile = Paths.get(keystorePath);

        result = onlineManagementClient.execute(
                "/subsystem=elytron/key-manager=httpsKM:add(key-store=httpsGenKS,credential-reference={clear-text=secret})");
        result.assertSuccess();

        result = onlineManagementClient.execute(
                "/subsystem=elytron/server-ssl-context=httpsSSC:add(key-manager=httpsKM,protocols=[\"TLSv1.2\"])");
        result.assertSuccess();

        result = onlineManagementClient.execute(
                "/subsystem=undertow/server=default-server/https-listener=https:read-attribute(name=security-realm)\n");
        if (result.isSuccess()) {
            Operations operations = new Operations(onlineManagementClient);
            Address undertowAddress = Address.subsystem("undertow").and("server", "default-server").and("https-listener",
                    "https");
            Batch batch = new Batch();
            batch.undefineAttribute(undertowAddress, "security-realm");
            batch.writeAttribute(undertowAddress, "ssl-context", "httpsSSC");
            result = operations.batch(batch);
            result.assertSuccess();
        }
        new Administration(onlineManagementClient).reload();
    }

    private static void disableHTTPSListener() throws InterruptedException, TimeoutException, IOException, CliException {
        Operations operations = new Operations(onlineManagementClient);
        Address undertowAddress = Address.subsystem("undertow").and("server", "default-server").and("https-listener",
                "https");
        Batch batch = new Batch();
        batch.undefineAttribute(undertowAddress, "ssl-context");
        batch.writeAttribute(undertowAddress, "security-realm", "ApplicationRealm");
        ModelNodeResult result = operations.batch(batch);
        result.assertSuccess();

        result = onlineManagementClient.execute("/subsystem=elytron/server-ssl-context=httpsSSC:remove()");
        result.assertSuccess();

        result = onlineManagementClient.execute(
                "/subsystem=elytron/key-manager=httpsKM:remove()");
        result.assertSuccess();

        result = onlineManagementClient.execute(
                "/subsystem=elytron/key-store=httpsGenKS:remove()");
        result.assertSuccess();

        new Administration(onlineManagementClient).reload();

        Files.delete(keyStoreFile);
    }

    private static void disableHTTPListener() throws InterruptedException, TimeoutException, IOException, CliException {
        ModelNodeResult result = onlineManagementClient.execute(
                "/subsystem=undertow/server=default-server/http-listener=default:remove");
        result.assertSuccess();

        result = onlineManagementClient.execute(
                "/subsystem=remoting/http-connector=http-remoting-connector:write-attribute(name=connector-ref, value=https)");
        result.assertSuccess();

        new Administration(onlineManagementClient).reload();
    }

    private static void enableHTTPListener() throws InterruptedException, TimeoutException, IOException, CliException {
        ModelNodeResult result = onlineManagementClient.execute(
                "/subsystem=undertow/server=default-server/http-listener=default:add(" +
                        "socket-binding=http,redirect-socket=https,enable-http2=true)");
        result.assertSuccess();

        result = onlineManagementClient.execute(
                "/subsystem=remoting/http-connector=http-remoting-connector:write-attribute(name=connector-ref, value=default)");
        result.assertSuccess();

        new Administration(onlineManagementClient).reload();
    }

    /**
     * @tpTestDetails Test to verify that the {@code openapi} endpoint is available via HTTPS once the listener
     *                has been properly configured
     * @tpPassCrit The {@code openapi} endpoint can be reached via [https] schema at related standard port number
     *             (8443), response status is HTTP 200 and response body is not empty
     *
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @InSequence(1)
    public void testOpenApiOnHTTPSListener(@ArquillianResource URL url)
            throws InterruptedException, TimeoutException, CliException, IOException {
        String httpsOpenApiUrl = String.format(
                "https://%s:%d/openapi",
                url.getHost(),
                url.getPort() - configuredHTTPPort + configuredHTTPSPort);

        //  enable HTTPS
        enableHTTPSListener();
        //  ... and disable HTTP
        disableHTTPListener();

        given().relaxedHTTPSValidation().get(httpsOpenApiUrl + "?format=JSON")
                .then()
                .statusCode(200)
                .contentType(equalToIgnoringCase(MediaType.APPLICATION_JSON))
                .body(not(empty()));

        //  disable HTTPS
        disableHTTPSListener();
        //  ... and re-enable HTTP
        enableHTTPListener();

    }

    /**
     * @tpTestDetails Test to verify that activating MP OpenAPI when the HTTP (default) listener is disabled
     *                causes the server to log a warning about MicroProfile OpenAPI specification requiring for the
     *                endpoint to be accessible via [http]
     * @tpPassCrit The server is logging expected {@code WFLYMPOAI0006} message because HTTP (default) listener
     *             does not exist
     *
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @InSequence(2)
    public void testMissingHTTPListenerWarningLogged() {
        ModelNodeLogChecker modelNodeLogChecker = new ModelNodeLogChecker(onlineManagementClient, 200);
        Assert.assertTrue(modelNodeLogChecker.logContains("WFLYMPOAI0006"));
    }
}
