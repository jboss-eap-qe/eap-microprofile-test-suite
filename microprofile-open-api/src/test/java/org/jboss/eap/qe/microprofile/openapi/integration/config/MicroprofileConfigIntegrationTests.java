package org.jboss.eap.qe.microprofile.openapi.integration.config;

import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalToIgnoringCase;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
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
import org.jboss.eap.qe.microprofile.openapi.apps.routing.router.RouterApplication;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.router.model.DistrictObject;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.router.rest.LocalServiceRouterInfoResource;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.router.rest.routed.RouterDistrictsResource;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.router.services.DistrictServiceClient;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.ConfigurationException;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.ArquillianContainerProperties;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.ArquillianDescriptorWrapper;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.MicroProfileServerSetupTask;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientRelatedException;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.deployment.ConfigurationUtil;
import org.jboss.eap.qe.microprofile.tooling.server.log.ModelNodeLogChecker;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.yaml.snakeyaml.Yaml;

/**
 * Test cases for MP OpenAPI and MP Config integration
 */
@RunWith(Arquillian.class)
@ServerSetup({ MicroprofileConfigIntegrationTests.OpenApiExtensionSetup.class })
@RunAsClient
public class MicroprofileConfigIntegrationTests {

    private final static String PROVIDER_DEPLOYMENT_NAME = "serviceProviderDeployment";
    private final static String ROUTER_DEPLOYMENT_NAME = "localServicesRouterTestDeployment";
    private final static String BADLY_CONFIGURED_ROUTER_DEPLOYMENT_NAME = "localServicesRouterBadlyConfiguredDeployment";
    private final static String SCAN_DISABLING_ROUTER_DEPLOYMENT_NAME = "localServicesRouterScanDisablingDeployment";

    private static ArquillianContainerProperties arquillianContainerProperties = new ArquillianContainerProperties(
            ArquillianDescriptorWrapper.getArquillianDescriptor());

    @Deployment(name = PROVIDER_DEPLOYMENT_NAME, order = 1, testable = false)
    public static Archive<?> serviceProviderDeployment() {
        //  This is the Services Provider deployment with default configuration and simulates the main deployment in
        //  the current scenario - i.e. the one that is run by Services Provider. Following deployments
        //  are used to demonstrate advanced MP Config (and vendor extension properties) integration features.
        return ShrinkWrap.create(WebArchive.class, PROVIDER_DEPLOYMENT_NAME + ".war")
                .addClasses(
                        ProviderApplication.class,
                        District.class,
                        DistrictEntity.class,
                        DistrictService.class,
                        InMemoryDistrictService.class,
                        DistrictsResource.class,
                        RoutingServiceConstants.class)
                .addAsWebInfResource(ConfigurationUtil.BEANS_XML_FILE_LOCATION, "beans.xml");
    }

    @Deployment(name = ROUTER_DEPLOYMENT_NAME, order = 2, testable = false)
    public static Archive<?> localServicesRouterTestDeployment()
            throws ConfigurationException, IOException, ManagementClientRelatedException {

        int configuredHTTPPort;
        try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
            configuredHTTPPort = OpenApiServerConfiguration.getHTTPPort(client);
        }

        //  OpenAPI doc here is configured for the second deployment to have a different path from the one
        //  defined by standard spec and to replace the value for Server records with a custom (local) one.
        //  Here we also add config properties to reach the Services Provider
        String mpConfigProperties = "mp.openapi.scan.disable=false"
                + "\n" +
                "mp.openapi.servers=https://xyz.io/v1"
                + "\n" +
                "mp.openapi.extensions.path=/local/openapi"
                + "\n" +
                "services.provider.host=" + arquillianContainerProperties.getDefaultManagementAddress()
                + "\n" +
                String.format("services.provider.port=%d", configuredHTTPPort);

