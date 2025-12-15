package org.jboss.eap.qe.microprofile.openapi.advanced.misc;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.eap.qe.microprofile.openapi.OpenApiServerConfiguration;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.router.RouterApplication;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.router.model.DistrictObject;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.router.model.PojoExample;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.router.rest.LocalServiceRouterExampleResource;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.router.rest.LocalServiceRouterInfoResource;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.router.rest.routed.RouterDistrictsResource;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.router.services.DistrictServiceClient;
import org.jboss.eap.qe.microprofile.openapi.model.OpenApiModelReader;
import org.jboss.eap.qe.microprofile.openapi.model.OpenApiSingletonOpenApiFilter;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.ConfigurationException;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.ArquillianContainerProperties;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.ArquillianDescriptorWrapper;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientRelatedException;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.deployment.ConfigurationUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;

/**
 * Validate processing rules which should be enforced when generating OpenAPI documentation
 *
 * @see <a href=
 *      "https://download.eclipse.org/microprofile/microprofile-open-api-4.1/microprofile-openapi-spec-4.1.html#_processing_rules">4.5.
 *      Processing rules</a>
 *
 *      <p>
 *      The processing rules mandate for the vendor implementation to operate in the following order: <br>
 *      - A {@link org.eclipse.microprofile.openapi.OASModelReader} instance, if present, should create the OpenAPI model <br>
 *      - Static files content, if present, should be loaded and merged with pre-existing model, overriding any conflicting
 *      values <br>
 *      - Annotations should be processed to further modify the generate model, overriding any conflicting values <br>
 *      - Eventually, any {@link org.eclipse.microprofile.openapi.OASFilter} instance should be invoked in order to apply
 *      any final customization to the model.
 *      </p>
 */
public class ProcessingRulesTestBase {

    protected static final String STATICALLY_GENERATED_ROUTER_DEPLOYMENT_NAME = "staticallyGeneratedLocalServicesRouterDeployment";
    protected static final String PROGRAMMATICALLY_MODELED_ROUTER_DEPLOYMENT_NAME = "programmaticallyModeledLocalServicesRouterDeployment";
    private final static String STATICALLY_GENERATED_ROUTER_CONFIGURATION_TEMPLATE = "mp.openapi.scan.exclude.packages=org.jboss.eap.qe.microprofile.openapi.apps.routing.router.rest.routed"
            + "\n" +
            "mp.openapi.scan.disable=false"
            + "\n" +
            "services.provider.host=%s"
            + "\n" +
            "services.provider.port=%d";
    private final static String PROGRAMMATICALLY_MODELED_ROUTER_CONFIGURATION_TEMPLATE = STATICALLY_GENERATED_ROUTER_CONFIGURATION_TEMPLATE
            + "\n" +
            "mp.openapi.model.reader=org.jboss.eap.qe.microprofile.openapi.model.%s"
            + "\n" +
            "mp.openapi.filter=org.jboss.eap.qe.microprofile.openapi.model.%s";

    private final static ArquillianContainerProperties ARQUILLIAN_CONTAINER_PROPERTIES = new ArquillianContainerProperties(
            ArquillianDescriptorWrapper.getArquillianDescriptor());

    @ArquillianResource
    protected Deployer deployer;

