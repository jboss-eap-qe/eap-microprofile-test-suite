package org.jboss.eap.qe.microprofile.jwt.keycloak;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.eap.qe.microprofile.jwt.EnableJwtSubsystemSetupTask;
import org.jboss.eap.qe.microprofile.jwt.testapp.Endpoints;
import org.jboss.eap.qe.microprofile.jwt.testapp.jaxrs.JaxRsTestApplication;
import org.jboss.eap.qe.microprofile.jwt.testapp.jaxrs.SecuredJaxRsEndpoint;
import org.jboss.eap.qe.ts.common.docker.Docker;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

/**
 * Test a high level scenario which includes deploying a container with Keycloak, setting it up and obtaining
 * authentication token from it.
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(EnableJwtSubsystemSetupTask.class)
public class KeycloakIntegrationHighLevelScenarioTest {

    private static final String KEYCLOAK_REALM_NAME = "foobar";

    private static final String KEYCLOAK_INSTANCE_HOSTNAME = "localhost";

    private static final String KEYCLOAK_CONTAINER_NAME = KeycloakIntegrationHighLevelScenarioTest.class.getSimpleName()
            + "-container";

    private static final int KEYCLOAK_EXPOSED_HTTP_PORT = 8179;
    private static final int KEYCLOAK_EXPOSED_TOKEN_PORT = 8149;
    private static final int KEYCLOAK_EXPOSED_MANAGEMENT_PORT = 9988;

    private static final String KEYCLOAK_ADMIN_USERNAME = "admin";
    private static final String KEYCLOAK_ADMIN_PASSWORD = "password";

    public static Docker keycloakContainer = new Docker.Builder(KEYCLOAK_CONTAINER_NAME,
            "registry.hub.docker.com/jboss/keycloak:8.0.1")
                    .setContainerReadyTimeout(2, TimeUnit.MINUTES)
                    .setContainerReadyCondition(() -> isContainerReady(KEYCLOAK_EXPOSED_HTTP_PORT))
                    .withPortMapping(KEYCLOAK_EXPOSED_HTTP_PORT + ":8080")
                    .withPortMapping(KEYCLOAK_EXPOSED_TOKEN_PORT + ":8085")
                    .withPortMapping(KEYCLOAK_EXPOSED_MANAGEMENT_PORT + ":9990")
                    .withEnvVar("KEYCLOAK_USER", KEYCLOAK_ADMIN_USERNAME)
                    .withEnvVar("KEYCLOAK_PASSWORD", KEYCLOAK_ADMIN_PASSWORD)
                    .withEnvVar("DB_VENDOR", "h2")
                    .withCmdArg("-b=0.0.0.0")
                    .withCmdArg("-bmanagement=0.0.0.0")
                    .build();

    public static KeycloakConfigurator keycloakConfigurator = new KeycloakConfigurator.Builder(KEYCLOAK_REALM_NAME)
            .adminPassword(KEYCLOAK_ADMIN_PASSWORD)
            .adminUsername(KEYCLOAK_ADMIN_USERNAME)
            .clientId("client-custom-id")
            .keycloakBindAddress(KEYCLOAK_INSTANCE_HOSTNAME)
            .keycloakHttpPort(KEYCLOAK_EXPOSED_HTTP_PORT)
            .build();

    @ClassRule
    public static TestRule ruleChain = RuleChain.outerRule(keycloakContainer)
            .around(keycloakConfigurator);

    @Deployment
    public static Archive<?> createDeployment() {
        //visit http://localhost:8179/auth/realms/foobar/.well-known/openid-configuration with running keycloak for values
        final String mpProperties = "mp.jwt.verify.publickey.location=http://localhost:8179/auth/realms/%1$s/protocol/openid-connect/certs%n"
                +
                "mp.jwt.verify.issuer=http://localhost:8179/auth/realms/%1$s";

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
        final String rawToken = keycloakConfigurator.getRawJwtFromKeyCloak(username, password);

        given().header("Authorization", "Bearer " + rawToken)
                .when().get(url.toExternalForm() + Endpoints.SECURED_ENDPOINT)
                .then()
                .body(equalTo(rawToken))
                .statusCode(200);
    }

    private static boolean isContainerReady(int port) {
        try {
            URL url = new URL("http://" + KEYCLOAK_INSTANCE_HOSTNAME + ":" + port + "/auth");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            return connection.getResponseCode() == HttpURLConnection.HTTP_OK;
        } catch (Exception ex) {
            return false;
        }
    }

}
