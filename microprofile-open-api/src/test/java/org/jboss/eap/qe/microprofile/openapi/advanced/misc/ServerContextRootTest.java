package org.jboss.eap.qe.microprofile.openapi.advanced.misc;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringCase;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
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
import org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.rest.EchoResource;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.services.InMemoryDistrictService;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.router.RouterApplication;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.router.model.DistrictObject;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.router.rest.LocalServiceRouterInfoResource;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.router.rest.routed.RouterDistrictsResource;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.router.services.DistrictServiceClient;
import org.jboss.eap.qe.microprofile.openapi.model.OpenApiFilter;
import org.jboss.eap.qe.microprofile.openapi.model.OpenApiModelReader;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.ConfigurationException;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.ArquillianContainerProperties;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.ArquillianDescriptorWrapper;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientRelatedException;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.yaml.snakeyaml.Yaml;

/**
 * Test to assess behavior in case of complex/negative scenarios involving server and context root information.
 *
 * The real world scenario for Local Services Router app is implemented here: the OpenAPI document is provided as
 * deliverable by Service Provider staff to Local Services Router staff so that it can be used as official documentation
 * once proper customization is applied.
 */
@RunWith(Arquillian.class)
@ServerSetup({ ServerContextRootTest.OpenApiExtensionSetup.class })
@RunAsClient
public class ServerContextRootTest {

    private final static String PROVIDER_DEPLOYMENT_NAME = "serviceProviderDeployment";
    private final static String ROUTER_DEPLOYMENT_NAME = "localServicesRouterDeployment";
    public static final String SERVER_ELEMENT_CENTRAL_SERVICE_PROVIDER_URL = "http://127.0.0.1:8080/serviceProviderDeployment";
    public static final String SERVER_ELEMENT_CENTRAL_SERVICE_PROVIDER_DESCRIPTION = "Central Service Provider server";

    private static ArquillianContainerProperties arquillianContainerProperties = new ArquillianContainerProperties(
            ArquillianDescriptorWrapper.getArquillianDescriptor());

    private final static String CONFIGURATION_TEMPLATE = "mp.openapi.scan.exclude.packages=org.jboss.eap.qe.microprofile.openapi.apps.routing.router.rest.routed"
            + "\n" +
            "mp.openapi.model.reader=org.jboss.eap.qe.microprofile.openapi.model.OpenApiModelReader"
            + "\n" +
            "mp.openapi.filter=org.jboss.eap.qe.microprofile.openapi.model.OpenApiFilter"
            + "\n" +
            "mp.openapi.scan.disable=false";

    @Deployment(name = PROVIDER_DEPLOYMENT_NAME, order = 1, testable = false)
    public static WebArchive serviceProviderDeployment() {
        //  This is the Services Provider deployment with default configuration and simulates the main deployment in
        //  the current scenario - i.e. the one that is run by Services Provider.
        return ShrinkWrap.create(
                WebArchive.class, PROVIDER_DEPLOYMENT_NAME + ".war")
                .addClasses(
                        ProviderApplication.class,
                        District.class,
                        DistrictEntity.class,
                        DistrictService.class,
                        InMemoryDistrictService.class,
                        DistrictsResource.class,
                        RoutingServiceConstants.class,
                        EchoResource.class)
                .addAsManifestResource(new StringAsset("mp.openapi.extensions.enabled=false"), "microprofile-config.properties")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    /**
     * MP Config properties in {@link ServerContextRootTest#CONFIGURATION_TEMPLATE} are used to tell MP OpenAPI to
     * skip doc generation for exposed "routed" JAX-RS endpoints as service consumers must rely to the original
     * documentation (META-INF/openapi.yaml), plus annotations from Local Service Provider "non-routed" JAX-RS endpoints,
     * edited through an OASModelReader implementation and eventually filtered by a OASFilter one.
     * NOT WORKING in EAR? Here we also add config properties to reach the Services Provider.
     *
     * @return A string containing a set of name/value configuration properties
     */
    private static String buildMicroProfileConfigProperties() {
        return CONFIGURATION_TEMPLATE;
    }

    @Deployment(name = ROUTER_DEPLOYMENT_NAME, order = 2, testable = false)
    public static WebArchive localServicesRouterDeployment()
            throws ConfigurationException, IOException, ManagementClientRelatedException {

        int configuredHTTPPort;
        try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
            configuredHTTPPort = OpenApiServerConfiguration.getHTTPPort(client);
        }
        String props = String.format("services.provider.host=%s\nservices.provider.port=%d",
                arquillianContainerProperties.getDefaultManagementAddress(),
                configuredHTTPPort);

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
                .addAsManifestResource(new StringAsset(buildMicroProfileConfigProperties() + "\n" + props),
                        "microprofile-config.properties")
                .addAsResource(ServerContextRootTest.class.getClassLoader().getResource("META-INF/openapi-server-path.yaml"),
                        "META-INF/openapi.yaml")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    static class OpenApiExtensionSetup implements ServerSetupTask {

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            OpenApiServerConfiguration.enableOpenApi(ManagementClientProvider.onlineStandalone(managementClient));
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            OpenApiServerConfiguration.disableOpenApi(ManagementClientProvider.onlineStandalone(managementClient));
        }
    }

