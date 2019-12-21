package org.jboss.eap.qe.microprofile.openapi.model;

import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.not;

import java.net.URISyntaxException;
import java.net.URL;

import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.jboss.arquillian.container.test.api.Deployment;
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
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;

/**
 * Test cases for MP OpenAPI programming model (migrated from RHOAR QE TS)
 */
@RunWith(Arquillian.class)
@ServerSetup({ ProgrammingModelTest.OpenApiExtensionSetup.class })
@RunAsClient
public class ProgrammingModelTest {

    private final static String DEPLOYMENT_NAME = ProgrammingModelTest.class.getSimpleName();

    private static OnlineManagementClient onlineManagementClient;

    @Deployment(testable = false)
    public static WebArchive createCentralDeployment() {

        String mpConfigProperties = "mp.openapi.model.reader=org.jboss.eap.qe.microprofile.openapi.model.OpenApiModelReader"
                + "\n" +
                "mp.openapi.filter=org.jboss.eap.qe.microprofile.openapi.model.OpenApiFilter"
                + "\n" +
                "mp.openapi.scan.disable=false";

        WebArchive deployment = ShrinkWrap.create(
                WebArchive.class,
                String.format("%s.war", DEPLOYMENT_NAME))
                .addClasses(ProviderApplication.class)
                .addClasses(
                        District.class,
                        DistrictEntity.class,
                        DistrictService.class,
                        InMemoryDistrictService.class,
                        DistrictsResource.class,
                        RoutingServiceConstants.class)
                .addClasses(
                        OpenApiModelReader.class,
                        OpenApiFilter.class)
                .addAsManifestResource(new StringAsset(mpConfigProperties), "microprofile-config.properties")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return deployment;
    }

    /**
     * @tpTestDetails Tests for application endpoint to be working
     * @tpPassCrit Verifies that the returned JSON data is a non empty list of districts
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testAppEndpoint(@ArquillianResource URL baseURL) {
        get(baseURL.toExternalForm() + "districts/all")
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
    public void testOpenApiDocumentForOpenApiInfoChange(@ArquillianResource URL baseURL) throws URISyntaxException {
        get(baseURL.toURI().resolve("/openapi"))
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
    public void testOpenApiDocumentForRouterFqdnExtensionModification(@ArquillianResource URL baseURL)
            throws URISyntaxException {
        get(baseURL.toURI().resolve("/openapi"))
                .then()
                .statusCode(200)
                .contentType(equalToIgnoringCase("application/yaml"))
                .body(containsString(OpenApiFilter.LOCAL_TEST_ROUTER_FQDN));
    }

    static class OpenApiExtensionSetup implements ServerSetupTask {

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            //  MP OpenAPI up
            onlineManagementClient = ManagementClientProvider.onlineStandalone();
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
}
