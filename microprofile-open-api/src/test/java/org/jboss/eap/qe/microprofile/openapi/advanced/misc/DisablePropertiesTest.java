package org.jboss.eap.qe.microprofile.openapi.advanced.misc;

import java.io.IOException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.eap.qe.microprofile.openapi.OpenApiServerConfiguration;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.ProviderApplication;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.RoutingServiceConstants;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.api.DistrictService;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.data.DistrictEntity;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.model.District;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.rest.DistrictsResource;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.rest.EchoResource;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.services.InMemoryDistrictService;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.router.RouterApplication;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.router.model.DistrictObject;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.router.rest.LocalServiceRouterInfoResource;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.router.rest.routed.RouterDistrictsResource;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.router.services.DistrictServiceClient;
import org.jboss.eap.qe.microprofile.openapi.model.OpenApiFilter;
import org.jboss.eap.qe.microprofile.openapi.model.OpenApiModelReader;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.ConfigurationException;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.ArquillianContainerProperties;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.ArquillianDescriptorWrapper;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.MicroProfileServerSetupTask;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientRelatedException;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.deployment.ConfigurationUtil;
import org.jboss.eap.qe.microprofile.tooling.server.log.ModelNodeLogChecker;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;

/**
 * Test to assess negative scenario for behavior for conflicting configuration properties, i.e. core
 * {@code mp.openapi.scan.disable} and extension {@code mp.openapi.extensions.enabled} usage.
 *
 * The real world scenario for Local Services Router app is implemented here: the OpenAPI document is provided as
 * deliverable by Service Provider staff to Local Services Router staff so that it can be used as official documentation
 * once proper customization is applied.
 */
@RunWith(Arquillian.class)
@ServerSetup({ DisablePropertiesTest.OpenApiExtensionSetup.class })
@RunAsClient
public class DisablePropertiesTest {

    private final static String PROVIDER_DEPLOYMENT_NAME = "serviceProviderDeployment";
    private final static String ROUTER_DEPLOYMENT_NAME = "localServicesRouterDeployment";

    private static ArquillianContainerProperties arquillianContainerProperties = new ArquillianContainerProperties(
            ArquillianDescriptorWrapper.getArquillianDescriptor());

    private final static String CONFIGURATION_TEMPLATE = "mp.openapi.scan.exclude.packages=org.jboss.eap.qe.microprofile.openapi.apps.routing.router.rest.routed"
            + "\n" +
            "mp.openapi.model.reader=org.jboss.eap.qe.microprofile.openapi.model.OpenApiModelReader"
            + "\n" +
            "mp.openapi.filter=org.jboss.eap.qe.microprofile.openapi.model.OpenApiFilter"
            + "\n" +
            "mp.openapi.scan.disable=false";

    @Deployment(name = PROVIDER_DEPLOYMENT_NAME, order = 1, testable = false)
    public static WebArchive serviceProviderDeployment() {
        //  This is the Services Provider deployment with default configuration and simulates the main deployment in
        //  the current scenario - i.e. the one that is run by Services Provider.
        return ShrinkWrap.create(
                WebArchive.class, PROVIDER_DEPLOYMENT_NAME + ".war")
                .addClasses(
                        ProviderApplication.class,
                        District.class,
                        DistrictEntity.class,
                        DistrictService.class,
                        InMemoryDistrictService.class,
                        DistrictsResource.class,
                        RoutingServiceConstants.class,
                        EchoResource.class)
                .addAsResource(
                        DisablePropertiesTest.class.getClassLoader().getResource(
                                "META-INF/microprofile-config-disabled.properties"),
                        "META-INF/microprofile-config.properties")
                .addAsWebInfResource(new StringAsset(ConfigurationUtil.BEANS_XML_FILE_CONTENT), "beans.xml");
    }

    /**
     * MP Config properties in {@link DisablePropertiesTest#CONFIGURATION_TEMPLATE} are used to tell MP OpenAPI to
     * skip doc generation for exposed "routed" JAX-RS endpoints as service consumers must rely to the original
     * documentation (META-INF/openapi.yaml), plus annotations from Local Service Provider "non-routed" JAX-RS endpoints,
     * edited through an OASModelReader implementation and eventually filtered by a OASFilter one.
     * NOT WORKING in EAR? Here we also add config properties to reach the Services Provider.
     *
     * @return A string containing a set of name/value configuration properties
     */
    private static String buildMicroProfileConfigProperties() {
        return CONFIGURATION_TEMPLATE;
    }

    @Deployment(name = ROUTER_DEPLOYMENT_NAME, order = 2, testable = false)
    public static WebArchive localServicesRouterDeployment()
            throws ConfigurationException, IOException, ManagementClientRelatedException {

        int configuredHTTPPort;
        try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
            configuredHTTPPort = OpenApiServerConfiguration.getHTTPPort(client);
        }
        String props = String.format("services.provider.host=%s\nservices.provider.port=%d",
                arquillianContainerProperties.getDefaultManagementAddress(),
                configuredHTTPPort);

        return ShrinkWrap.create(
                WebArchive.class, ROUTER_DEPLOYMENT_NAME + ".war")
                .addClasses(
                        RouterApplication.class,
                        LocalServiceRouterInfoResource.class,
                        DistrictObject.class,
                        RouterDistrictsResource.class,
                        DistrictServiceClient.class,
                        OpenApiModelReader.class,
                        OpenApiFilter.class)
                .addAsManifestResource(new StringAsset(buildMicroProfileConfigProperties() + "\n" + props),
                        "microprofile-config.properties")
                .addAsResource(DisablePropertiesTest.class.getClassLoader().getResource("META-INF/openapi-server-path.yaml"),
                        "META-INF/openapi.yaml")
                .addAsWebInfResource(new StringAsset(ConfigurationUtil.BEANS_XML_FILE_CONTENT), "beans.xml");
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
     * @tpTestDetails Test to verify that using {@code mp.openapi.scan.disable} core property <b>does not</b> disable
     *                the OpenAPI document generation process, which in fact is achieved through
     *                {@code mp.openapi.extensions.path} extension
     *                property. So in this test case deployment, the OpenAPI document is generated by processing the
     *                first and consequently the second (i.e. Local Service Router) deployment is skipped
     * @tpPassCrit The server is logging expected {@code WFLYMPOAI0003} message because the first deployment does not
     *             deactivate OpenAPI subsystem, and the secodn deployment is not properly configured in order to have a custom
     *             url
     *             for {@code openapi} endpoint
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testWarningIsLoggedBecauseOfSkippedBadlyConfiguredDeployment() throws ConfigurationException, IOException {
        try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
            ModelNodeLogChecker modelNodeLogChecker = new ModelNodeLogChecker(client, 100);
            Assert.assertTrue(modelNodeLogChecker.logContains("WFLYMPOAI0003"));
        }
    }
}
