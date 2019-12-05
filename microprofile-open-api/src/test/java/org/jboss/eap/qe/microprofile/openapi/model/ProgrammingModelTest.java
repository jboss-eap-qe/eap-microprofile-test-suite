package org.jboss.eap.qe.microprofile.openapi.model;

import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.not;

import java.io.IOException;

import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.eap.qe.microprofile.openapi.OpenApiDeploymentUrlProvider;
import org.jboss.eap.qe.microprofile.openapi.OpenApiServerConfiguration;
import org.jboss.eap.qe.microprofile.openapi.RestApplication;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.RoutingServiceConstants;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.api.DistrictService;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.data.DistrictEntity;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.model.District;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.rest.DistrictsResource;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.services.InMemoryDistrictService;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.ConfigurationException;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientRelatedException;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;

/**
 * Test cases for MP OpenAPI programming model (migrated from RHOAR QE TS)
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ProgrammingModelTest {

    private final static String DEPLOYMENT_NAME = ProgrammingModelTest.class.getSimpleName();

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

    @Deployment(testable = false)
    public static WebArchive createCentralDeployment() {
        WebArchive deployment = ShrinkWrap.create(
                WebArchive.class,
                String.format("%s.war", DEPLOYMENT_NAME))
                .addClasses(RestApplication.class)
                .addClasses(
                        District.class,
                        DistrictEntity.class,
                        DistrictService.class,
                        InMemoryDistrictService.class,
                        DistrictsResource.class,
                        RoutingServiceConstants.class)
                .addClasses(OpenApiModelReader.class, OpenApiFilter.class)
                .addAsResource(
                        "META-INF/microprofile-config.properties")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return deployment;
    }

    /**
     * @tpTestDetails Tests for application endpoint to be working
     * @tpPassCrit Verifies that the returned JSON data is a non empty list of districts
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testAppEndpoint() {
        get(OpenApiDeploymentUrlProvider.composeDefaultDeploymentBaseUrl(DEPLOYMENT_NAME + "/districts/all"))
                .then()
                .statusCode(200)
                .contentType(equalToIgnoringCase(MediaType.APPLICATION_JSON))
                .body(not(empty()));
    }

    /**
     * @tpTestDetails Verifies MP OpenAPI programming model by creating custom base document
     * @tpPassCrit Verifies that {@link OpenAPI#getInfo()} has been successfully modified by {@link OpenApiModelReader}
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testOpenApiDocumentForOpenApiInfoChange() {
        get(openApiUrl)
                .then()
                .statusCode(200)
                .contentType(equalToIgnoringCase("application/yaml"))
                .body(containsString("Generated"));
    }

    /**
     * @tpTestDetails Verifies MP OpenAPI programming model by filtering generated document
     * @tpPassCrit Verifies that {@link RoutingServiceConstants#OPENAPI_OPERATION_EXTENSION_PROXY_FQDN_NAME)} extension
     *             value has been successfully modified by {@link OpenApiFilter}
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testOpenApiDocumentForRouterFqdnExtensionModification() {
        get(openApiUrl)
                .then()
                .statusCode(200)
                .contentType(equalToIgnoringCase("application/yaml"))
                .body(containsString(OpenApiFilter.LOCAL_TEST_ROUTER_FQDN));
    }
}
