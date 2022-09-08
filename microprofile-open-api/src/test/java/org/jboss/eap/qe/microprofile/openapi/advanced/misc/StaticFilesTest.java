package org.jboss.eap.qe.microprofile.openapi.advanced.misc;

import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalToIgnoringCase;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.Testable;
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
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.yaml.snakeyaml.Yaml;

/**
 * Test to assess behavior in case of complex/negative scenarios involving static files.
 * One EAR deployment contains 2 WAR archives to simulate a complex environment for Service Provider site:
 * the first WAR is the SP app, while the second one represents a test LSR implemented by SP staff as an examples for
 * future implementors.
 * The second WAR stores 2 static files, to assess that the proper file is chosen in this negative scenario.
 * One more deployment uses a non-valid OpenAPI static file to verify the behavior in this case.
 * Finally one deployment is used to assess that the OpenAPI document is served correctly when using a "big" (+3MB)
 * static file as a source.
 *
 * The real world scenario for Local Services Router app is implemented here: the OpenAPI document is provided as
 * deliverable by Service Provider staff to Local Services Router staff so that it can be used as official documentation
 * once proper customization is applied.
 */
@RunWith(Arquillian.class)
@ServerSetup({ StaticFilesTest.OpenApiExtensionSetup.class })
@RunAsClient
public class StaticFilesTest {

    private final static String PROVIDER_DEPLOYMENT_NAME = "serviceProviderDeployment";
    private final static String ROUTER_DEPLOYMENT_NAME = "localServicesRouterDeployment";
    private final static String PARENT_PROVIDER_DEPLOYMENT_NAME = "parent-" + PROVIDER_DEPLOYMENT_NAME;
    private final static String BROKEN_STATIC_FILE_ROUTER_DEPLOYMENT_NAME = "broken-static-file-" + ROUTER_DEPLOYMENT_NAME;
    private final static String BIG_STATIC_FILE_ROUTER_DEPLOYMENT_NAME = "big-static-file-" + ROUTER_DEPLOYMENT_NAME;
    public static final int REPEAT_BODY_CONTENTS_ITERATIONS = 2048;

    private static ArquillianContainerProperties arquillianContainerProperties = new ArquillianContainerProperties(
            ArquillianDescriptorWrapper.getArquillianDescriptor());

