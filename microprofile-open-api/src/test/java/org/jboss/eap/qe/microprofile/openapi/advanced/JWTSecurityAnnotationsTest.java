package org.jboss.eap.qe.microprofile.openapi.advanced;

import static io.restassured.RestAssured.get;
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
import org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.services.InMemoryDistrictService;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.router.RouterApplication;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.router.model.DistrictObject;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.router.rest.LocalServiceRouterInfoResource;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.router.rest.routed.RouterDistrictsResource;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.router.services.DistrictServiceClient;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.ConfigurationException;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.ArquillianContainerProperties;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.ArquillianDescriptorWrapper;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientRelatedException;
import org.jboss.shrinkwrap.api.Archive;
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
 * Test cases for usage of JWT related security MP OpenAPI annotations
 */
@RunWith(Arquillian.class)
@ServerSetup({ JWTSecurityAnnotationsTest.OpenApiExtensionSetup.class })
@RunAsClient
public class JWTSecurityAnnotationsTest {

    private final static String PROVIDER_DEPLOYMENT_NAME = "serviceProviderDeployment";
    private final static String ROUTER_DEPLOYMENT_NAME = "localServicesRouterDeployment";
    private final static String CONFIGURATION_TEMPLATE = "mp.openapi.scan.exclude.packages=org.jboss.eap.qe.microprofile.openapi.apps.routing.router.rest.routed"
            + "\n" +
            "mp.openapi.scan.disable=false"
            + "\n" +
            "services.provider.host=%s"
            + "\n" +
            "services.provider.port=%d";

    private static ArquillianContainerProperties arquillianContainerProperties = new ArquillianContainerProperties(
            ArquillianDescriptorWrapper.getArquillianDescriptor());

    @Deployment(name = PROVIDER_DEPLOYMENT_NAME, order = 1, testable = false)
    public static Archive<?> serviceProviderDeployment() {
        //  MP OpenAPI disabled on this Services Provider deployment for testing purposes, we don't need it here
        String mpConfigProperties = "mp.openapi.extensions.enabled=false";
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
                .addAsManifestResource(new StringAsset(mpConfigProperties), "microprofile-config.properties")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    /**
     * OpenAPI doc here is exposing doc generated only from Local Service Provider "non-routed" JAX-RS endpoints
     * as MP Config is used to tell MP OpenAPI to skip doc generation for exposed "routed" JAX-RS endpoints
     * Here we also add config properties to reach the Services Provider.
     *
     * @return A string containing a set of name/value configuration properties.
     * @throws ManagementClientRelatedException CLI management exceptions
     * @throws ConfigurationException Arquillian container configuration exception
     * @throws IOException Management client disposal
     */
    private static String buildMicroProfileConfigProperties()
            throws ManagementClientRelatedException, ConfigurationException, IOException {

        int configuredHTTPPort;
        try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
            configuredHTTPPort = OpenApiServerConfiguration.getHTTPPort(client);
        }
        return String.format(CONFIGURATION_TEMPLATE, arquillianContainerProperties.getDefaultManagementAddress(),
                configuredHTTPPort);
    }

    @Deployment(name = ROUTER_DEPLOYMENT_NAME, order = 2, testable = false)
    public static Archive<?> localServicesRouterDeployment()
            throws ConfigurationException, IOException, ManagementClientRelatedException {

        String mpConfigProperties = buildMicroProfileConfigProperties();

        return ShrinkWrap.create(
                WebArchive.class, ROUTER_DEPLOYMENT_NAME + ".war")
                .addClasses(
                        RouterApplication.class,
                        LocalServiceRouterInfoResource.class,
                        DistrictObject.class,
                        RouterDistrictsResource.class,
                        DistrictServiceClient.class)
                .addAsManifestResource(new StringAsset(mpConfigProperties), "microprofile-config.properties")
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
     * @tpTestDetails Test to verify that the app secured endpoints are up and running
     * @tpPassCrit One of the Local Service Router endpoints is called to retrieve data from Service Provider,
     *             verifying the HTTP response code and content type
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testAppEndpoint(@ArquillianResource @OperateOnDeployment(ROUTER_DEPLOYMENT_NAME) URL baseURL) {
        get(baseURL.toExternalForm() + "info/fqdn")
                .then()
                .statusCode(200)
                //  RestEasy >3.9.3 adds charset info unless resteasy.add.charset is set to false,
                //  thus expecting for it to be there, see
                //  https://docs.jboss.org/resteasy/docs/3.9.3.Final/userguide/html/Installation_Configuration.html#configuration_switches
                .contentType(equalToIgnoringCase("text/plain;charset=UTF-8"));
    }

    /**
     * @tpTestDetails Test to verify that information derived from {@code @SecuritySchemes} annotations - in this case
     *                related to JWT - is contained in generated OpenAPI document, see
     *                https://groups.google.com/forum/?utm_medium=email&utm_source=footer#!msg/quarkus-dev/QZgqrblsYEc/sgDcnHYlBAAJ
     * @tpPassCrit OpenAPI endpoint is called to verify its availability (HTTP response code 200) and then
     *             the returned data is checked to asses that operations in the generated OpenAPI document do contain
     *             information
     *             about the JWT related {@code @SecurityScheme} which is placed just over the JAX-RS resource class
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testForSecurityAnnotationsForAllOperations(
            @ArquillianResource @OperateOnDeployment(ROUTER_DEPLOYMENT_NAME) URL baseURL) throws URISyntaxException {
        String responseContent = get(baseURL.toURI().resolve("/openapi"))
                .then()
                .statusCode(200)
                .extract().asString();

        Yaml yaml = new Yaml();
        Map<String, Object> yamlMap = yaml.load(responseContent);

        Map<String, Object> paths = (Map<String, Object>) yamlMap.get("paths");
        Assert.assertFalse("\"paths\" property is empty", paths.isEmpty());

        Map<String, Object> operationPath = (Map<String, Object>) paths.get("/info/fqdn");
        Assert.assertFalse("\"/info/fqdn\" property is empty", operationPath.isEmpty());

        Map<String, Object> getMethod = (Map<String, Object>) operationPath.get("get");
        Assert.assertFalse("\"/info/fqdn\" \"get\" property is empty", getMethod.isEmpty());

        List<Object> securitySchemes = (List<Object>) getMethod.get("security");
        Assert.assertTrue("\"/info/fqdn\" operation for GET verb should have exactly 1 security scheme",
                securitySchemes.size() == 1);

        Map<String, Object> httpSecuredSecurityScheme = (Map<String, Object>) securitySchemes.get(0);
        Assert.assertFalse("Security scheme item [0] for \"/info/fqdn\" operation for GET verb is empty",
                httpSecuredSecurityScheme.isEmpty());
        Assert.assertNotNull(
                "Security requirement named \"http_secured\" belonging to Security scheme item [0] for \"/info/fqdn\" operation (GET verb) should be present",
                httpSecuredSecurityScheme.get("http_secured"));
    }
}
