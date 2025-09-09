package org.jboss.eap.qe.microprofile.config.multiple.deployments.expressions;

import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.is;

import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.MicroProfileServerSetupTask;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;

/**
 * This test class validates MicroProfile Config expression-based property resolution when multiple WARs are
 * deployed.
 * <p>
 * Test cases are inspired by the related
 * <a href=
 * "https://download.eclipse.org/microprofile/microprofile-config-3.1/microprofile-config-spec-3.1.html#property-expressions">MicroProfile
 * Config documentation</a>
 * </p>
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(MultipleWarDeploymentExpressionTestCase.GlobalConfigSourceSetupTask.class)
public class MultipleWarDeploymentExpressionTestCase {

    static final String PROPERTY_NAME_SERVER_URL = "server.url";
    static final String PROPERTY_NAME_SERVER_HOST = "server.host";
    static final String PROPERTY_NAME_SERVER_PORT = "server.port";
    static final String PROPERTY_NAME_SERVER_ENDPOINT = "server.endpoint";
    static final String PROPERTY_NAME_SERVER_ENDPOINT_FOO = "server.endpoint.path.foo";
    static final String PROPERTY_NAME_SERVER_ENDPOINT_BAZ = "server.endpoint.path.baz";
    static final String PROPERTY_NAME_SERVER_ENDPOINT_BAR = "server.endpoint.path.bar";

    static final String PROPERTY_VALUE_SERVER_PORT_DEFAULT = "80";
    static final String PROPERTY_VALUE_SERVER_HOST_1 = "server1";
    static final String PROPERTY_VALUE_SERVER_HOST_2 = "server2";
    static final String PROPERTY_VALUE_SERVER_PORT_1 = "8080";
    static final String PROPERTY_VALUE_SERVER_PORT_2 = "8081";
    static final String PROPERTY_VALUE_SERVER_ENDPOINT_FOO = "foo-endpoint";
    static final String PROPERTY_VALUE_SERVER_ENDPOINT_BAZ = "baz-endpoint";
    static final String PROPERTY_VALUE_SERVER_ENDPOINT_DEFAULT = String.format(
            "${server.endpoint.path.${%s}}", PROPERTY_NAME_SERVER_ENDPOINT_BAR);
    static final String PROPERTY_VALUE_SERVER_URL_DEFAULT = String.format(
            "http://${%s:example.org}:${%s}/${%s}", PROPERTY_NAME_SERVER_HOST, PROPERTY_NAME_SERVER_PORT,
            PROPERTY_NAME_SERVER_ENDPOINT_BAR);

    @ArquillianResource
    @OperateOnDeployment("d1")
    private URL url1;

    @ArquillianResource
    @OperateOnDeployment("d2")
    private URL url2;

    @Deployment(name = "d1", order = 1, testable = false)
    public static Archive<?> createDeployment1() {
        return ShrinkWrap.create(WebArchive.class, "d1.war")
                .addClasses(ConfigResource.class, JaxRsActivator.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsManifestResource(new StringAsset(
                        "config_ordinal=700\n" +
                                String.format("%s=%s\n", PROPERTY_NAME_SERVER_HOST, PROPERTY_VALUE_SERVER_HOST_1) +
                                String.format("%s=%s\n", PROPERTY_NAME_SERVER_PORT, PROPERTY_VALUE_SERVER_PORT_1) +
                                String.format("%s=%s\n", PROPERTY_NAME_SERVER_ENDPOINT_FOO, PROPERTY_VALUE_SERVER_ENDPOINT_FOO)
                                +
                                String.format("%s=foo\n", PROPERTY_NAME_SERVER_ENDPOINT_BAR)), // <- "server.endpoint.path.bar" redirects to the actual path, which is "foo" in this case
                        "microprofile-config.properties");
    }

    @Deployment(name = "d2", order = 2, testable = false)
    public static Archive<?> createDeployment2() {
        return ShrinkWrap.create(WebArchive.class, "d2.war")
                .addClasses(ConfigResource.class, JaxRsActivator.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsManifestResource(new StringAsset(
                        "config_ordinal=700\n" +
                                String.format("%s=%s\n", PROPERTY_NAME_SERVER_HOST, PROPERTY_VALUE_SERVER_HOST_2) +
                                String.format("%s=%s\n", PROPERTY_NAME_SERVER_PORT, PROPERTY_VALUE_SERVER_PORT_2) +
                                String.format("%s=%s\n", PROPERTY_NAME_SERVER_ENDPOINT_BAZ, PROPERTY_VALUE_SERVER_ENDPOINT_BAZ)
                                +
                                String.format("%s=baz\n", PROPERTY_NAME_SERVER_ENDPOINT_BAR)), // <- "server.endpoint.path.bar" redirects to the actual path, which is "baz" in this case
                        "microprofile-config.properties");
    }

    /**
     * @tpTestDetails Verify that a simple expression is resolved correctly, based on the deployment value, which
     *                overrides the default, that was set via system property.
     * @tpPassCrit A system property value is overridden by the one set at deployment level
     * @tpSince JBoss EAP XP 6
     */
    @Test
    public void testSimpleExpressionIsResolvedProperly() {
        get(url1.toExternalForm() + "config/" + PROPERTY_NAME_SERVER_PORT)
                .then()
                .statusCode(200)
                .body(is("8080"));

        get(url2.toExternalForm() + "config/" + PROPERTY_NAME_SERVER_PORT)
                .then()
                .statusCode(200)
                .body(is("8081"));
    }

    /**
     * @tpTestDetails Verify that a composed expression is resolved correctly, based on the deployment value, which
     *                is expanded into the default, that was set via system property.
     * @tpPassCrit A system property value is overridden by the composed one
     * @tpSince JBoss EAP XP 6
     */
    @Test
    public void testComposedExpressionIsResolvedProperly() {
        get(url1.toExternalForm() + "config/" + PROPERTY_NAME_SERVER_ENDPOINT)
                .then()
                .statusCode(200)
                .body(is(PROPERTY_VALUE_SERVER_ENDPOINT_FOO));

        get(url2.toExternalForm() + "config/" + PROPERTY_NAME_SERVER_ENDPOINT)
                .then()
                .statusCode(200)
                .body(is(PROPERTY_VALUE_SERVER_ENDPOINT_BAZ));
    }

    /**
     * @tpTestDetails Verify that a multiple expression is resolved correctly, based on the deployment values, which
     *                are expanded into the default, that was set via system property.
     * @tpPassCrit A system property value is overridden by the expanded one
     * @tpSince JBoss EAP XP 6
     */
    @Test
    public void testMultipleExpressionIsResolvedProperly() {
        get(url1.toExternalForm() + "config/" + PROPERTY_NAME_SERVER_URL)
                .then()
                .statusCode(200)
                .body(is("http://server1:8080/foo"));

        get(url2.toExternalForm() + "config/" + PROPERTY_NAME_SERVER_URL)
                .then()
                .statusCode(200)
                .body(is("http://server2:8081/baz"));
    }

    public static class GlobalConfigSourceSetupTask implements MicroProfileServerSetupTask {

        @Override
        public void setup() throws Exception {

            try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
                client.execute("/subsystem=microprofile-config-smallrye/config-source=test:add(ordinal=600)");
                client.execute(
                        "/subsystem=microprofile-config-smallrye/config-source=test:map-put(name=properties,key=server.port,value="
                                + PROPERTY_VALUE_SERVER_PORT_DEFAULT + ")");
                client.execute(
                        "/subsystem=microprofile-config-smallrye/config-source=test:map-put(name=properties,key=server.endpoint,value=\""
                                + PROPERTY_VALUE_SERVER_ENDPOINT_DEFAULT + "\")");
                client.execute(
                        "/subsystem=microprofile-config-smallrye/config-source=test:map-put(name=properties,key=server.url,value=\""
                                + PROPERTY_VALUE_SERVER_URL_DEFAULT + "\")");
                new Administration(client).reload();
            }
        }

        @Override
        public void tearDown() throws Exception {

            try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
                client.execute("/subsystem=microprofile-config-smallrye/config-source=test:remove");
            }
        }
    }
}