        return ShrinkWrap.create(
                WebArchive.class, ROUTER_DEPLOYMENT_NAME + ".war")
                .addClasses(
                        RouterApplication.class,
                        LocalServiceRouterInfoResource.class,
                        DistrictObject.class,
                        RouterDistrictsResource.class,
                        DistrictServiceClient.class)
                .addAsManifestResource(new StringAsset(mpConfigProperties), "microprofile-config.properties")
                .addAsWebInfResource(ConfigurationUtil.BEANS_XML_FILE_LOCATION, "beans.xml");
    }

    @Deployment(name = BADLY_CONFIGURED_ROUTER_DEPLOYMENT_NAME, order = 3, testable = false)
    public static Archive<?> localServicesRouterBadlyConfiguredDeployment()
            throws ConfigurationException, IOException, ManagementClientRelatedException {

        int configuredHTTPPort;
        try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
            configuredHTTPPort = OpenApiServerConfiguration.getHTTPPort(client);
        }

        //  OpenAPI doc here is badly configured for the third deployment - i.e. no value is provided for a custom
        //  path for "openapi" endpoint, in order to assess that the server is actually logging expected WARN IDs.
        //  Here we also add config properties to reach the Services Provider
        String mpConfigProperties = "mp.openapi.scan.disable=false"
                + "\n" +
                "mp.openapi.servers=https://xyz.io/v1"
                + "\n" +
                "services.provider.host=" + arquillianContainerProperties.getDefaultManagementAddress()
                + "\n" +
                String.format("services.provider.port=%d", configuredHTTPPort);

        return ShrinkWrap.create(
                WebArchive.class, BADLY_CONFIGURED_ROUTER_DEPLOYMENT_NAME + ".war")
                .addClasses(
                        RouterApplication.class,
                        LocalServiceRouterInfoResource.class,
                        DistrictObject.class,
                        RouterDistrictsResource.class,
                        DistrictServiceClient.class)
                .addAsManifestResource(new StringAsset(mpConfigProperties), "microprofile-config.properties")
                .addAsWebInfResource(ConfigurationUtil.BEANS_XML_FILE_LOCATION, "beans.xml");
    }

    @Deployment(name = SCAN_DISABLING_ROUTER_DEPLOYMENT_NAME, order = 4, testable = false)
    public static Archive<?> localServicesRouterScanDisablingDeployment()
            throws ConfigurationException, IOException, ManagementClientRelatedException {

        int configuredHTTPPort;
        try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
            configuredHTTPPort = OpenApiServerConfiguration.getHTTPPort(client);
        }
        //  OpenAPI doc here is badly configured for the fourth deployment - i.e. OpenAPI annotations scan is disabled.
        //  This represents a negative scenario because in situation like server reboot it must be verified that
        //  this configuration will not affect other deployments annotations scan and doc generation in general.
        //  The server should log a WARN to inform the user that doc generation was skipped for this deployment -
        //  as no custom path was provided.
        //  Here we also add config properties to reach the Services Provider
        String mpConfigProperties = "mp.openapi.scan.disable=true"
                //                + "\n" +
                //                "mp.openapi.extensions.path=/disabled/openapi"
                + "\n" +
                "services.provider.host=" + arquillianContainerProperties.getDefaultManagementAddress()
                + "\n" +
                String.format("services.provider.port=%d", configuredHTTPPort);

        return ShrinkWrap.create(
                WebArchive.class, SCAN_DISABLING_ROUTER_DEPLOYMENT_NAME + ".war")
                .addClasses(
                        RouterApplication.class,
                        LocalServiceRouterInfoResource.class,
                        DistrictObject.class,
                        RouterDistrictsResource.class,
                        DistrictServiceClient.class)
                .addAsManifestResource(new StringAsset(mpConfigProperties), "microprofile-config.properties")
                .addAsWebInfResource(ConfigurationUtil.BEANS_XML_FILE_LOCATION, "beans.xml");
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
     * @tpTestDetails Integration test to verify usage of MP Config extension property
     *                {@code mp.openapi.extensions.path} by Local Service Router deployments
     * @tpPassCrit The server is logging expected {@code WFLYMPOAI0007} message because a deployment was configured
     *             in order to have a custom url for {@code openapi} endpoint
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testWarningIsLoggedBecauseOfNonConventionalOpenApiUrl() throws ConfigurationException, IOException {
        try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
            ModelNodeLogChecker modelNodeLogChecker = new ModelNodeLogChecker(client, 100);
            Assert.assertTrue(modelNodeLogChecker.logContains("WFLYMPOAI0007"));
        }
    }

    /**
     * @tpTestDetails Integration test to verify that missing MP config {@code mp.openapi.extensions.path} extension
     *                property causes the server to log a warning about document generation being skipped for one given
     *                deployment
     * @tpPassCrit The server is logging expected {@code WFLYMPOAI0003} message because one deployment was not
     *             properly configured in order to have a custom url for {@code openapi} endpoint
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testWarningIsLoggedBecauseOfSkippedBadlyConfiguredDeployment() throws ConfigurationException, IOException {
        try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
            ModelNodeLogChecker modelNodeLogChecker = new ModelNodeLogChecker(client, 100);
            Assert.assertTrue(modelNodeLogChecker.logContains("WFLYMPOAI0003"));
        }
    }

    /**
     * @tpTestDetails Integration test to verify usage of MP Config core property {@code mp.openapi.servers} by
     *                Local Service Router deployments
     * @tpPassCrit The generated OpenAPI document {@code Server Records} contents are overridden by custom value
     *             provided through {@code mp.openapi.servers} property
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testServerRecordsAreOverriddenByConfigProperty(
            @ArquillianResource @OperateOnDeployment(ROUTER_DEPLOYMENT_NAME) URL baseURL)
            throws URISyntaxException {
        get(baseURL.toURI().resolve("/local/openapi"))
                .then()
                .statusCode(200)
                .contentType(equalToIgnoringCase("application/yaml"))
                .body(containsString("url: https://xyz.io/v1"));
    }

    /**
     * @tpTestDetails Integration test to verify usage of MP Config core property {@code mp.openapi.scan.disable} by
     *                Local Service Router deployments
     * @tpPassCrit The generated OpenAPI document {@code paths} property contents are not empty - i.e. the main
     *             deployment related OpenAPI document is generated properly
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testDisabledAnnotationsScanIsNotAffectingPreviouslyDeployedConfiguration(
            @ArquillianResource @OperateOnDeployment(PROVIDER_DEPLOYMENT_NAME) URL baseURL)
            throws URISyntaxException {
        String responseContent = get(baseURL.toURI().resolve("/openapi"))
                .then()
                .statusCode(200)
                .extract().asString();

        Yaml yaml = new Yaml();
        Object yamlObject = yaml.load(responseContent);
        Map<String, Object> yamlMap = (Map<String, Object>) yamlObject;

        Map<String, Object> paths = (Map<String, Object>) yamlMap.get("paths");
        Assert.assertFalse("\"paths\" property is empty", paths.isEmpty());
    }

    /**
     * @tpTestDetails Integration test to verify usage of MP Config core property {@code mp.openapi.scan.disable} by
     *                Local Service Router deployments
     * @tpPassCrit The generated OpenAPI document {@code info} property contents are not empty - i.e. the main
     *             deployment related OpenAPI document is generated properly
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testDisabledAnnotationsScanIsNotAffectingPreviousDeploymentInfo(
            @ArquillianResource @OperateOnDeployment(PROVIDER_DEPLOYMENT_NAME) URL baseURL)
            throws URISyntaxException {
        String responseContent = get(baseURL.toURI().resolve("/openapi"))
                .then()
                .statusCode(200)
                .extract().asString();

        Assert.assertTrue(
                "The generated OpenAPI document \"Info\" property has a wrong value for \"title\" property",
                responseContent.contains(String.format("title: %s.war", PROVIDER_DEPLOYMENT_NAME)));
    }

    /**
     * @tpTestDetails Integration test to verify usage of MP Config core property {@code mp.openapi.scan.disable} by
     *                Local Service Router deployments
     * @tpPassCrit The generated OpenAPI document {@code servers} property contents are not empty - i.e. the main
     *             deployment related OpenAPI document is generated properly
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testDisabledAnnotationsScanIsNotAffectingPreviousDeploymentServerRecords(
            @ArquillianResource @OperateOnDeployment(PROVIDER_DEPLOYMENT_NAME) URL baseURL)
            throws URISyntaxException {
        String responseContent = get(baseURL.toURI().resolve("/openapi"))
                .then()
                .statusCode(200)
                .extract().asString();

        Yaml yaml = new Yaml();
        Object yamlObject = yaml.load(responseContent);
        Map<String, Object> yamlMap = (Map<String, Object>) yamlObject;

        List<Object> serverRecords = (List<Object>) yamlMap.get("servers");
        Assert.assertFalse("\"servers\" property is empty", serverRecords.isEmpty());

        Map<String, Object> firstServerRecord = (Map<String, Object>) serverRecords.get(0);
        Assert.assertNotNull("Expected server record not found", firstServerRecord);
        Assert.assertEquals("\"url\" property value is not expected",
                firstServerRecord.get("url"), String.format("/%s", PROVIDER_DEPLOYMENT_NAME));
    }
}
