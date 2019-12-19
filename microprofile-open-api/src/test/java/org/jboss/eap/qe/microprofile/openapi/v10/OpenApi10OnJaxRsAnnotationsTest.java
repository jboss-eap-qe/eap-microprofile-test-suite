package org.jboss.eap.qe.microprofile.openapi.v10;

import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.comparesEqualTo;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.eap.qe.microprofile.openapi.OpenApiDeploymentUrlProvider;
import org.jboss.eap.qe.microprofile.openapi.OpenApiServerConfiguration;
import org.jboss.eap.qe.microprofile.openapi.OpenApiTestConstants;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.ProviderApplication;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.RoutingServiceConstants;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.api.DistrictService;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.data.DistrictEntity;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.model.District;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.rest.DistrictsResource;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.services.InMemoryDistrictService;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.ConfigurationException;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientRelatedException;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.yaml.snakeyaml.Yaml;

/**
 * Test cases for MP OpenAPI v. 1.0 annotations on JAX-RS components (migrated from RHOAR QE TS)
 */
@RunWith(Arquillian.class)
@RunAsClient
public class OpenApi10OnJaxRsAnnotationsTest {

    private final static String DEPLOYMENT_NAME = OpenApi10OnJaxRsAnnotationsTest.class.getSimpleName();

    static String openApiUrl, resourceUrl;
    static OnlineManagementClient onlineManagementClient;

