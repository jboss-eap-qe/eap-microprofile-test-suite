package org.jboss.eap.qe.microprofile.openapi.advanced;

import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.not;

import jakarta.ws.rs.core.MediaType;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.hamcrest.Matcher;
import org.jboss.arquillian.container.test.api.Deployment;
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
import org.jboss.eap.qe.microprofile.openapi.model.OpenApiFilter;
import org.jboss.eap.qe.microprofile.openapi.model.OpenApiModelReader;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.ConfigurationException;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.ArquillianContainerProperties;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.ArquillianDescriptorWrapper;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.MicroProfileServerSetupTask;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientRelatedException;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.deployment.ConfigurationUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;

/**
 * Base class providing deployments and test cases to assess that the process of generating OpenAPI document is
 * executed correctly, by following all the MicroProfile OpenAPI spec processing rules - i.e. a contract-first
 * (static file based), with a process enabling customization and filtering.
 * <p>
 * The real world scenario for Local Services Router app is implemented: the OpenAPI document is provided as
 * deliverable by the Service Provider staff to Local Services Router staff so that it can be used as official
 * documentation once proper customization is applied.
 * </p>
 * {@link SingleDeploymentHybridDocumentationTest} and {@link MultipleDeploymentsHybridDocumentationTest} cover the
 * single deployment and multiple deployment scenarios respectively.
 */
public class HybridDocumentationTest {

    private final static String PROVIDER_DEPLOYMENT_NAME = "serviceProviderDeployment";
    protected final static String ROUTER_DEPLOYMENT_NAME = "localServicesRouterDeployment";
    private final static String CONFIGURATION_TEMPLATE = "mp.openapi.scan.exclude.packages=org.jboss.eap.qe.microprofile.openapi.apps.routing.router.rest.routed"
            + "\n" +
            "mp.openapi.model.reader=org.jboss.eap.qe.microprofile.openapi.model.%s"
            + "\n" +
            "mp.openapi.filter=org.jboss.eap.qe.microprofile.openapi.model.%s"
            + "\n" +
            "mp.openapi.scan.disable=false"
            + "\n" +
            "services.provider.host=%s"
            + "\n" +
            "services.provider.port=%d";

    private final static ArquillianContainerProperties ARQUILLIAN_CONTAINER_PROPERTIES = new ArquillianContainerProperties(
            ArquillianDescriptorWrapper.getArquillianDescriptor());

    /**
     * Builds a deployment archive with MP OpenAPI disabled to reflect the real life scenario in which the
     * OpenAPI documentation for each Local Service Router app should be generated starting from the static file
     * provided as a deliverable by the Service Provider staff.
     * In this scenario the Service Provider app represented by this deployment would sit on another server so we're
     * disabling it to be sure an {@code openapi} endpoint is not registered for it.
     * The current test only deploys this Service Provider app as a backend for the Local Service Router REST calls.
     *
     * @return {@link WebArchive} instance for the Service provider app deployment
     */
    @Deployment(name = PROVIDER_DEPLOYMENT_NAME, order = 1, testable = false)
    static Archive<?> serviceProviderDeployment() {
        return ShrinkWrap.create(
                WebArchive.class, PROVIDER_DEPLOYMENT_NAME + ".war")
                .addClasses(
                        ProviderApplication.class,
                        District.class,
                        DistrictEntity.class,
                        DistrictService.class,
                        InMemoryDistrictService.class,
                        DistrictsResource.class,
                        RoutingServiceConstants.class)
                .addAsManifestResource(new StringAsset("mp.openapi.extensions.enabled=false"), "microprofile-config.properties")
                .addAsWebInfResource(ConfigurationUtil.BEANS_XML_FILE_LOCATION, "beans.xml");
    }

