package org.jboss.eap.qe.microprofile.openapi.integration.restclient;

import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.*;

import java.io.IOException;

import javax.ws.rs.core.MediaType;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.eap.qe.microprofile.openapi.OpenApiDeploymentUrlProvider;
import org.jboss.eap.qe.microprofile.openapi.OpenApiServerConfiguration;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.ProviderApplication;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.RoutingServiceConstants;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.api.DistrictService;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.data.DistrictEntity;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.model.District;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.rest.DistrictsResource;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.services.InMemoryDistrictService;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.router.model.DistrictObject;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.router.rest.LocalServiceRouterInfoResource;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.router.rest.routed.RouterDistrictsResource;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.router.services.DistrictServiceClient;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.ConfigurationException;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientRelatedException;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;

/**
 * Test cases for MP OpenAPI and MP Rest Client integration
 */
@RunWith(Arquillian.class)
@RunAsClient
public class RestClientIntegrationTest {

    private final static String PROVIDER_DEPLOYMENT_NAME = "serviceProviderDeployment";
    private final static String ROUTER_DEPLOYMENT_NAME = "localServicesRouterDeployment";

    private static String openApiUrl;
    private static OnlineManagementClient onlineManagementClient;

    @BeforeClass
    public static void setup() throws ManagementClientRelatedException, ConfigurationException {
        openApiUrl = OpenApiDeploymentUrlProvider.composeDefaultOpenApiUrl();
        //  MP OpenAPI up
        onlineManagementClient = ManagementClientProvider.onlineStandalone();
        if (!OpenApiServerConfiguration.openapiSubsystemExists(onlineManagementClient)) {
            OpenApiServerConfiguration.enableOpenApi(onlineManagementClient);
        }
    }

    @AfterClass
    public static void tearDown() throws ManagementClientRelatedException, IOException {
        //  MP OpenAPI down
        try {
            OpenApiServerConfiguration.disableOpenApi(onlineManagementClient);
        } finally {
            onlineManagementClient.close();
        }
    }

    @Deployment(name = "serviceProviderDeployment", order = 1, testable = false)
    public static Archive<?> serviceProviderDeployment() {
        //  MP OpenAPI disabled on this Services Provider deployment for testing purposes, we don't want it here
        String mpConfigProperties = "mp.openapi.extensions.enabled=false";
        WebArchive deployment = ShrinkWrap.create(
                WebArchive.class, PROVIDER_DEPLOYMENT_NAME + ".war")
                .addClasses(ProviderApplication.class)
                .addClasses(
                        District.class,
                        DistrictEntity.class,
                        DistrictService.class,
                        InMemoryDistrictService.class,
                        DistrictsResource.class,
                        RoutingServiceConstants.class)
                .addAsManifestResource(new StringAsset(mpConfigProperties), "microprofile-config.properties")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return deployment;
    }

    @Deployment(name = "localServicesRouterDeployment", order = 2, testable = false)
    public static Archive<?> localServicesRouterDeployment() {
        //  OpenAPI doc here is exposing doc generated only from Local Service Provider "non-routed" JAX-RS endpoints
        //  as MP Config is used to tell MP OpenAPI to skip doc generation for exposed "routed" JAX-RS endpoints
        String mpConfigProperties = "mp.openapi.scan.exclude.packages=org.jboss.eap.qe.microprofile.openapi.apps.routing.router.rest.routed"
                + "\n" +
                "mp.openapi.scan.disable=false";

        WebArchive deployment = ShrinkWrap.create(
                WebArchive.class, ROUTER_DEPLOYMENT_NAME + ".war")
                .addClasses(
                        ProviderApplication.class,
                        LocalServiceRouterInfoResource.class)
                .addClasses(
                        DistrictObject.class,
                        RouterDistrictsResource.class,
                        DistrictServiceClient.class)
                .addAsManifestResource(new StringAsset(mpConfigProperties), "microprofile-config.properties")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return deployment;
    }

    /**
     * @tpTestDetails Integration test to verify MP Rest Client usage by Local Service Router JAX-RS resource
     * @tpPassCrit One of the Local Service Router endpoints is called to retrieve data from Service Provider,
     *             verifying the HTTP response code and content type
     *
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testAppEndpoint() {
        get(OpenApiDeploymentUrlProvider.composeDefaultDeploymentBaseUrl(ROUTER_DEPLOYMENT_NAME + "/districts/all"))
                .then()
                .statusCode(200)
                .contentType(equalToIgnoringCase(MediaType.APPLICATION_JSON));
    }

    /**
     * @tpTestDetails Integration test to verify that MP Rest Client integration does not affect OpenAPI generation
     * @tpPassCrit The generated document does not contain a string which uniquely identifies one of the Service
     *             Provider endpoints URL
     *
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testNoRestClientInterfaceAnnotationInOpenApiDoc() {
        get(openApiUrl)
                .then()
                .statusCode(200)
                .contentType(equalToIgnoringCase("application/yaml"))
                .body(not(containsString("/districts/all:")));
    }

    /**
     * @tpTestDetails Integration test to verify that MP Rest Client integration does not affect OpenAPI generation
     * @tpPassCrit The generated document contains a string that uniquely identifies one of the Local Service Router
     *             endpoints URL and that was generated from local JAX-RS resources annotation
     *
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testOpenApiDocContainsLocalServicesRouterInformation() {
        get(openApiUrl)
                .then()
                .statusCode(200)
                .contentType(equalToIgnoringCase("application/yaml"))
                .body(containsString("fqdn"));
    }
}
