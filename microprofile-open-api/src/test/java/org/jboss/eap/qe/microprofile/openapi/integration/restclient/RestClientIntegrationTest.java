package org.jboss.eap.qe.microprofile.openapi.integration.restclient;

import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.not;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

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
import org.jboss.eap.qe.microprofile.tooling.server.configuration.ConfigurationException;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.ArquillianContainerProperties;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.ArquillianDescriptorWrapper;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.MicroProfileServerSetupTask;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientRelatedException;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.deployment.ConfigurationUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;

import jakarta.ws.rs.core.MediaType;

/**
 * Test cases for MP OpenAPI and MP Rest Client integration
 */
@RunWith(Arquillian.class)
@ServerSetup({ RestClientIntegrationTest.OpenApiExtensionSetup.class })
@RunAsClient
public class RestClientIntegrationTest {

    private final static String PROVIDER_DEPLOYMENT_NAME = "serviceProviderDeployment";
    private final static String ROUTER_DEPLOYMENT_NAME = "localServicesRouterDeployment";

    private static ArquillianContainerProperties arquillianContainerProperties = new ArquillianContainerProperties(
            ArquillianDescriptorWrapper.getArquillianDescriptor());

    @Deployment(name = PROVIDER_DEPLOYMENT_NAME, order = 1, testable = false)
    public static Archive<?> serviceProviderDeployment() {
        //  MP OpenAPI disabled on this Services Provider deployment for testing purposes, we don't want it here
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
                .addAsWebInfResource(ConfigurationUtil.BEANS_XML_FILE_LOCATION, "beans.xml");
    }

    @Deployment(name = ROUTER_DEPLOYMENT_NAME, order = 2, testable = false)
    public static Archive<?> localServicesRouterDeployment()
            throws ConfigurationException, IOException, ManagementClientRelatedException {

        int configuredHTTPPort;
        try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
            configuredHTTPPort = OpenApiServerConfiguration.getHTTPPort(client);
        }

        //  OpenAPI doc here is exposing doc generated only from Local Service Provider "non-routed" JAX-RS endpoints
        //  as MP Config is used to tell MP OpenAPI to skip doc generation for exposed "routed" JAX-RS endpoints
        //  Here we also add config properties to reach the Services Provider
        String mpConfigProperties = "mp.openapi.scan.exclude.packages=org.jboss.eap.qe.microprofile.openapi.apps.routing.router.rest.routed"
                + "\n" +
                "mp.openapi.scan.disable=false"
                + "\n" +
                //  both deployments sit on the same host
                "services.provider.host=" + arquillianContainerProperties.getDefaultManagementAddress()
                + "\n" +
                String.format("services.provider.port=%d", configuredHTTPPort);

        return ShrinkWrap.create(
                WebArchive.class, ROUTER_DEPLOYMENT_NAME + ".war")
                .addClasses(
                        RouterApplication.class,
                        LocalServiceRouterInfoResource.class,
                        DistrictObject.class,
                        RouterDistrictsResource.class,
                        DistrictServiceClient.class)
                .addAsManifestResource(new StringAsset(mpConfigProperties), "microprofile-config.properties")
                .addAsWebInfResource(ConfigurationUtil.BEANS_XML_FILE_LOCATION, "beans.xml");
    }

    static class OpenApiExtensionSetup implements MicroProfileServerSetupTask {

        @Override
        public void setup() throws Exception {
            try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
                OpenApiServerConfiguration.enableOpenApi(client);
            }
        }

        @Override
        public void tearDown() throws Exception {
            try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
                OpenApiServerConfiguration.disableOpenApi(client);
            }
        }
    }

    /**
     * @tpTestDetails Integration test to verify MP Rest Client usage by Local Service Router JAX-RS resource
     * @tpPassCrit One of the Local Service Router endpoints is called to retrieve data from Service Provider,
     *             verifying the HTTP response code and content type
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testAppEndpoint(@ArquillianResource @OperateOnDeployment(ROUTER_DEPLOYMENT_NAME) URL baseURL) {
        get(baseURL.toExternalForm() + "districts/all")
                .then()
                .statusCode(200)
                .contentType(equalToIgnoringCase(MediaType.APPLICATION_JSON));
    }

    /**
     * @tpTestDetails Integration test to verify that MP Rest Client integration does not affect OpenAPI generation
     * @tpPassCrit The generated document does not contain a string which uniquely identifies one of the Service
     *             Provider endpoints URL
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testNoRestClientInterfaceAnnotationInOpenApiDoc(
            @ArquillianResource @OperateOnDeployment(ROUTER_DEPLOYMENT_NAME) URL baseURL) throws URISyntaxException {
        get(baseURL.toURI().resolve("/openapi"))
                .then()
                .statusCode(200)
                .contentType(equalToIgnoringCase("application/yaml"))
                .body(not(containsString("/districts/all:")));
    }

    /**
     * @tpTestDetails Integration test to verify that MP Rest Client integration does not affect OpenAPI generation
     * @tpPassCrit The generated document contains a string that uniquely identifies one of the Local Service Router
     *             endpoints URL and that was generated from local JAX-RS resources annotation
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testOpenApiDocContainsLocalServicesRouterInformation(
            @ArquillianResource @OperateOnDeployment(ROUTER_DEPLOYMENT_NAME) URL baseURL)
            throws URISyntaxException {
        get(baseURL.toURI().resolve("/openapi"))
                .then()
                .statusCode(200)
                .contentType(equalToIgnoringCase("application/yaml"))
                .body(containsString("fqdn"));
    }
}
