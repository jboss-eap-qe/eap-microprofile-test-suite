package org.jboss.eap.qe.microprofile.openapi.advanced;

import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.not;

import jakarta.ws.rs.core.MediaType;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.stream.Stream;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.eap.qe.microprofile.openapi.OpenApiServerConfiguration;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.ProviderApplication;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.RoutingServiceConstants;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.api.DistrictService;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.data.DistrictEntity;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.model.District;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.rest.DistrictsResource;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.services.InMemoryDistrictService;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.router.RouterApplication;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.router.model.DistrictObject;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.router.rest.LocalServiceRouterInfoResource;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.router.rest.routed.RouterDistrictsResource;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.router.services.DistrictServiceClient;
import org.jboss.eap.qe.microprofile.openapi.model.AnotherOpenApiFilter;
import org.jboss.eap.qe.microprofile.openapi.model.AnotherOpenApiModelReader;
import org.jboss.eap.qe.microprofile.openapi.model.OpenApiFilter;
import org.jboss.eap.qe.microprofile.openapi.model.OpenApiModelReader;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.ConfigurationException;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.ArquillianContainerProperties;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.ArquillianDescriptorWrapper;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.MicroProfileServerSetupTask;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.deployment.ConfigurationUtil;
import org.jboss.eap.qe.microprofile.tooling.server.log.LogChecker;
import org.jboss.eap.qe.microprofile.tooling.server.log.ModelNodeLogChecker;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;

/**
 * Specific test cases validating the behavior when multiple WAR deployments are present.
 * <p>
 * This test uses the same scenario implemented in other test classes, and specifically simulates one case in which
 * a Service Provider application is deployed to a dedicated server virtual host.
 * Additionally, two Local Service Router applications are deployed, and use a custom OASFilter to modify "global"
 * properties and make them discordant within the same server virtual host generated OpenAPI documentation.
 * </p>
 */
@RunWith(Arquillian.class)
@ServerSetup({ MultipleAndDiscordantLocalServiceRouterWarsTest.OpenApiExtensionSetup.class })
@RunAsClient
public class MultipleAndDiscordantLocalServiceRouterWarsTest {

    private static final String PROVIDER_DEPLOYMENT_NAME = "serviceProviderDeployment";
    private static final String SERVICE_PROVIDER_HOST = "service-provider";
    private static final int SERVICE_PROVIDER_PORT = 8081;
    private static final String ROUTER_DEPLOYMENT_NAME = "localServicesRouterDeployment";
    private static final String ANOTHER_ROUTER_DEPLOYMENT_NAME = "anotherLocalServicesRouterDeployment";
    private static final String CONFIGURATION_TEMPLATE = "mp.openapi.scan.exclude.packages=org.jboss.eap.qe.microprofile.openapi.apps.routing.router.rest.routed"
            + "\n" +
            "mp.openapi.model.reader=org.jboss.eap.qe.microprofile.openapi.model.%s"
            + "\n" +
            "mp.openapi.filter=org.jboss.eap.qe.microprofile.openapi.model.%s"
            + "\n" +
            "mp.openapi.scan.disable=false"
            + "\n" +
            "services.provider.host=%s"
            + "\n" +
            "services.provider.port=%d";

    private final static ArquillianContainerProperties ARQUILLIAN_CONTAINER_PROPERTIES = new ArquillianContainerProperties(
            ArquillianDescriptorWrapper.getArquillianDescriptor());

