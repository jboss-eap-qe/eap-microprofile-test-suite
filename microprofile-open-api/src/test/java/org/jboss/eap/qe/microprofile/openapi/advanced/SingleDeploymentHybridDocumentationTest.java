package org.jboss.eap.qe.microprofile.openapi.advanced;

import java.net.URISyntaxException;
import java.net.URL;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.RoutingServiceConstants;
import org.jboss.eap.qe.microprofile.openapi.model.OpenApiFilter;
import org.jboss.eap.qe.microprofile.openapi.model.OpenApiModelReader;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Extends {@link HybridDocumentationTest} to verify hybrid OpenAPI documentation in a single deployment scenario.
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({ HybridDocumentationTest.OpenApiExtensionSetup.class })
public class SingleDeploymentHybridDocumentationTest extends HybridDocumentationTest {

    /**
     * @tpTestDetails Integration test to verify MP Rest Client usage by Local Service Router JAX-RS resource
     * @tpPassCrit One of the Local Service Router endpoints is called to retrieve data from Service Provider,
     *             verifying the HTTP response code and content type
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testRoutedEndpoint(@ArquillianResource @OperateOnDeployment(ROUTER_DEPLOYMENT_NAME) URL baseURL) {
        verifyRoutedEndpoint(baseURL);
    }

    /**
     * @tpTestDetails Verifies proper processing by assessing that information stored in non-routed services
     *                annotations is preserved in final document
     * @tpPassCrit The generated document contains a string that uniquely identifies one of the Local Service Router
     *             endpoints URL and that was generated from local JAX-RS resources annotation
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testNonRoutedEndpoint(
            @ArquillianResource @OperateOnDeployment(ROUTER_DEPLOYMENT_NAME) URL baseURL) {
        verifyNonRoutedEndpoint(baseURL);
    }

    /**
     * @tpTestDetails Test to verify that static file has been used for OpenAPI document generation
     * @tpPassCrit The generated document does contain a string which uniquely identifies one of the routed Service
     *             Provider endpoints URL
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testExpectedStaticFileInformationInOpenApiDoc(
            @ArquillianResource @OperateOnDeployment(ROUTER_DEPLOYMENT_NAME) URL baseURL) throws URISyntaxException {
        verifyExpectedStaticFileInformationInOpenApiDoc(baseURL);
    }

    /**
     * @tpTestDetails Verifies proper processing by using MP OpenAPI programming model to filter the generated document
     * @tpPassCrit Verifies that {@link RoutingServiceConstants#OPENAPI_OPERATION_EXTENSION_PROXY_FQDN_NAME)} extension
     *             value has been successfully modified by {@link OpenApiFilter}
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testOpenApiDocumentForRouterFqdnExtensionModification(
            @ArquillianResource @OperateOnDeployment(ROUTER_DEPLOYMENT_NAME) URL baseURL)
            throws URISyntaxException {
        verifyOpenApiDocumentForRouterFqdnExtensionModification(baseURL, OpenApiFilter.LOCAL_TEST_ROUTER_FQDN);
    }

    /**
     * @tpTestDetails Verifies proper processing by using MP OpenAPI programming model to create a custom base document
     * @tpPassCrit Verifies that {@link OpenAPI#getInfo()} has been successfully modified by {@link OpenApiModelReader}
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testOpenApiDocumentForOpenApiInfoChange(
            @ArquillianResource @OperateOnDeployment(ROUTER_DEPLOYMENT_NAME) URL baseURL) throws URISyntaxException {
        verifyOpenApiDocumentForOpenApiInfoChange(baseURL, true);
    }
}
