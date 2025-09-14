package org.jboss.eap.qe.microprofile.jwt.keycloak;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

import java.net.URL;

import org.arquillian.cube.docker.impl.client.config.Await;
import org.arquillian.cube.docker.junit.rule.ContainerDslRule;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.eap.qe.microprofile.jwt.EnableJwtSubsystemSetupTask;
import org.jboss.eap.qe.microprofile.jwt.testapp.Endpoints;
import org.jboss.eap.qe.microprofile.jwt.testapp.jaxrs.JaxRsTestApplication;
import org.jboss.eap.qe.microprofile.jwt.testapp.jaxrs.SecuredJaxRsEndpoint;
import org.jboss.eap.qe.ts.common.docker.junit.DockerRequiredTests;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test a high level scenario which includes deploying a container with Keycloak, setting it up and obtaining
 * authentication token from it.
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(EnableJwtSubsystemSetupTask.class)
@Category(DockerRequiredTests.class)
public class KeycloakIntegrationHighLevelScenarioTest {

    private static final String KEYCLOAK_REALM_NAME = "foobar";

    private static final String KEYCLOAK_CONTAINER_NAME = KeycloakIntegrationHighLevelScenarioTest.class.getSimpleName()
            + "-container";

    private static final String KEYCLOAK_ADMIN_USERNAME = "admin";
    private static final String KEYCLOAK_ADMIN_PASSWORD = "password";
    private static final Logger log = LoggerFactory.getLogger(KeycloakIntegrationHighLevelScenarioTest.class);

    public static ContainerDslRule keycloakContainerDslRule = new ContainerDslRule("quay.io/keycloak/keycloak:24.0",
            KEYCLOAK_CONTAINER_NAME)
            .withAwaitStrategy(keycloakContainerAwaitStrategy())
            .withPortBinding(KeycloakConfigurator.KEYCLOAK_EXPOSED_HTTP_PORT + "->8080")
            .withEnvironment("KEYCLOAK_ADMIN", KEYCLOAK_ADMIN_USERNAME)
            .withEnvironment("KEYCLOAK_ADMIN_PASSWORD", KEYCLOAK_ADMIN_PASSWORD)
            .withEnvironment("KC_HEALTH_ENABLED", "true")
            .withCommand("start-dev");

    public static KeycloakConfigurator keycloakConfigurator = new KeycloakConfigurator.Builder(KEYCLOAK_REALM_NAME)
            .adminPassword(KEYCLOAK_ADMIN_PASSWORD)
            .adminUsername(KEYCLOAK_ADMIN_USERNAME)
            .clientId("client-custom-id")
            .keycloakBindAddress(KeycloakConfigurator.KEYCLOAK_INSTANCE_HOSTNAME)
            .keycloakHttpPort(KeycloakConfigurator.KEYCLOAK_EXPOSED_HTTP_PORT)
            .build();

    @ClassRule
    public static TestRule ruleChain = RuleChain.outerRule(keycloakContainerDslRule)
            .around(keycloakConfigurator);

    private static Await keycloakContainerAwaitStrategy() {
        Await await = new Await();
        await.setStrategy(
                "org.jboss.eap.qe.microprofile.jwt.keycloak.KeycloakContainerAwaitStrategy");
        return await;
    }

    @Deployment
    public static Archive<?> createDeployment() {
        //visit http://localhost:8179/auth/realms/foobar/.well-known/openid-configuration with running keycloak for values
        final String mpProperties = "mp.jwt.verify.publickey.location=http://localhost:8179/realms/%1$s/protocol/openid-connect/certs%n"
                +
                "mp.jwt.verify.issuer=http://localhost:8179/realms/%1$s";

        return ShrinkWrap.create(WebArchive.class, KeycloakIntegrationHighLevelScenarioTest.class.getSimpleName() + ".war")
                .addClass(SecuredJaxRsEndpoint.class)
                .addClass(JaxRsTestApplication.class)
                .addAsManifestResource(
                        new StringAsset(String.format(mpProperties, KEYCLOAK_REALM_NAME)),
                        "microprofile-config.properties");
    }

    /**
     * @tpTestDetails A high level scenario in which a Keycloak is used for generating a JWT which will be supplied to
     *                server.
     * @tpPassCrit Server accepts the JWT and returns its raw form.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void accessSecuredEndpointWithKeycloakProvidedJwt(@ArquillianResource URL url) {

        final String username = "qux";
        final String password = "foo";
        keycloakConfigurator.addUser(username, password);
        final String rawToken = keycloakConfigurator.getRawJwtFromKeyCloakForTestUser(username, password);

        given().header("Authorization", "Bearer " + rawToken)
                .when().get(url.toExternalForm() + Endpoints.SECURED_ENDPOINT)
                .then()
                .body(equalTo(rawToken))
                .statusCode(200);
    }
}