    /**
     * Builds a deployment archive with enabled MP OpenAPI annotations scan to implement a complex scenario in
     * which the Service Provider is deployed on a dedicated Undertow server/virtual host, while the OpenAPI
     * documentation for each Local Service Router app should be generated starting from the static
     * file provided as a deliverable by the Service Provider staff, and modified by specific
     * {@link org.eclipse.microprofile.openapi.OASFilter} and {@link org.eclipse.microprofile.openapi.OASModelReader}
     * instances.
     * <p>
     * In this scenario the Service Provider app is deployed to a dedicated virtual host, therefore the relevant
     * {@code /openapi} endpoint should output the OpenAPI documentation which is based on the Service Provider
     * application MicroProfile OpenAPI annotations.
     * The Service Provider app still works as a backend for the Local Service Router(s) REST calls.
     * </p>
     *
     * @return {@link WebArchive} instance for the Service provider app deployment
     */
    @Deployment(name = PROVIDER_DEPLOYMENT_NAME, order = 1, testable = false)
    static Archive<?> serviceProviderDeployment() {
        return ShrinkWrap.create(
                WebArchive.class, PROVIDER_DEPLOYMENT_NAME + ".war")
                .addClasses(
                        ProviderApplication.class,
                        District.class,
                        DistrictEntity.class,
                        DistrictService.class,
                        InMemoryDistrictService.class,
                        DistrictsResource.class,
                        RoutingServiceConstants.class)
                .addAsManifestResource(new StringAsset("mp.openapi.scan.disable=false"), "microprofile-config.properties")
                .addAsWebInfResource(MultipleAndDiscordantLocalServiceRouterWarsTest.class.getClassLoader().getResource(
                        "WEB-INF/jboss-web-service-provider-host.xml"), "jboss-web.xml")
                .addAsWebInfResource(ConfigurationUtil.BEANS_XML_FILE_LOCATION, "beans.xml");
    }

    static String buildMicroProfileConfigProperties(final String openApiModelReader, final String openApiFilter)
            throws ConfigurationException {

        return String.format(CONFIGURATION_TEMPLATE,
                openApiModelReader,
                openApiFilter,
                // TODO - fixme: arq properties would return 127.0.0.1 which wouldn't work with undertow default host config
                "localhost",
                SERVICE_PROVIDER_PORT);
    }

    /**
     * A Local Service Router application.
     * <p>
     * MP Config is used to tell MP OpenAPI to skip doc generation for exposed "routed" JAX-RS endpoints as service
     * consumers must rely on the original documentation (META-INF/openapi.yaml), plus annotations from Local Service
     * Provider "non-routed" JAX-RS endpoints, edited through an OASModelReader implementation and eventually filtered
     * through a OASFilter one.
     * Here we also add config properties to reach the Services Provider, which is deployed to a dedicated virtual host.
     * </p>
     *
     * @return {@link Archive} instance that packages the first Local Service Router application deployment
     */
    @Deployment(name = ROUTER_DEPLOYMENT_NAME, order = 2, testable = false)
    static Archive<?> localServicesRouterDeployment()
            throws ConfigurationException {

        String mpConfigProperties = buildMicroProfileConfigProperties(
                OpenApiModelReader.class.getSimpleName(), OpenApiFilter.class.getSimpleName());

        return ShrinkWrap.create(
                WebArchive.class, ROUTER_DEPLOYMENT_NAME + ".war")
                .addClasses(
                        RouterApplication.class,
                        LocalServiceRouterInfoResource.class,
                        DistrictObject.class,
                        RouterDistrictsResource.class,
                        DistrictServiceClient.class,
                        OpenApiModelReader.class,
                        OpenApiFilter.class)
                .addAsManifestResource(new StringAsset(mpConfigProperties), "microprofile-config.properties")
                .addAsResource("META-INF/openapi.yaml")
                .addAsWebInfResource(ConfigurationUtil.BEANS_XML_FILE_LOCATION, "beans.xml");
    }

    /**
     * Another Local Service Router application.
     * <p>
     * The MP Config property are similar but two different {@link org.eclipse.microprofile.openapi.OASModelReader}
     * and {@link org.eclipse.microprofile.openapi.OASFilter} are used, in order to create discordant
     * values for global OpenAPI properties.
     * </p>
     *
     * @return {@link Archive} instance that packages the second Local Service Router application deployment
     */
    @Deployment(name = ANOTHER_ROUTER_DEPLOYMENT_NAME, order = 3, testable = false)
    public static Archive<?> anotherLocalServicesRouterDeployment()
            throws ConfigurationException {

        String mpConfigProperties = buildMicroProfileConfigProperties(
                AnotherOpenApiModelReader.class.getSimpleName(), AnotherOpenApiFilter.class.getSimpleName());

        return ShrinkWrap.create(
                WebArchive.class, ANOTHER_ROUTER_DEPLOYMENT_NAME + ".war")
                .addClasses(
                        RouterApplication.class,
                        LocalServiceRouterInfoResource.class,
                        DistrictObject.class,
                        RouterDistrictsResource.class,
                        DistrictServiceClient.class,
                        AnotherOpenApiModelReader.class,
                        AnotherOpenApiFilter.class)
                .addAsManifestResource(new StringAsset(mpConfigProperties), "microprofile-config.properties")
                .addAsResource("META-INF/openapi.yaml")
                .addAsWebInfResource(ConfigurationUtil.BEANS_XML_FILE_LOCATION, "beans.xml");
    }