    private final static String CONFIGURATION_TEMPLATE = "mp.openapi.scan.exclude.packages=org.jboss.eap.qe.microprofile.openapi.apps.routing.router.rest.routed"
            + "\n" +
            "mp.openapi.model.reader=org.jboss.eap.qe.microprofile.openapi.model.OpenApiModelReader"
            + "\n" +
            "mp.openapi.filter=org.jboss.eap.qe.microprofile.openapi.model.OpenApiFilter"
            + "\n" +
            "mp.openapi.scan.disable=false";

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
                        RoutingServiceConstants.class)
                .addAsManifestResource(new StringAsset("mp.openapi.extensions.enabled=false"), "microprofile-config.properties")
                .addAsWebInfResource(new StringAsset(ConfigurationUtil.BEANS_XML_FILE_CONTENT), "beans.xml");
    }

    /**
     * MP Config properties in {@link StaticFilesTest#CONFIGURATION_TEMPLATE} are used to tell MP OpenAPI to
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
                        DistrictServiceClient.class,
                        OpenApiModelReader.class,
                        OpenApiFilter.class)
                .addAsManifestResource(new StringAsset(buildMicroProfileConfigProperties() + "\n" + props),
                        "microprofile-config.properties")
                .addAsResource("META-INF/openapi.yaml")
                .addAsResource("META-INF/openapi.yml")
                .addAsResource("META-INF/openapi.json")
                .addAsWebInfResource(new StringAsset(ConfigurationUtil.BEANS_XML_FILE_CONTENT), "beans.xml");
    }

    @Deployment(name = PARENT_PROVIDER_DEPLOYMENT_NAME, order = 1, testable = false)
    public static Archive<?> parentDeployment() throws ConfigurationException, IOException, ManagementClientRelatedException {
        return ShrinkWrap.create(EnterpriseArchive.class, PARENT_PROVIDER_DEPLOYMENT_NAME + ".ear")
                .addAsModules(serviceProviderDeployment(), Testable.archiveToTest(localServicesRouterDeployment()));
    }

    @Deployment(name = BROKEN_STATIC_FILE_ROUTER_DEPLOYMENT_NAME, order = 2, testable = false)
    public static WebArchive brokenStaticFileLocalServicesRouterDeployment()
            throws ConfigurationException, IOException, ManagementClientRelatedException {

        int configuredHTTPPort;
        try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
            configuredHTTPPort = OpenApiServerConfiguration.getHTTPPort(client);
        }
        String props = String.format(
                "mp.openapi.extensions.path=/broken/openapi\nservices.provider.host=%s\nservices.provider.port=%d",
                arquillianContainerProperties.getDefaultManagementAddress(),
                configuredHTTPPort);

        return ShrinkWrap.create(
                WebArchive.class, BROKEN_STATIC_FILE_ROUTER_DEPLOYMENT_NAME + ".war")
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
                .addAsResource("META-INF/openapi-broken.yaml", "META-INF/openapi.yaml")
                .addAsWebInfResource(new StringAsset(ConfigurationUtil.BEANS_XML_FILE_CONTENT), "beans.xml");
    }

    @Deployment(name = BIG_STATIC_FILE_ROUTER_DEPLOYMENT_NAME, order = 3, testable = false)
    public static WebArchive bigStaticFileLocalServicesRouterDeployment()
            throws ConfigurationException, IOException, ManagementClientRelatedException {

        int configuredHTTPPort;
        try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
            configuredHTTPPort = OpenApiServerConfiguration.getHTTPPort(client);
        }
        String props = String.format(
                "mp.openapi.extensions.path=/big/openapi\nservices.provider.host=%s\nservices.provider.port=%d",
                arquillianContainerProperties.getDefaultManagementAddress(),
                configuredHTTPPort);

        //  let's build a big openapi file
        String bigFileContents = "";
        //  header
        StringBuilder headerContentBuilder = new StringBuilder();
        try (Stream<String> stream = Files.lines(Paths.get("src/test/resources/META-INF/openapi-fragment-header.yaml"),
                StandardCharsets.UTF_8)) {
            stream.forEach(s -> headerContentBuilder.append(s).append("\n"));
        }
        bigFileContents += headerContentBuilder.toString();
        StringBuilder bodyChunkContentBuilder = new StringBuilder();
        try (Stream<String> stream = Files.lines(Paths.get("src/test/resources/META-INF/openapi-fragment-body.yaml"),
                StandardCharsets.UTF_8)) {
            stream.forEach(s -> bodyChunkContentBuilder.append(s).append("\n"));
        }
        String bodyChunk = bodyChunkContentBuilder.toString();
        for (int i = 0; i < REPEAT_BODY_CONTENTS_ITERATIONS; i++) {
            //  n-chunk of body
            bigFileContents += bodyChunk.replaceAll("@@ID@@", String.valueOf(i));
        }
        //  footer
        StringBuilder footerContentBuilder = new StringBuilder();
        try (Stream<String> stream = Files.lines(Paths.get("src/test/resources/META-INF/openapi-fragment-footer.yaml"),
                StandardCharsets.UTF_8)) {
            stream.forEach(s -> footerContentBuilder.append(s).append("\n"));
        }
        bigFileContents += footerContentBuilder.toString();

        return ShrinkWrap.create(
                WebArchive.class, BIG_STATIC_FILE_ROUTER_DEPLOYMENT_NAME + ".war")
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
                .addAsResource(new StringAsset(bigFileContents), "META-INF/openapi.yaml")
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
     * @tpTestDetails Verifies proper processing by assessing that information stored in non-routed services
     *                annotations is preserved in final document
     * @tpPassCrit The generated document contains a string that uniquely identifies one of the Local Service Router
     *             endpoints URL and that was generated from local JAX-RS resources annotation
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testNonRoutedEndpoint(
            @ArquillianResource @OperateOnDeployment(PARENT_PROVIDER_DEPLOYMENT_NAME) URL baseURL) {
        get(baseURL.toExternalForm() + "/" + ROUTER_DEPLOYMENT_NAME + "/info/fqdn")
                .then()
                .statusCode(200)
                //  RestEasy >3.9.3 adds charset info unless resteasy.add.charset is set to false,
                //  thus expecting for it to be there, see
                //  https://docs.jboss.org/resteasy/docs/3.9.3.Final/userguide/html/Installation_Configuration.html#configuration_switches
                .contentType(equalToIgnoringCase("text/plain;charset=UTF-8"));
    }

    /**
     * @tpTestDetails Test to verify that static file has been used for OpenAPI document generation
     * @tpPassCrit The generated document does contain a string which uniquely identifies one of the routed Service
     *             Provider endpoints URL
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testExpectedStaticFileInformationInOpenApiDoc(
            @ArquillianResource @OperateOnDeployment(PARENT_PROVIDER_DEPLOYMENT_NAME) URL baseURL) throws URISyntaxException {
        get(baseURL.toURI().resolve("/openapi"))
                .then()
                .statusCode(200)
                .contentType(equalToIgnoringCase("application/yaml"))
                .body(containsString("/districts/all:"));
    }

    /**
     * @tpTestDetails Verifies proper processing by using MP OpenAPI programming model to create a custom base document
     * @tpPassCrit Verifies that {@link OpenAPI#getInfo()} has been successfully modified by {@link OpenApiModelReader}
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testOpenApiDocumentForOpenApiInfoChange(
            @ArquillianResource @OperateOnDeployment(PARENT_PROVIDER_DEPLOYMENT_NAME) URL baseURL) throws URISyntaxException {
        get(baseURL.toURI().resolve("/openapi"))
                .then()
                .statusCode(200)
                .contentType(equalToIgnoringCase("application/yaml"))
                .body(containsString("Generated"));
    }

    /**
     * @tpTestDetails Verifies proper processing by using MP OpenAPI programming model to filter the generated document
     * @tpPassCrit Verifies that {@link RoutingServiceConstants#OPENAPI_OPERATION_EXTENSION_PROXY_FQDN_NAME)} extension
     *             value has been successfully modified by {@link OpenApiFilter}
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testOpenApiDocumentForRouterFqdnExtensionModification(
            @ArquillianResource @OperateOnDeployment(PARENT_PROVIDER_DEPLOYMENT_NAME) URL baseURL)
            throws URISyntaxException {
        get(baseURL.toURI().resolve("/openapi"))
                .then()
                .statusCode(200)
                .contentType(equalToIgnoringCase("application/yaml"))
                .body(containsString(OpenApiFilter.LOCAL_TEST_ROUTER_FQDN));
    }

    @Test
    @Ignore("This should fail but currently there are no log messages neither Exceptions to expect in order to assess " +
            "this, see https://issues.redhat.com/projects/WFWIP/issues/WFWIP-292")
    public void testDocumentValidityWhenOpenApiStaticFileIsNotValid(
            @ArquillianResource @OperateOnDeployment(BROKEN_STATIC_FILE_ROUTER_DEPLOYMENT_NAME) URL baseURL)
            throws URISyntaxException {

        String yamlResponseContent = get(baseURL.toURI().resolve("/broken/openapi"))
                .then()
                .statusCode(200)
                .contentType(equalToIgnoringCase("application/yaml"))
                .extract().asString();

        Yaml yaml = new Yaml();
        Object yamlObject = yaml.load(yamlResponseContent);

        Assert.assertNotNull(yamlObject);

        //  TODO: how to assess that this test should fail?
    }

    /**
     * @tpTestDetails Verifies that the OpenAPI document is generated and available when using a "big" (+3MB) static
     *                file as a source.
     * @tpPassCrit Verifies that the generated document contains the path for the last operation defined, i.e.
     *             having the {@code district} part ending with the
     *             {@link StaticFilesTest#REPEAT_BODY_CONTENTS_ITERATIONS}
     *             suffix
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testOpenApiDocumentGeneratedWhenBigStaticFileIsUsed(
            @ArquillianResource @OperateOnDeployment(PARENT_PROVIDER_DEPLOYMENT_NAME) URL baseURL)
            throws URISyntaxException {
        get(baseURL.toURI().resolve("/big/openapi"))
                .then()
                .statusCode(200)
                .contentType(equalToIgnoringCase("application/yaml"))
                .body(containsString(String.format("/districts%d/all:", (REPEAT_BODY_CONTENTS_ITERATIONS - 1))));
        ;
    }
}
