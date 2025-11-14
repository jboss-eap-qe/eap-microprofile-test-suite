package org.jboss.eap.qe.microprofile.openapi.util;

import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.equalToIgnoringCase;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

import io.restassured.response.ValidatableResponse;

/**
 * Provides utility methods for MicroProfile OpenAPI tests
 */
public class MicroProfileOpenApiTestUtils {

    /**
     * Get a {@link ValidatableResponse} instance as the result of a call to a MicroProfile OpenAPI endpoint
     *
     * @param baseUrl The {@link URL} instance representing the MicroProfile OpenAPI endpoint to be called
     * @return A {@link ValidatableResponse} instance holding the result of a call to a MicroProfile OpenAPI endpoint
     * @throws URISyntaxException
     */
    public static ValidatableResponse getGeneratedOpenApi(final URL baseUrl) throws URISyntaxException {
        return get(baseUrl.toURI().resolve("/openapi"))
                .then()
                .statusCode(200)
                .contentType(equalToIgnoringCase("application/yaml"));
    }

    /**
     * Extracts the {@code jsonSchemaDialect} element value from the generated OpenAPI document
     *
     * @param openApiYamlContents The contents of the OpenAPI document, in YAML format
     * @return The value of the {@code jsonSchemaDialect} element
     */
    @SuppressWarnings("unchecked")
    public static String getGeneratedJsonSchemaDialect(final String openApiYamlContents) {
        final Yaml yaml = new Yaml();
        final Object yamlObject = yaml.load(openApiYamlContents);

        final Map<String, Object> yamlMap = (Map<String, Object>) yamlObject;
        return (String) yamlMap.get("jsonSchemaDialect");
    }

    /**
     * Extracts a root element value from the generated OpenAPI document
     *
     * @param openApiYamlContents The contents of the OpenAPI document, in YAML format
     * @param element The root element which value should be extracted
     * @return The value of the given root element
     */
    @SuppressWarnings("unchecked")
    public static <T> T getGeneratedRootElement(String openApiYamlContents, String element) {
        Yaml yaml = new Yaml();
        Object yamlObject = yaml.load(openApiYamlContents);
        Map<String, Object> yamlMap = (Map<String, Object>) yamlObject;
        return (T) yamlMap.get(element);
    }

    /**
     * Extracts the value of an element belonging to the {@code .components} OpenAPI document element
     *
     * @param openApiYamlContents The contents of the OpenAPI document, in YAML format
     * @param element The element, belonging to the {@code .components} element, which value should be extracted
     * @return The value of the given {@code .components} child element
     */
    @SuppressWarnings("unchecked")
    public static <T> T getGeneratedComponentElement(final String openApiYamlContents, final String element) {
        Yaml yaml = new Yaml();
        Object yamlObject = yaml.load(openApiYamlContents);
        Map<String, Object> yamlMap = (Map<String, Object>) yamlObject;
        Map<String, Object> components = (Map<String, Object>) yamlMap.get("components");
        return (T) components.get(element);
    }
}