    static class OpenApiExtensionSetup implements MicroProfileServerSetupTask {
        @Override
        public void setup() throws Exception {
            try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
                OpenApiServerConfiguration.enableOpenApi(client);
                // setup a dedicated virtual host for the service provider
                client.execute(
                        String.format("/socket-binding-group=standard-sockets/socket-binding=%s:add(port=%d",
                                SERVICE_PROVIDER_HOST, SERVICE_PROVIDER_PORT));
                client.execute(
                        String.format("/subsystem=undertow/server=%s:add", SERVICE_PROVIDER_HOST));
                client.execute(
                        String.format("/subsystem=undertow/server=%1$s/host=%1$s-host:add(alias=[localhost])",
                                SERVICE_PROVIDER_HOST));
                client.execute(
                        String.format("/subsystem=undertow/server=%1$s:write-attribute(default-host=%1$s-host)",
                                SERVICE_PROVIDER_HOST));
                client.execute(
                        String.format("/subsystem=undertow/server=%1$s/http-listener=%1$s:add(socket-binding=%1$s)",
                                SERVICE_PROVIDER_HOST));
                client.execute(
                        String.format(
                                "/subsystem=undertow/server=%1$s/host=%1$s-host/setting=access-log:add(prefix=\"%1$s-\")",
                                SERVICE_PROVIDER_HOST));
                // configure a MicroProfile config property that will resolve discordant values for Local Service Router
                // applications, which are both deployed to default-server default-host
                client.execute(
                        "/subsystem=microprofile-config-smallrye/config-source=props:add(properties={" +
                                "\"mp.openapi.extensions.info.description\" = " +
                                "\"Sourced from common MP Config configuration\"})");
                new Administration(client).reload();
            }
        }