    /**
     * @tpTestDetails Test to verify that the central Service provider {@code echo} endpoint is available
     * @tpPassCrit The central Service provider {@code echo} endpoint is called, HTTP 200 code is sent by server and
     *             response contains the value echoed from {@code message} query parameter sent by the client HTTP request.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testEchoEndpoint(@ArquillianResource @OperateOnDeployment(PROVIDER_DEPLOYMENT_NAME) URL baseURL) {
        String expectedMessage = "Testing echo endpoint on central Service provider";
        final String responseContent = given().queryParam("message", expectedMessage)
                .get(baseURL.toExternalForm() + "echo")
                .then()
                .statusCode(200)
                //  RestEasy >3.9.3 adds charset info unless resteasy.add.charset is set to false,
                //  thus expecting for it to be there, see
                //  https://docs.jboss.org/resteasy/docs/3.9.3.Final/userguide/html/Installation_Configuration.html#configuration_switches
                .contentType(equalToIgnoringCase("text/plain;charset=UTF-8"))
                .extract().asString();
        assertThat(responseContent, equalTo(expectedMessage));
    }

    /**
     * @tpTestDetails Test to verify that the {@code Server} annotation generated by Wildfly implementation is properly
     *                overridden at {@code pathItem} level
     * @tpPassCrit The generated document does contain a string which uniquely identifies the Service Provider
     *             {@code echo} endpoint and that its {@code Server} elements is referencing the Service Provider app.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testExpectedServerInformationInOpenApiDoc(
            @ArquillianResource @OperateOnDeployment(ROUTER_DEPLOYMENT_NAME) URL baseURL) throws URISyntaxException {
        final String responseContent = get(baseURL.toURI().resolve("/openapi"))
                .then()
                .statusCode(200)
                .contentType(equalToIgnoringCase("application/yaml"))
                .body(containsString("/echo:"))
                .extract().asString();

        Yaml yaml = new Yaml();
        Object yamlObject = yaml.load(responseContent);

        Map<String, Object> yamlMap = (Map<String, Object>) yamlObject;
        Map<String, Object> paths = (Map<String, Object>) yamlMap.get("paths");
        Assert.assertFalse("\"paths\" property is empty", paths.isEmpty());

        Map<String, Object> restPath = (Map<String, Object>) paths.get("/echo");
        Assert.assertFalse("\"/echo\" property is empty", restPath.isEmpty());

        Map<String, Object> getMethod = (Map<String, Object>) restPath.get("get");
        Assert.assertFalse("\"/echo\" \"get\" property is empty", getMethod.isEmpty());

        List<Object> servers = (List<Object>) getMethod.get("servers");
        Assert.assertTrue("\"/echo\" operation for GET verb should have exactly 1 server",
                servers.size() == 1);

        Map<String, Object> pathItemServerDefinition = (Map<String, Object>) servers.get(0);
        Assert.assertFalse("Server definition element [0] for \"/echo\" operation of GET verb is empty",
                pathItemServerDefinition.isEmpty());
        Assert.assertTrue(
                "\"description\" property of server definition element [0] for \"/echo\" operation (GET verb) should be set to \""
                        + SERVER_ELEMENT_CENTRAL_SERVICE_PROVIDER_DESCRIPTION + "\"",
                pathItemServerDefinition.get("description").equals(SERVER_ELEMENT_CENTRAL_SERVICE_PROVIDER_DESCRIPTION));
        Assert.assertTrue(
                "\"url\" property of server definition element [0] for \"/echo\" operation (GET verb) should be set to \""
                        + SERVER_ELEMENT_CENTRAL_SERVICE_PROVIDER_URL + "\"",
                pathItemServerDefinition.get("url").equals(SERVER_ELEMENT_CENTRAL_SERVICE_PROVIDER_URL));
    }
}
