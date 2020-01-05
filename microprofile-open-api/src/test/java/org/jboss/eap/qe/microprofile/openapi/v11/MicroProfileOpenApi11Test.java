package org.jboss.eap.qe.microprofile.openapi.v11;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.comparesEqualTo;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;

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
 * Test cases for MP OpenAPI v. 1.1 annotations on JAX-RS components (migrated from TT QE TS)
 */
@RunWith(Arquillian.class)
@ServerSetup({ MicroProfileOpenApi11Test.OpenApiExtensionSetup.class })
@RunAsClient
public class MicroProfileOpenApi11Test {
    private final static String DEPLOYMENT_NAME = MicroProfileOpenApi11Test.class.getSimpleName();

    static OnlineManagementClient onlineManagementClient;

    @Deployment(testable = false)
    public static Archive<?> deployment() {
        WebArchive deployment = ShrinkWrap.create(WebArchive.class, String.format("%s.war", DEPLOYMENT_NAME))
                .addClasses(
                        ProviderApplication.class,
                        District.class,
                        DistrictEntity.class,
                        DistrictService.class,
                        InMemoryDistrictService.class,
                        DistrictsResource.class,
                        RoutingServiceConstants.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return deployment;
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

    /**
     * @tpTestDetails Tests for an application PATCH endpoint to be working
     * @tpPassCrit Calls application PATCH endpoint twice to modify and then reset a district data, checking for
     *             expected properties values in returned data
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testAppEndpoint(@ArquillianResource URL baseURL) {
        String patchTestURL = baseURL.toExternalForm() + "districts/DW";
        given()
                .header("Accept", MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .body(new DistrictEntity("DW", "Western district - OBS", Boolean.TRUE)).when()
                .patch(patchTestURL)
                .then()
                .statusCode(200)
                .body("code", comparesEqualTo("DW"),
                        "obsolete", comparesEqualTo(Boolean.TRUE),
                        "name", comparesEqualTo("Western district - OBS"));
        given()
                .header("Accept", MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .body(new DistrictEntity("DW", "Western district", Boolean.FALSE)).when()
                .patch(patchTestURL)
                .then()
                .statusCode(200)
                .body("code", comparesEqualTo("DW"),
                        "obsolete", comparesEqualTo(Boolean.FALSE),
                        "name", comparesEqualTo("Western district"));
    }

    /**
     * @tpTestDetails Tests for proper OpenAPI documentation of JAX-RS application endpoint and MP OpenAPI v. 1.1
     *                annotations
     * @tpPassCrit Verifies that generated document contains information for v 1.1 features:<br>
     *             - the addition of the JAXRS 2.1 PATCH method <br>
     *             - Content now supports a singular example field <br>
     *             - Extension now has a parseValue field for complex values
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testOpenApiDocumentForV11Annotations(@ArquillianResource URL baseURL) throws URISyntaxException {
        String responseContent = get(baseURL.toURI().resolve("/openapi")).then()
                .statusCode(200)
                .extract().asString();
        Yaml yaml = new Yaml();
        Object yamlObject = yaml.load(responseContent);
        Map<String, Object> yamlMap = (Map<String, Object>) yamlObject;
        Map<String, Object> paths = (Map<String, Object>) yamlMap.get("paths");
        Assert.assertFalse("\"paths\" property is empty", paths.isEmpty());

        Map<String, Object> getDistrictByCodePath = (Map<String, Object>) paths.get("/districts/{code}");
        Assert.assertFalse("\"/districts/{code}\" property is empty", getDistrictByCodePath.isEmpty());

        // 1.1 the addition of the JAXRS 2.1 PATCH method
        Map<String, Object> patchMethod = (Map<String, Object>) getDistrictByCodePath.get("patch");
        Assert.assertFalse("\"/districts/{code}\" \"patch\" property is empty", patchMethod.isEmpty());
        Assert.assertEquals("\"/districts/{code}\" \"patch\" property should have exactly 12 keys",
                patchMethod.keySet().size(), 12);

        // 1.1 @Content now supports a singular example field
        Map<String, Object> responses = (Map<String, Object>) patchMethod.get("responses");
        Assert.assertFalse("\"/districts/{code}\" \"responses\" for PATCH verb is empty", responses.isEmpty());

        Map<String, Object> http200Response = (Map<String, Object>) responses.get("200");
        Assert.assertFalse("\"/districts/{code}\" \"response\" for PATCH verb and HTTP status 200 is empty",
                http200Response.isEmpty());

        Map<String, Object> contentAnnotation = (Map<String, Object>) http200Response.get("content");
        Assert.assertFalse(
                "\"/districts/{code}\" \"response\" for PATCH verb and HTTP status 200 has empty \"content\" property",
                contentAnnotation.isEmpty());

        Map<String, Object> contentTypeJson = (Map<String, Object>) contentAnnotation.get("application/json");
        Assert.assertFalse(
                "\"/districts/{code}\" \"response\" for PATCH verb and HTTP status 200 has \"content\" but empty \"application/json\" property",
                contentTypeJson.isEmpty());
        Assert.assertTrue(
                "\"/districts/{code}\" \"response\" for PATCH verb and HTTP status 200 has \"content\" but unexpected value for \"example\" property",
                contentTypeJson.get("example").equals("{'code': 'DW', 'name': 'Western district', 'obsolete': false}"));

        // 1.1 @Extension now has a parseValue field for complex values
        Assert.assertTrue("\"x-string-property\" has unexpected value",
                patchMethod.get("x-string-property").equals("string-value"));
        Assert.assertTrue("\"x-boolean-property\" has unexpected value", patchMethod.get("x-boolean-property").equals(true));
        Assert.assertTrue("\"x-number-property\" has unexpected value", patchMethod.get("x-number-property").equals(42));

        Map<String, Object> objectProperty = (Map<String, Object>) patchMethod.get("x-object-property");
        Assert.assertNotNull("\"x-object-property\" is null", objectProperty);
        Assert.assertTrue("\"x-object-property\" has unexpected value for \"property-1\"",
                objectProperty.get("property-1").equals("value-1"));
        Assert.assertTrue("\"x-object-property\" has unexpected value for \"property-2\"",
                objectProperty.get("property-2").equals("value-2"));
        Assert.assertTrue("\"x-object-property\" has unexpected value for \"property-3::prop-3-1\" field",
                ((Map<String, Object>) objectProperty.get("property-3")).get("prop-3-1").equals(42));
        Assert.assertTrue("\"x-object-property\" has unexpected value for \"property-3::prop-3-2\" field",
                ((Map<String, Object>) objectProperty.get("property-3")).get("prop-3-2").equals(true));

        Assert.assertTrue(
                "\"x-string-array-property\" does not contains all expected values",
                ((List<String>) patchMethod.get("x-string-array-property")).containsAll(Arrays.asList("one", "two", "three")));

        List<Object> objectArrayProperty = (List<Object>) patchMethod.get("x-object-array-property");
        Assert.assertTrue(
                "\"x-object-array-property\" should have exactly 2 items", objectArrayProperty.size() == 2);
        Assert.assertTrue("\"x-object-array-property\" item [0] has unexpected value for \"name\"",
                ((Map<String, String>) objectArrayProperty.get(0)).get("name").equals("item-1"));
        Assert.assertTrue("\"x-object-array-property\" item [1] has unexpected value for \"name\"",
                ((Map<String, String>) objectArrayProperty.get(1)).get("name").equals("item-2"));
    }
}