        @Override
        public void tearDown() throws Exception {
            try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
                OpenApiServerConfiguration.disableOpenApi(client);
                // remove the MicroProfile config property that will resolve discordant values for Local Service Router
                // applications, which are both deployed to default-server default-host
                client.execute("/subsystem=microprofile-config-smallrye/config-source=props:remove");
                // remove the dedicated virtual host for the service provider
                client.execute(
                        String.format("/subsystem=undertow/server=%1$s/http-listener=%1$s:remove",
                                SERVICE_PROVIDER_HOST));
                client.execute(String.format(
                        "/subsystem=undertow/server=%1$s/host=%1$s-host:remove",
                        SERVICE_PROVIDER_HOST));
                client.execute(String.format("/subsystem=undertow/server=%s:remove", SERVICE_PROVIDER_HOST));
                client.execute(String.format("/socket-binding-group=standard-sockets/socket-binding=%s:remove",
                        SERVICE_PROVIDER_HOST));
                new Administration(client).reload();
            }
        }
    }

    /**
     * @tpTestDetails Test to verify that the WFLYMPOAI0009 WARN is logged when a discordant "global" OpanAPI property
     *                is defined by two deployments in the same virtual host
     * @tpPassCrit The server is logging expected {@code WFLYMPOAI0009} WARN message
     * @tpSince JBoss EAP XP 6
     */
    @Test
    public void testDiscordantGlobalPropertyWarningLogged() throws ConfigurationException, IOException {
        try (final OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
            final String message = "WFLYMPOAI0009: Ignoring deployment-specific property value for externalDocs.url due " +
                    "to conflicts: {anotherLocalServicesRouterDeployment=http://another-oas-model-reader-based-external-docs.org, "
                    +
                    "localServicesRouterDeployment=http://oas-model-reader-based-external-docs.org}";
            final LogChecker logChecker = new ModelNodeLogChecker(client, 100, true);
            Assert.assertTrue(logChecker.logContains(message));
        }
    }

    /**
     * @tpTestDetails Integration test to verify MP Rest Client usage by Local Service Router JAX-RS resource
     * @tpPassCrit Both Local Service Router endpoints is called to retrieve data from Service Provider,
     *             verifying the HTTP response code and content type
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testRoutedEndpoint(
            @ArquillianResource @OperateOnDeployment(ROUTER_DEPLOYMENT_NAME) URL localServiceRouterURL,
            @ArquillianResource @OperateOnDeployment(ANOTHER_ROUTER_DEPLOYMENT_NAME) URL anotherLocalServiceRouterURL) {
        Stream.of(localServiceRouterURL, anotherLocalServiceRouterURL)
                .forEach(url -> {
                    System.out.printf("    Calling %s%n", url);
                    get(url.toExternalForm() + "districts/all")
                            .then()
                            .statusCode(200)
                            .contentType(equalToIgnoringCase(MediaType.APPLICATION_JSON));
                });
    }

    /**
     * @tpTestDetails Verifies proper processing by assessing that information stored in non-routed services
     *                annotations is preserved in final document
     * @tpPassCrit The generated document contains a string that uniquely identifies one of the Local Service Router
     *             endpoints URL and that was generated from local JAX-RS resources annotation
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testNonRoutedEndpoint(
            @ArquillianResource @OperateOnDeployment(ROUTER_DEPLOYMENT_NAME) URL localServiceRouterURL,
            @ArquillianResource @OperateOnDeployment(ANOTHER_ROUTER_DEPLOYMENT_NAME) URL anotherLocalServiceRouterURL) {
        Stream.of(localServiceRouterURL, anotherLocalServiceRouterURL).forEach(url -> get(url.toExternalForm() + "info/fqdn")
                .then()
                .statusCode(200)
                //  RestEasy >3.9.3 adds charset info unless resteasy.add.charset is set to false,
                //  thus expecting for it to be there, see
                //  https://docs.jboss.org/resteasy/docs/3.9.3.Final/userguide/html/Installation_Configuration.html#configuration_switches
                .contentType(equalToIgnoringCase("text/plain;charset=UTF-8")));
    }

    /**
     * @tpTestDetails Test to verify that static file has been used for OpenAPI document generation
     * @tpPassCrit The generated document does contain a string which uniquely identifies one of the routed Service
     *             Provider endpoints URL
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testExpectedStaticFileInformationInOpenApiDoc(
            @ArquillianResource @OperateOnDeployment(ROUTER_DEPLOYMENT_NAME) URL localServiceRouterURL)
            throws URISyntaxException {
        get(localServiceRouterURL.toURI().resolve("/openapi"))
                .then()
                .statusCode(200)
                .contentType(equalToIgnoringCase("application/yaml"))
                .body(containsString("/districts/all:"));

    }

    /**
     * @tpTestDetails Verifies proper processing by using MP OpenAPI programming model to filter the generated document
     * @tpPassCrit Verifies that {@link RoutingServiceConstants#OPENAPI_OPERATION_EXTENSION_PROXY_FQDN_NAME)} extension
     *             value has been successfully modified by {@link OpenApiFilter}, relatively to each deployment context
     *             path.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testOpenApiDocumentForRouterFqdnExtensionModification(
            @ArquillianResource @OperateOnDeployment(ROUTER_DEPLOYMENT_NAME) URL localServiceRouterURL)
            throws URISyntaxException {

        get(localServiceRouterURL.toURI().resolve("/openapi"))
                .then()
                .statusCode(200)
                .contentType(equalToIgnoringCase("application/yaml"))
                .body(containsString(String.format("%s: %s",
                        RoutingServiceConstants.OPENAPI_OPERATION_EXTENSION_PROXY_FQDN_NAME,
                        OpenApiFilter.LOCAL_TEST_ROUTER_FQDN)));

    }

    /**
     * @tpTestDetails Verifies proper processing by using MP OpenAPI programming model to create a custom base document
     * @tpPassCrit Verifies that {@link OpenAPI#getInfo()}, which is a global <i>discordant</i> property, was sourced
     *             from the MicroProfile Config
     *             {@code mp.openapi.extensions.server.default-server.host.default-host.info.description}
     *             property
     * @tpSince JBoss EAP XP 6
     */
    @Test
    public void testOpenApiDocumentForOpenApiInfoChange(
            @ArquillianResource @OperateOnDeployment(ROUTER_DEPLOYMENT_NAME) URL baseURL) throws URISyntaxException {
        get(baseURL.toURI().resolve("/openapi"))
                .then()
                .statusCode(200)
                .contentType(equalToIgnoringCase("application/yaml"))
                .body(not(containsString("Generated by router: ")))
                .body(containsString("Sourced from common MP Config configuration"));
    }
}