    /**
     * MP Config is used to tell MP OpenAPI to skip doc generation for exposed "routed" JAX-RS endpoints as service
     * consumers must rely on the original documentation (META-INF/openapi.yaml), plus annotations from Local Service
     * Provider "non-routed" JAX-RS endpoints, edited through an OASModelReader implementation and eventually filtered
     * through a OASFilter one.
     * Here we also add config properties to reach the Services Provider.
     *
     * @return A string containing a set of name/value configuration properties
     * @throws ManagementClientRelatedException CLI management exceptions
     * @throws ConfigurationException Arquillian container configuration exception
     * @throws IOException Management client disposal
     */
    static String buildMicroProfileConfigProperties(final String openApiModelReader, final String openApiFilter)
            throws ManagementClientRelatedException, ConfigurationException, IOException {

        int configuredHTTPPort;
        try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
            configuredHTTPPort = OpenApiServerConfiguration.getHTTPPort(client);
        }
        return String.format(CONFIGURATION_TEMPLATE,
                openApiModelReader,
                openApiFilter,
                ARQUILLIAN_CONTAINER_PROPERTIES.getDefaultManagementAddress(),
                configuredHTTPPort);
    }

    @Deployment(name = ROUTER_DEPLOYMENT_NAME, order = 2, testable = false)
    static Archive<?> localServicesRouterDeployment()
            throws ConfigurationException, IOException, ManagementClientRelatedException {

        String mpConfigProperties = buildMicroProfileConfigProperties(
                OpenApiModelReader.class.getSimpleName(), OpenApiFilter.class.getSimpleName());

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
                .addAsManifestResource(new StringAsset(mpConfigProperties), "microprofile-config.properties")
                .addAsResource("META-INF/openapi.yaml")
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
     * Integration test to verify MP Rest Client usage by Local Service Router JAX-RS resource
     * One of the Local Service Router endpoints is called to retrieve data from Service Provider,
     * verifying the HTTP response code and content type
     *
     * @since EAP 7.4.0.CD19
     */
    protected void verifyRoutedEndpoint(final URL baseURL) {
        get(baseURL.toExternalForm() + "districts/all")
                .then()
                .statusCode(200)
                .contentType(equalToIgnoringCase(MediaType.APPLICATION_JSON));
    }

    /**
     * Verifies proper processing by assessing that information stored in non-routed services
     * annotations is preserved in final document
     * The generated document contains a string that uniquely identifies one of the Local Service Router
     * endpoints URL and that was generated from local JAX-RS resources annotation
     *
     * @since EAP 7.4.0.CD19
     */
    protected void verifyNonRoutedEndpoint(final URL baseURL) {
        get(baseURL.toExternalForm() + "info/fqdn")
                .then()
                .statusCode(200)
                //  RestEasy >3.9.3 adds charset info unless resteasy.add.charset is set to false,
                //  thus expecting for it to be there, see
                //  https://docs.jboss.org/resteasy/docs/3.9.3.Final/userguide/html/Installation_Configuration.html#configuration_switches
                .contentType(equalToIgnoringCase("text/plain;charset=UTF-8"));
    }

    /**
     * Test to verify that static file has been used for OpenAPI document generation
     * The generated document does contain a string which uniquely identifies one of the routed Service
     * Provider endpoints URL
     *
     * @since EAP 7.4.0.CD19
     */
    protected void verifyExpectedStaticFileInformationInOpenApiDoc(final URL baseURL) throws URISyntaxException {
        get(baseURL.toURI().resolve("/openapi"))
                .then()
                .statusCode(200)
                .contentType(equalToIgnoringCase("application/yaml"))
                .body(containsString("/districts/all:"));
    }

    /**
     * Verifies proper processing by using MP OpenAPI programming model to create a custom base document
     * Verifies that {@link OpenAPI#getInfo()} has been successfully modified by {@link OpenApiModelReader}
     *
     * @since EAP 7.4.0.CD19
     */
    protected void verifyOpenApiDocumentForOpenApiInfoChange(final URL baseURL, final boolean shouldContainGeneratedInfo)
            throws URISyntaxException {
        Matcher<String> matcher = containsString("Generated by router: ");
        if (!shouldContainGeneratedInfo) {
            matcher = not(matcher);
        }
        get(baseURL.toURI().resolve("/openapi"))
                .then()
                .statusCode(200)
                .contentType(equalToIgnoringCase("application/yaml"))
                .body(matcher);
    }

    /**
     * Verifies proper processing by using MP OpenAPI programming model to filter the generated document
     * Verifies that {@link RoutingServiceConstants#OPENAPI_OPERATION_EXTENSION_PROXY_FQDN_NAME)} extension
     * value has been successfully modified by {@link OpenApiFilter}
     *
     * @since EAP 7.4.0.CD19
     */
    protected void verifyOpenApiDocumentForRouterFqdnExtensionModification(final URL baseURL, final String localRouterFqdn)
            throws URISyntaxException {
        get(baseURL.toURI().resolve("/openapi"))
                .then()
                .statusCode(200)
                .contentType(equalToIgnoringCase("application/yaml"))
                .body(containsString(String.format("%s: %s",
                        RoutingServiceConstants.OPENAPI_OPERATION_EXTENSION_PROXY_FQDN_NAME, localRouterFqdn)));
    }
}