    static String buildStaticallyGeneratedRouterDeploymentMicroProfileConfigProperties()
            throws ConfigurationException, IOException, ManagementClientRelatedException {
        try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
            final String configuredHttpHost = ARQUILLIAN_CONTAINER_PROPERTIES.getDefaultManagementAddress();
            final int configuredHttpPort = OpenApiServerConfiguration.getHTTPPort(client);
            final InetAddress address = new InetSocketAddress(configuredHttpHost, configuredHttpPort).getAddress();
            return String.format(STATICALLY_GENERATED_ROUTER_CONFIGURATION_TEMPLATE,
                    address.getCanonicalHostName(),
                    8080);
        }
    }

    static String buildProgrammaticallyModeledRouterDeploymentMicroProfileConfigProperties(final String openApiModelReader,
            final String openApiFilter) throws ConfigurationException, IOException, ManagementClientRelatedException {
        try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
            final String configuredHttpHost = ARQUILLIAN_CONTAINER_PROPERTIES.getDefaultManagementAddress();
            final int configuredHttpPort = OpenApiServerConfiguration.getHTTPPort(client);
            final InetAddress address = new InetSocketAddress(configuredHttpHost, configuredHttpPort).getAddress();
            return String.format(PROGRAMMATICALLY_MODELED_ROUTER_CONFIGURATION_TEMPLATE,
                    address.getCanonicalHostName(),
                    8080,
                    openApiModelReader,
                    openApiFilter);
        }
    }

    /**
     * MP Config is used to tell MP OpenAPI to skip doc generation for exposed "routed" JAX-RS endpoints as service
     * consumers must rely on the original documentation (META-INF/openapi.yaml), in order generate the OpenAPI
     * documentation from a static file.
     * MP config properties to reach the Services Provider are added as well.
     *
     * @return {@link WebArchive} instance containing the Local Service Router application that generates the OpenAPI
     *         documentation from a static file.
     */
    @Deployment(name = STATICALLY_GENERATED_ROUTER_DEPLOYMENT_NAME, managed = false, testable = false)
    static Archive<?> staticallyGeneratedLocalServicesRouterDeployment()
            throws ConfigurationException, ManagementClientRelatedException, IOException {
        return routerDeployment(false);
    }

    /**
     * MP Config is used to tell MP OpenAPI to skip doc generation for exposed "routed" JAX-RS endpoints as service
     * consumers must rely on the original documentation (META-INF/openapi.yaml), plus annotations from Local Service
     * Provider "non-routed" JAX-RS endpoints, edited through an {@link org.eclipse.microprofile.openapi.OASModelReader}
     * implementation and eventually filtered through a {@link org.eclipse.microprofile.openapi.OASFilter} one.
     * MP config properties to reach the Services Provider are added as well.
     *
     * @return {@link WebArchive} instance containing the Local Service Router application that generates the OpenAPI
     *         documentation by modifying a static file.
     */
    @Deployment(name = PROGRAMMATICALLY_MODELED_ROUTER_DEPLOYMENT_NAME, managed = false, testable = false)
    static Archive<?> programmaticallyModeledLocalServicesRouterDeployment()
            throws ConfigurationException, ManagementClientRelatedException, IOException {
        return routerDeployment(true);
    }

    private static Archive<?> routerDeployment(final boolean programmaticallyModify)
            throws ConfigurationException, ManagementClientRelatedException, IOException {
        String mpConfigProperties = programmaticallyModify
                ? buildProgrammaticallyModeledRouterDeploymentMicroProfileConfigProperties(
                        OpenApiModelReader.class.getSimpleName(), OpenApiSingletonOpenApiFilter.class.getSimpleName())
                : buildStaticallyGeneratedRouterDeploymentMicroProfileConfigProperties();
        List<Class<?>> classes = Stream.of(RouterApplication.class,
                LocalServiceRouterInfoResource.class,
                DistrictObject.class,
                PojoExample.class,
                RouterDistrictsResource.class,
                DistrictServiceClient.class).collect(Collectors.toList());
        if (programmaticallyModify) {
            classes.add(OpenApiModelReader.class);
            classes.add(OpenApiSingletonOpenApiFilter.class);
            classes.add(LocalServiceRouterExampleResource.class);
        }
        final String deploymentName = programmaticallyModify ? PROGRAMMATICALLY_MODELED_ROUTER_DEPLOYMENT_NAME
                : STATICALLY_GENERATED_ROUTER_DEPLOYMENT_NAME;
        final String staticFileName = programmaticallyModify ? "openapi.yaml" : "openapi-with-external-docs.yaml";
        return ShrinkWrap.create(
                WebArchive.class, deploymentName + ".war")
                .addClasses(classes.toArray(Class[]::new))
                .addAsManifestResource(new StringAsset(mpConfigProperties), "microprofile-config.properties")
                .addAsResource(String.format("META-INF/%s", staticFileName), "META-INF/openapi.yaml")
                .addAsWebInfResource(ConfigurationUtil.BEANS_XML_FILE_LOCATION, "beans.xml");
    }
}