    @BeforeClass
    public static void setup() throws ManagementClientRelatedException, ConfigurationException {
        openApiUrl = OpenApiDeploymentUrlProvider.composeDefaultOpenApiUrl();
        resourceUrl = String.format(
                "http://%s:%s/%s/districts/DW",
                OpenApiTestConstants.DEFAULT_HOST_NAME,
                OpenApiTestConstants.DEFAULT_ENDPOINT_PORT,
                DEPLOYMENT_NAME);
        //  MP OpenAPI up
        onlineManagementClient = ManagementClientProvider.onlineStandalone();
        OpenApiServerConfiguration.enableOpenApi(onlineManagementClient);
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
    public static Archive<?> createCDeployment() {
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
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return deployment;
    }

    /**
     * @tpTestDetails Tests for application endpoint to be working
     * @tpPassCrit Verifies that the returned JSON data corresponds to server side POJO
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testAppEndpoint() {
        get(resourceUrl).then()
                .statusCode(200)
                .body("code", comparesEqualTo("DW"),
                        "obsolete", comparesEqualTo(Boolean.FALSE),
                        "name", comparesEqualTo("Western district"));
    }

    /**
     * @tpTestDetails Tests for proper OpenAPI documentation of application endpoint containing JAX-RS annotations
     * @tpPassCrit Verifies that the returned YAML data has corresponding values for JAX-RS annotations
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testOpenApiDocumentForPathAndQueryParam() {

        String responseContent = get(openApiUrl)
                .then()
                .statusCode(200)
                .extract().asString();

        Yaml yaml = new Yaml();
        Object yamlObject = yaml.load(responseContent);

        Map<String, Object> yamlMap = (Map<String, Object>) yamlObject;
        Map<String, Object> paths = (Map<String, Object>) yamlMap.get("paths");
        Assert.assertFalse("\"paths\" property is empty", paths.isEmpty());

        Map<String, Object> restPath = (Map<String, Object>) paths.get("/districts/{code}");
        Assert.assertFalse("\"/districts/{code}\" property is empty", restPath.isEmpty());

        Map<String, Object> getMethod = (Map<String, Object>) restPath.get("get");
        Assert.assertFalse("\"/districts/{code}\" \"get\" property is empty", getMethod.isEmpty());

        List<Object> parameters = (List<Object>) getMethod.get("parameters");
        Assert.assertTrue("\"/districts/{code}\" operation for GET verb should have exactly 2 parameters",
                parameters.size() == 2);

        Map<String, Object> pathParam = (Map<String, Object>) parameters.get(0);
        Assert.assertFalse("Parameter [0] for \"/districts/{code}\" operation for GET verb is empty", pathParam.isEmpty());
        Assert.assertTrue(
                "\"name\" property of parameter [0] for \"/districts/{code}\" operation (GET verb) should be set to \"code\"",
                pathParam.get("name").equals("code"));
        Assert.assertTrue(
                "\"in\" property of parameter [0] for \"/districts/{code}\" operation (GET verb) should be set to \"path\"",
                pathParam.get("in").equals("path"));

        Map<String, Object> queryParam = (Map<String, Object>) parameters.get(1);
        Assert.assertFalse("Parameter [1] for \"/districts/{code}\" operation for GET verb is empty", queryParam.isEmpty());
        Assert.assertTrue(
                "\"name\" property of parameter [1] for \"/districts/{code}\" operation (GET verb) should be set to \"excludeObsolete\"",
                queryParam.get("name").equals("excludeObsolete"));
        Assert.assertTrue(
                "\"in\" property of parameter [1] for \"/districts/{code}\" operation (GET verb) should be set to \"query\"",
                queryParam.get("in").equals("query"));
    }

    /**
     * @tpTestDetails Tests for proper OpenAPI documentation of application endpoint returning a structured (POJO)
     *                response body
     * @tpPassCrit Verifies that the returned YAML data has corresponding values for schema definition of returned POJO
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testOpenApiDocumentForResponseTypeSchema() {

        String responseContent = get(openApiUrl)
                .then()
                .statusCode(200)
                .extract().asString();

        Yaml yaml = new Yaml();
        Object yamlObject = yaml.load(responseContent);
        Map<String, Object> yamlMap = (Map<String, Object>) yamlObject;

        Map<String, Object> paths = (Map<String, Object>) yamlMap.get("paths");
        Assert.assertFalse("\"paths\" property is empty", paths.isEmpty());

        Map<String, Object> getDistrictByCodePath = (Map<String, Object>) paths.get("/districts/{code}");
        Assert.assertFalse("\"/districts/{code}\" property is empty", getDistrictByCodePath.isEmpty());

        Map<String, Object> getMethod = (Map<String, Object>) getDistrictByCodePath.get("get");
        Assert.assertFalse("\"/districts/{code}\" \"get\" property is empty", getMethod.isEmpty());
        Assert.assertNotNull("\"/districts/{code}\" \"responses\" for GET verb is null", getMethod.get("responses"));

        Map<String, Object> responses = (Map<String, Object>) getMethod.get("responses");
        Assert.assertNotNull("\"/districts/{code}\" \"response\" for GET verb and HTTP status 200 is null",
                responses.get("200"));

        Map<String, Object> http200Response = (Map<String, Object>) responses.get("200");
        Assert.assertNotNull(
                "\"/districts/{code}\" \"response\" for GET verb and HTTP status 200 has null \"content\" property",
                http200Response.get("content"));

        Map<String, Object> http200ResponseContent = (Map<String, Object>) http200Response.get("content");
        Assert.assertNotNull(
                "\"/districts/{code}\" \"response\" for GET verb and HTTP status 200 has \"content\" but null \"application/json\" property",
                http200ResponseContent.get("application/json"));

        Map<String, Object> jsonContent = (Map<String, Object>) http200ResponseContent.get("application/json");
        Assert.assertNotNull(
                "\"content\" property of \"/districts/{code}\" \"response\" for GET verb and HTTP status 200 has null \"schema\" property for \"application/json\" type",
                jsonContent.get("schema"));

        Assert.assertNotNull("\"components\" property is null", yamlMap.get("components"));

        Map<String, Object> components = (Map<String, Object>) yamlMap.get("components");
        Assert.assertNotNull("\"components/schemas\" property is null", components.get("schemas"));

        Map<String, Object> schemas = (Map<String, Object>) components.get("schemas");
        Assert.assertNotNull("\"components/schemas/District\" property is null", schemas.get("DistrictEntity"));

        Map<String, Object> responsePOJO = (Map<String, Object>) schemas.get("DistrictEntity");
        Assert.assertNotNull(responsePOJO.get("properties"));

        Map<String, Object> properties = (Map<String, Object>) responsePOJO.get("properties");
        Assert.assertNotNull("\"components/schemas/District\" is missing \"code\" property", properties.get("code"));
        Assert.assertNotNull("\"components/schemas/District\" is missing \"name\" property", properties.get("name"));
        Assert.assertNotNull("\"components/schemas/District\" is missing \"obsolete\" property", properties.get("obsolete"));
    }
}
