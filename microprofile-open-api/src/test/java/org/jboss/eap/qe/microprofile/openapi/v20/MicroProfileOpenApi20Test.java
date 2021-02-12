package org.jboss.eap.qe.microprofile.openapi.v20;

import static io.restassured.RestAssured.get;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jboss.arquillian.container.test.api.Deployment;
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
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.MicroProfileServerSetupTask;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.yaml.snakeyaml.Yaml;

/**
 * Tests for OpenAPI documentation of MP OpenAPI v2.0 annotations
 */
@RunWith(Arquillian.class)
@ServerSetup({ MicroProfileOpenApi20Test.OpenApiExtensionSetup.class })
@RunAsClient
public class MicroProfileOpenApi20Test {

    private final static String DEPLOYMENT_NAME = MicroProfileOpenApi20Test.class.getSimpleName();

    @Deployment(testable = false)
    public static Archive<?> deployment() {
        return ShrinkWrap.create(WebArchive.class, String.format("%s.war", DEPLOYMENT_NAME))
                .addClasses(
                        ProviderApplication.class,
                        District.class,
                        DistrictEntity.class,
                        DistrictService.class,
                        InMemoryDistrictService.class,
                        DistrictsResource.class,
                        RoutingServiceConstants.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsResource(MicroProfileOpenApi20Test.class.getClassLoader().getResource(
                        "META-INF/schema-microprofile-config.properties"),
                        "META-INF/microprofile-config.properties");
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
     * @tpTestDetails Tests for OpenAPI documentation of MP OpenAPI v2.0
     *                * annotations
     * @tpPassCrit Verifies that generated document contains information for v2.0 features:<br>
     *             - the addition of @SchemaProperty annotation for a schema inline definition <br>
     *             - the addition of @RequestBodySchema annotation for a request body schema definition <br>
     *             - the addition of @APIResponseSchema annotation for a response body schema definition <br>
     *             - the addition of mp.openapi.schema.* property define a schema for Java classes
     * @tpSince EAP 7.4.0.CD23
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testOpenApiDocumentForV20Annotations(@ArquillianResource URL baseURL) throws URISyntaxException {
        String responseContent = get(baseURL.toURI().resolve("/openapi")).then()
                .statusCode(200)
                .extract().asString();

        Yaml yaml = new Yaml();
        Object yamlObject = yaml.load(responseContent);
        Map<String, Object> yamlMap = (Map<String, Object>) yamlObject;

        Map<String, Object> paths = (Map<String, Object>) yamlMap.get("paths");
        Assert.assertFalse("\"paths\" property is empty", paths.isEmpty());

        // 2.0 The @SchemaProperty annotation has been added to allow the properties for a schema to be defined inline
        Map<String, Object> getDistrictByCodePath = (Map<String, Object>) paths.get("/districts/{code}");
        Assert.assertFalse("\"/districts/{code}\" property is empty", getDistrictByCodePath.isEmpty());

        Map<String, Object> getMethod = (Map<String, Object>) getDistrictByCodePath.get("get");
        Assert.assertFalse("\"/districts/{code}\" \"get\" property is empty", getMethod.isEmpty());
        Assert.assertEquals("\"/districts/{code}\" \"get\" property should have exactly 6 keys",
                6, getMethod.keySet().size());

        Map<String, Object> responses = (Map<String, Object>) getMethod.get("responses");
        Assert.assertFalse("\"/districts/{code}\" \"responses\" for GET verb is empty", responses.isEmpty());

        Map<String, Object> http200Response = (Map<String, Object>) responses.get("200");
        Assert.assertFalse("\"/districts/{code}\" \"response\" for GET verb and HTTP status 200 is empty",
                http200Response.isEmpty());

        Map<String, Object> contentAnnotation = (Map<String, Object>) http200Response.get("content");
        Assert.assertFalse(
                "\"/districts/{code}\" \"response\" for GET verb and HTTP status 200 has empty \"content\" property",
                contentAnnotation.isEmpty());

        Map<String, Object> contentTypeJson = (Map<String, Object>) contentAnnotation.get("application/json");
        Assert.assertFalse(
                "\"/districts/{code}\" \"response\" for GET verb and HTTP status 200 has \"content\" but empty \"application/json\" property",
                contentTypeJson.isEmpty());

        Map<String, Object> schema = (Map<String, Object>) contentTypeJson.get("schema");
        Assert.assertFalse(
                "\"/districts/{code}\" \"response\" for GET verb and HTTP status 200 has \"application/json\" but empty \"schema\" property",
                schema.isEmpty());

        Map<String, Object> schemaProperties = (Map<String, Object>) schema.get("properties");
        Assert.assertFalse(
                "\"/districts/{code}\" \"response\" for GET verb and HTTP status 200 has \"schema\" but empty \"properties\" property",
                schemaProperties.isEmpty());

        Assert.assertTrue(
                "\"/districts/{code}\" \"response\" for GET verb and HTTP status 200 has \"properties\" but missing \"alias\" property",
                schemaProperties.containsKey("alias"));

        Assert.assertTrue("", ((LinkedHashMap) schemaProperties.get("alias")).containsValue("Alias of district"));

        // 2.0 The @RequestBodySchema annotation has been added to provide a shorthand mechanism to specify the schema for a request body
        Map<String, Object> getDistrictByCode20Path = (Map<String, Object>) paths.get("/districts/{code}/v20");
        Assert.assertFalse("\"/districts/{code}/v20\" property is empty", getDistrictByCode20Path.isEmpty());

        Map<String, Object> patchMethod = (Map<String, Object>) getDistrictByCodePath.get("patch");
        Assert.assertFalse("\"/districts/{code}/v20\" \"patch\" property is empty", patchMethod.isEmpty());
        Assert.assertEquals("\"/districts/{code}/v20\" \"patch\" property should have exactly 13 keys",
                13, patchMethod.keySet().size());

        Map<String, Object> requestBody = (Map<String, Object>) patchMethod.get("requestBody");
        Assert.assertFalse("\"/districts/{code}/v20\" \"requestBody\" for PATCH verb is empty", requestBody.isEmpty());

        Map<String, Object> patchRequestContentAnnotation = (Map<String, Object>) requestBody.get("content");
        Assert.assertFalse(
                "\"/districts/{code}/v20\" \"requestBody\" for PATCH verb has empty \"content\" property",
                patchRequestContentAnnotation.isEmpty());

        Map<String, Object> patchRequestContentTypeJson = (Map<String, Object>) patchRequestContentAnnotation
                .get("application/json");
        Assert.assertFalse(
                "\"/districts/{code}/v20\" \"response\" for PATCH verb has \"content\" but empty \"application/json\" property",
                patchRequestContentTypeJson.isEmpty());

        Map<String, Object> requestBodySchema = (Map<String, Object>) patchRequestContentTypeJson.get("schema");
        Assert.assertFalse(
                "\"/districts/{code}/v20\" \"response\" for GET verb has \"application/json\" but empty \"schema\" property",
                requestBodySchema.isEmpty());

        Assert.assertEquals("Specified schema for the request body is not equal to DistrictEntity",
                "#/components/schemas/DistrictEntity", requestBodySchema.get("$ref"));

        // 2.0 The @APIResponseSchema annotation has been added to provide a shorthand mechanism to specify the schema for a response body
        Map<String, Object> patchResponses = (Map<String, Object>) patchMethod.get("responses");
        Assert.assertFalse("\"/districts/{code}/v20\" \"responses\" for PATCH verb is empty", patchResponses.isEmpty());

        Map<String, Object> patchHttp200Response = (Map<String, Object>) patchResponses.get("200");
        Assert.assertFalse("\"/districts/{code}/v20\" \"response\" for PATCH verb and HTTP status 200 is empty",
                patchHttp200Response.isEmpty());

        Map<String, Object> patchResponseContentAnnotation = (Map<String, Object>) patchHttp200Response.get("content");
        Assert.assertFalse(
                "\"/districts/{code}/v20\" \"response\" for PATCH verb and HTTP status 200 has empty \"content\" property",
                patchResponseContentAnnotation.isEmpty());

        Map<String, Object> patchResponseContentTypeJson = (Map<String, Object>) patchResponseContentAnnotation
                .get("application/json");
        Assert.assertFalse(
                "\"/districts/{code}/v20\" \"response\" for PATCH verb and HTTP status 200 has \"content\" but empty \"application/json\" property",
                patchResponseContentTypeJson.isEmpty());

        Map<String, Object> responseSchema = (Map<String, Object>) patchResponseContentTypeJson.get("schema");
        Assert.assertFalse(
                "\"/districts/{code}/v20\" \"response\" for PATCH verb has \"application/json\" but empty \"schema\" property",
                responseSchema.isEmpty());

        Assert.assertEquals("Specified schema for the response body is not equal to DistrictEntity",
                "#/components/schemas/DistrictEntity", responseSchema.get("$ref"));

        // 2.0 The mp.openapi.schema.* property - allow the schema for a specific class to be specified.

        // let's check whether there is a MockString schema in "components" property
        Map<String, Object> components = (Map<String, Object>) yamlMap.get("components");
        Assert.assertFalse("\"components\" property is empty", components.isEmpty());

        Map<String, Object> schemas = (Map<String, Object>) components.get("schemas");
        Assert.assertFalse("\"schemas\" property is empty", schemas.isEmpty());

        Map<String, Object> mockString = (Map<String, Object>) schemas.get("MockString");

        Assert.assertEquals("Schema description for String class has an unexpected value",
                "Mock custom String class defined with config", mockString.get("description"));
        Assert.assertEquals("Schema type for String class has an unexpected value", "string", mockString.get("type"));

        // let's check whether /districts/{code} response for GET verb and HTTP status 200 has MockString reference in it's DistrictEntity scheme "code" property value
        Map<String, Object> codeProperty = (Map<String, Object>) schemaProperties.get("code");

        Assert.assertEquals(
                "\"/districts/{code}\" \"response\" for GET verb and HTTP status 200 has unexpected value for schema \"code\" property",
                "#/components/schemas/MockString",
                codeProperty.get("$ref"));
    }
}
