package org.jboss.eap.qe.microprofile.openapi.advanced;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;

import java.net.URISyntaxException;
import java.net.URL;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
import org.jboss.eap.qe.microprofile.openapi.v10.OpenApi10OnJaxRsAnnotationsTest;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.MicroProfileServerSetupTask;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;

/**
 * Test cases to verify negative scenario in which "Accept" HTTP header and {@code format} query parameter are
 * conflicting or not valid.
 * {@link MediaType} constants are used as HTTP "Accept" header values.
 */
@RunWith(Arquillian.class)
@ServerSetup({ AcceptHeadersAndFormatParameterInteractionTest.OpenApiExtensionSetup.class })
@RunAsClient
public class AcceptHeadersAndFormatParameterInteractionTest {

    private final static String DEPLOYMENT_NAME = OpenApi10OnJaxRsAnnotationsTest.class.getSimpleName();

    @Deployment(testable = false)
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(
                WebArchive.class,
                String.format("%s.war", DEPLOYMENT_NAME))
                .addClasses(
                        ProviderApplication.class,
                        District.class,
                        DistrictEntity.class,
                        DistrictService.class,
                        InMemoryDistrictService.class,
                        DistrictsResource.class,
                        RoutingServiceConstants.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
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
     * @tpTestDetails Sends HTTP request with {@link MediaType#APPLICATION_JSON} {@code Accept} header and no {@code format}
     *                query parameter.
     * @tpPassCrit Despite missing {@code format} query parameter, the right Content Type is returned as per
     *             MicroProfile OpenAPI spec.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testResponseJsonContentTypeWhenAcceptHeaderAndNoQueryParameter(@ArquillianResource URL baseURL)
            throws URISyntaxException {
        given()
                .header("Accept", MediaType.APPLICATION_JSON)
                .get(baseURL.toURI().resolve("/openapi"))
                .then()
                .statusCode(200)
                .contentType(MediaType.APPLICATION_JSON);
    }

    /**
     * @tpTestDetails Sends HTTP request with {@code format} query parameter.
     * @tpPassCrit The {@code format} query parameter is used and the right Content Type is returned as per
     *             MicroProfile OpenAPI spec.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testResponseJsonContentTypeWhenJsonQueryParameter(@ArquillianResource URL baseURL) throws URISyntaxException {
        get(baseURL.toURI().resolve("/openapi?format=JSON"))
                .then()
                .statusCode(200)
                .contentType(MediaType.APPLICATION_JSON);
    }

    /**
     * @tpTestDetails Sends HTTP request with {@link MediaType#APPLICATION_JSON} {@code Accept} header and {@code YAML} value
     *                for {@code format} query parameter.
     * @tpPassCrit Despite conflicting information is sent to server through {@code format} query parameter,
     *             the right Content Type is returned as per MicroProfile OpenAPI spec.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testResponseJsonContentTypeWhenAcceptHeaderAndYamlQueryParameter(@ArquillianResource URL baseURL)
            throws URISyntaxException {
        given()
                .header("Accept", MediaType.APPLICATION_JSON)
                .get(baseURL.toURI().resolve("/openapi?format=YAML"))
                .then()
                .statusCode(200)
                .contentType(MediaType.APPLICATION_JSON);
    }

    /**
     * @tpTestDetails Sends HTTP request with {@link MediaType#TEXT_PLAIN} {@code Accept} header and no {@code format} query
     *                parameter.
     * @tpPassCrit As unexpected information is sent to server through the "Accept" HTTP header,
     *             {@link javax.ws.rs.core.Response.Status#NOT_ACCEPTABLE} status code is returned by the server.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testResponseYamlContentTypeWhenTextPlainAcceptHeaderAndNoQueryParameter(@ArquillianResource URL baseURL)
            throws URISyntaxException {
        given()
                .header("Accept", MediaType.TEXT_PLAIN)
                .get(baseURL.toURI().resolve("/openapi"))
                .then()
                .statusCode(Response.Status.NOT_ACCEPTABLE.getStatusCode());
    }

    /**
     * @tpTestDetails Sends HTTP request with {@link MediaType#TEXT_XML} {@code Accept} header and no {@code format} query
     *                parameter.
     * @tpPassCrit As unexpected information is sent to server through the "Accept" HTTP header,
     *             {@link javax.ws.rs.core.Response.Status#NOT_ACCEPTABLE} status code is returned by the server.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testResponseYamlContentTypeWhenTextXmlAcceptHeaderAndNoQueryParameter(@ArquillianResource URL baseURL)
            throws URISyntaxException {
        given()
                .header("Accept", MediaType.TEXT_XML)
                .get(baseURL.toURI().resolve("/openapi"))
                .then()
                .statusCode(Response.Status.NOT_ACCEPTABLE.getStatusCode());
    }

    /**
     * @tpTestDetails Sends HTTP request with {@link MediaType#TEXT_HTML} {@code Accept} header and no {@code format} query
     *                parameter.
     * @tpPassCrit As unexpected information is sent to server through the "Accept" HTTP header,
     *             {@link javax.ws.rs.core.Response.Status#NOT_ACCEPTABLE} status code is returned by the server.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testResponseYamlContentTypeWhenTextHtmlAcceptHeaderAndNoQueryParameter(@ArquillianResource URL baseURL)
            throws URISyntaxException {
        given()
                .header("Accept", MediaType.TEXT_HTML)
                .get(baseURL.toURI().resolve("/openapi"))
                .then()
                .statusCode(Response.Status.NOT_ACCEPTABLE.getStatusCode());
    }

    /**
     * @tpTestDetails Sends HTTP request with {@link MediaType#APPLICATION_XML} {@code Accept} header and no {@code format}
     *                query parameter.
     * @tpPassCrit As unexpected information is sent to server through the "Accept" HTTP header,
     *             {@link javax.ws.rs.core.Response.Status#NOT_ACCEPTABLE} status code is returned by the server.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testResponseYamlContentTypeWhenApplicationXmlAcceptHeaderAndNoQueryParameter(@ArquillianResource URL baseURL)
            throws URISyntaxException {
        given()
                .header("Accept", MediaType.APPLICATION_XML)
                .get(baseURL.toURI().resolve("/openapi"))
                .then()
                .statusCode(Response.Status.NOT_ACCEPTABLE.getStatusCode());
    }

    /**
     * @tpTestDetails Sends HTTP request with {@link MediaType#APPLICATION_ATOM_XML} {@code Accept} header and no {@code format}
     *                query parameter.
     * @tpPassCrit As unexpected information is sent to server through the "Accept" HTTP header,
     *             {@link javax.ws.rs.core.Response.Status#NOT_ACCEPTABLE} status code is returned by the server.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testResponseYamlContentTypeWhenAtomXmlAcceptHeaderAndNoQueryParameter(@ArquillianResource URL baseURL)
            throws URISyntaxException {
        given()
                .header("Accept", MediaType.APPLICATION_ATOM_XML)
                .get(baseURL.toURI().resolve("/openapi"))
                .then()
                .statusCode(Response.Status.NOT_ACCEPTABLE.getStatusCode());
    }

    /**
     * @tpTestDetails Sends HTTP request with {@link MediaType#APPLICATION_XHTML_XML} {@code Accept} header and no
     *                {@code format} query parameter.
     * @tpPassCrit As unexpected information is sent to server through the "Accept" HTTP header,
     *             {@link javax.ws.rs.core.Response.Status#NOT_ACCEPTABLE} status code is returned by the server.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testResponseYamlContentTypeWhenXhtmlXmlAcceptHeaderAndNoQueryParameter(@ArquillianResource URL baseURL)
            throws URISyntaxException {
        given()
                .header("Accept", MediaType.APPLICATION_XHTML_XML)
                .get(baseURL.toURI().resolve("/openapi"))
                .then()
                .statusCode(Response.Status.NOT_ACCEPTABLE.getStatusCode());
    }

    /**
     * @tpTestDetails Sends HTTP request with {@link MediaType#APPLICATION_SVG_XML} {@code Accept} header and no {@code format}
     *                query parameter.
     * @tpPassCrit As unexpected information is sent to server through the "Accept" HTTP header,
     *             {@link javax.ws.rs.core.Response.Status#NOT_ACCEPTABLE} status code is returned by the server.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testResponseYamlContentTypeWhenSvgXmlAcceptHeaderAndNoQueryParameter(@ArquillianResource URL baseURL)
            throws URISyntaxException {
        given()
                .header("Accept", MediaType.APPLICATION_SVG_XML)
                .get(baseURL.toURI().resolve("/openapi"))
                .then()
                .statusCode(Response.Status.NOT_ACCEPTABLE.getStatusCode());
    }

    /**
     * @tpTestDetails Sends HTTP request with {@link MediaType#SERVER_SENT_EVENTS} {@code Accept} header and no {@code format}
     *                query parameter.
     * @tpPassCrit As unexpected information is sent to server through the "Accept" HTTP header,
     *             {@link javax.ws.rs.core.Response.Status#NOT_ACCEPTABLE} status code is returned by the server.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testResponseYamlContentTypeWhenTextStreamAcceptHeaderAndNoQueryParameter(@ArquillianResource URL baseURL)
            throws URISyntaxException {
        given()
                .header("Accept", MediaType.SERVER_SENT_EVENTS)
                .get(baseURL.toURI().resolve("/openapi"))
                .then()
                .statusCode(Response.Status.NOT_ACCEPTABLE.getStatusCode());
    }

    /**
     * @tpTestDetails Sends HTTP request with {@link MediaType#APPLICATION_OCTET_STREAM} {@code Accept} header and no
     *                {@code format}
     *                query parameter.
     * @tpPassCrit As unexpected information is sent to server through the "Accept" HTTP header,
     *             {@link javax.ws.rs.core.Response.Status#NOT_ACCEPTABLE} status code is returned by the server.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testResponseYamlContentTypeWhenApplicationStreamAcceptHeaderAndNoQueryParameter(@ArquillianResource URL baseURL)
            throws URISyntaxException {
        given()
                .header("Accept", MediaType.APPLICATION_OCTET_STREAM)
                .get(baseURL.toURI().resolve("/openapi"))
                .then()
                .statusCode(Response.Status.NOT_ACCEPTABLE.getStatusCode());
    }

    /**
     * @tpTestDetails Sends HTTP request with {@link MediaType#APPLICATION_FORM_URLENCODED} {@code Accept} header and no
     *                {@code format}
     *                query parameter.
     * @tpPassCrit As unexpected information is sent to server through the "Accept" HTTP header,
     *             {@link javax.ws.rs.core.Response.Status#NOT_ACCEPTABLE} status code is returned by the server.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testResponseYamlContentTypeWhenUrlEncodedFormAcceptHeaderAndNoQueryParameter(
            @ArquillianResource URL baseURL)
            throws URISyntaxException {
        given()
                .header("Accept", MediaType.APPLICATION_FORM_URLENCODED)
                .get(baseURL.toURI().resolve("/openapi"))
                .then()
                .statusCode(Response.Status.NOT_ACCEPTABLE.getStatusCode());
    }

    /**
     * @tpTestDetails Sends HTTP request with {@link MediaType#MULTIPART_FORM_DATA} {@code Accept} header and no {@code format}
     *                query parameter.
     * @tpPassCrit As unexpected information is sent to server through the "Accept" HTTP header,
     *             {@link javax.ws.rs.core.Response.Status#NOT_ACCEPTABLE} status code is returned by the server.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testResponseYamlContentTypeWhenMultipartFormDataAcceptHeaderAndNoQueryParameter(
            @ArquillianResource URL baseURL)
            throws URISyntaxException {
        given()
                .header("Accept", MediaType.MULTIPART_FORM_DATA)
                .get(baseURL.toURI().resolve("/openapi"))
                .then()
                .statusCode(Response.Status.NOT_ACCEPTABLE.getStatusCode());
    }

    /**
     * @tpTestDetails Sends HTTP request with {@code application/*} {@code Accept} header and no {@code format} query
     *                parameter.
     * @tpPassCrit As unexpected information is sent to server through the "Accept" HTTP header,
     *             {@link javax.ws.rs.core.Response.Status#NOT_ACCEPTABLE} status code is returned by the server.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testResponseYamlContentTypeWhenWildcardAcceptHeaderAndNoQueryParameter(
            @ArquillianResource URL baseURL)
            throws URISyntaxException {
        given()
                .header("Accept", "application/*")
                .get(baseURL.toURI().resolve("/openapi"))
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType("application/yaml");
    }

    /**
     * @tpTestDetails Sends HTTP request with {@link MediaType#APPLICATION_JSON_PATCH_JSON} {@code Accept} header and no
     *                {@code format}
     *                query parameter.
     * @tpPassCrit As unexpected information is sent to server through the "Accept" HTTP header,
     *             {@link javax.ws.rs.core.Response.Status#NOT_ACCEPTABLE} status code is returned by the server.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testResponseYamlContentTypeWhenJsonPatchJsonAcceptHeaderAndNoQueryParameter(
            @ArquillianResource URL baseURL)
            throws URISyntaxException {
        given()
                .header("Accept", MediaType.APPLICATION_JSON_PATCH_JSON)
                .get(baseURL.toURI().resolve("/openapi"))
                .then()
                .statusCode(Response.Status.NOT_ACCEPTABLE.getStatusCode());
    }
}
