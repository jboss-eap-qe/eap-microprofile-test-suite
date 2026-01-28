package org.jboss.eap.qe.microprofile.openapi.advanced;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.RoutingServiceConstants;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.router.RouterApplication;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.router.model.DistrictObject;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.router.rest.LocalServiceRouterInfoResource;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.router.rest.routed.RouterDistrictsResource;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.router.services.DistrictServiceClient;
import org.jboss.eap.qe.microprofile.openapi.model.AnotherOpenApiFilter;
import org.jboss.eap.qe.microprofile.openapi.model.AnotherOpenApiModelReader;
import org.jboss.eap.qe.microprofile.openapi.model.OpenApiFilter;
import org.jboss.eap.qe.microprofile.openapi.model.OpenApiModelReader;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.ConfigurationException;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientRelatedException;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.deployment.ConfigurationUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Extends {@link HybridDocumentationTest} to verify hybrid OpenAPI documentation in a multiple deployments scenario.
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({ HybridDocumentationTest.OpenApiExtensionSetup.class })
public class MultipleDeploymentsHybridDocumentationTest extends HybridDocumentationTest {

    private final static String ANOTHER_ROUTER_DEPLOYMENT_NAME = "anotherLocalServicesRouterDeployment";

    @Deployment(name = ANOTHER_ROUTER_DEPLOYMENT_NAME, order = 3, testable = false)
    public static Archive<?> anotherLocalServicesRouterDeployment()
            throws ConfigurationException, IOException, ManagementClientRelatedException {

        String mpConfigProperties = buildMicroProfileConfigProperties(
                AnotherOpenApiModelReader.class.getSimpleName(), AnotherOpenApiFilter.class.getSimpleName());

        return ShrinkWrap.create(
                WebArchive.class, ANOTHER_ROUTER_DEPLOYMENT_NAME + ".war")
                .addClasses(
                        RouterApplication.class,
                        LocalServiceRouterInfoResource.class,
                        DistrictObject.class,
                        RouterDistrictsResource.class,
                        DistrictServiceClient.class,
                        AnotherOpenApiModelReader.class,
                        AnotherOpenApiFilter.class)
                .addAsManifestResource(new StringAsset(mpConfigProperties), "microprofile-config.properties")
                .addAsResource("META-INF/openapi.yaml")
                .addAsWebInfResource(ConfigurationUtil.BEANS_XML_FILE_LOCATION, "beans.xml");
    }

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
     *             value has been successfully modified by {@link OpenApiFilter}, relatively to each deployment context
     *             path.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testOpenApiDocumentForRouterFqdnExtensionModification(
            @ArquillianResource @OperateOnDeployment(ROUTER_DEPLOYMENT_NAME) URL baseURL)
            throws URISyntaxException {
        verifyOpenApiDocumentForRouterFqdnExtensionModification(baseURL, OpenApiFilter.LOCAL_TEST_ROUTER_FQDN);
        verifyOpenApiDocumentForRouterFqdnExtensionModification(baseURL, AnotherOpenApiFilter.ANOTEHR_LOCAL_TEST_ROUTER_FQDN);
    }

    /**
     * @tpTestDetails Verifies proper processing by using MP OpenAPI programming model to create a custom base document
     * @tpPassCrit Verifies that {@link OpenAPI#getInfo()}, which is a global <i>discordant</i> property, was not
     *             modified by {@link OpenApiModelReader}
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testOpenApiDocumentForOpenApiInfoChange(
            @ArquillianResource @OperateOnDeployment(ROUTER_DEPLOYMENT_NAME) URL baseURL) throws URISyntaxException {
        verifyOpenApiDocumentForOpenApiInfoChange(baseURL, false);
    }
}
