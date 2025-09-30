package org.jboss.eap.qe.microprofile.jwt.keycloak;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.eap.qe.microprofile.jwt.EnableJwtSubsystemSetupTask;
import org.jboss.eap.qe.microprofile.jwt.testapp.Endpoints;
import org.jboss.eap.qe.microprofile.jwt.testapp.jaxrs.JaxRsTestApplication;
import org.jboss.eap.qe.microprofile.jwt.testapp.jaxrs.SecuredJaxRsEndpoint;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.ConfigurationException;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.jboss.eap.qe.microprofile.tooling.server.log.LogChecker;
import org.jboss.eap.qe.microprofile.tooling.server.log.ModelNodeLogChecker;
import org.jboss.eap.qe.ts.common.docker.Docker;
import org.jboss.eap.qe.ts.common.docker.junit.DockerRequiredTests;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;

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

    private static final String ANOTHER_KEYCLOAK_REALM_NAME = "foobar2";

    private static final String KEYCLOAK_INSTANCE_HOSTNAME = "localhost";

    private static final String KEYCLOAK_CONTAINER_NAME = KeycloakIntegrationHighLevelScenarioTest.class.getSimpleName()
            + "-container";

    private static final String DEPLOYMENT_1_NAME = "deployment1";

    private static final String DEPLOYMENT_2_NAME = "deployment2";

    private static final String DEPLOYMENT_3_NAME = "deployment3";

    private static final int KEYCLOAK_EXPOSED_HTTP_PORT = 8179;

    private static final String KEYCLOAK_ADMIN_USERNAME = "admin";
    private static final String KEYCLOAK_ADMIN_PASSWORD = "password";

    public static Docker keycloakContainer = new Docker.Builder(KEYCLOAK_CONTAINER_NAME,
            "quay.io/keycloak/keycloak:24.0")
            .setContainerReadyTimeout(2, TimeUnit.MINUTES)
            .setContainerReadyCondition(() -> isContainerReady(KEYCLOAK_EXPOSED_HTTP_PORT))
            .withPortMapping(KEYCLOAK_EXPOSED_HTTP_PORT + ":8080")
            .withEnvVar("KEYCLOAK_ADMIN", KEYCLOAK_ADMIN_USERNAME)
            .withEnvVar("KEYCLOAK_ADMIN_PASSWORD", KEYCLOAK_ADMIN_PASSWORD)
            .withEnvVar("KC_HEALTH_ENABLED", "true")
            .withCmdArg("start-dev")
            .build();

    /**
     * Configures the {@link KeycloakIntegrationHighLevelScenarioTest#KEYCLOAK_REALM_NAME} realm in Keycloak
     */
    public static KeycloakConfigurator keycloakConfigurator = new KeycloakConfigurator.Builder(KEYCLOAK_REALM_NAME)
            .adminPassword(KEYCLOAK_ADMIN_PASSWORD)
            .adminUsername(KEYCLOAK_ADMIN_USERNAME)
            .clientId("client-custom-id")
            .keycloakBindAddress(KEYCLOAK_INSTANCE_HOSTNAME)
            .keycloakHttpPort(KEYCLOAK_EXPOSED_HTTP_PORT)
            .build();

    /**
     * Configures the {@link KeycloakIntegrationHighLevelScenarioTest#ANOTHER_KEYCLOAK_REALM_NAME} realm in Keycloak
     */
    public static KeycloakConfigurator anotherKeycloakConfigurator = new KeycloakConfigurator.Builder(
            ANOTHER_KEYCLOAK_REALM_NAME)
            .adminPassword(KEYCLOAK_ADMIN_PASSWORD)
            .adminUsername(KEYCLOAK_ADMIN_USERNAME)
            .clientId("another-client-custom-id")
            .keycloakBindAddress(KEYCLOAK_INSTANCE_HOSTNAME)
            .keycloakHttpPort(KEYCLOAK_EXPOSED_HTTP_PORT)
            .build();

    /**
     * Rules the existence of a Keycloak DOcker container which is configured with two realms, i.e.
     * {@link KeycloakIntegrationHighLevelScenarioTest#KEYCLOAK_REALM_NAME} and
     * {@link KeycloakIntegrationHighLevelScenarioTest#ANOTHER_KEYCLOAK_REALM_NAME}
     */
    @ClassRule
    public static TestRule keycloakRuleChain = RuleChain.outerRule(keycloakContainer)
            .around(keycloakConfigurator)
            .around(anotherKeycloakConfigurator);

    /**
     * Builds a WAR that represents the first deployment, i.e. a simple Jakarta RESTful application which
     * exposes one secured endpoint, i.e. {@link SecuredJaxRsEndpoint}, and is configured via MicroProfile Config to
     * set the MicroProfile JWT key location to the one held by the
     * {@link KeycloakIntegrationHighLevelScenarioTest#KEYCLOAK_REALM_NAME} Keycloak realm.
     *
     * @return A WAR archive representing the first deployment.
     */
    @Deployment(name = DEPLOYMENT_1_NAME, testable = false)
    public static Archive<?> firstDeployment() {
        return createDeployment(DEPLOYMENT_1_NAME, KEYCLOAK_REALM_NAME);
    }

    /**
     * Builds a WAR that represents the second deployment, identical to
     * {@link KeycloakIntegrationHighLevelScenarioTest#firstDeployment()}
     *
     * @return A WAR archive representing the second deployment.
     */
    @Deployment(name = DEPLOYMENT_2_NAME, testable = false)
    public static Archive<?> anotherDeployment() {
        return createDeployment(DEPLOYMENT_2_NAME, KEYCLOAK_REALM_NAME);
    }

    /**
     * Builds a WAR that represents the third deployment, i.e. a simple Jakarta RESTful application which
     * exposes one secured endpoint, i.e. {@link SecuredJaxRsEndpoint}, and is configured via MicroProfile Config to
     * set the MicroProfile JWT key location to the one held by the
     * {@link KeycloakIntegrationHighLevelScenarioTest#ANOTHER_KEYCLOAK_REALM_NAME} Keycloak realm.
     *
     * @return A WAR archive representing the third deployment.
     */
    @Deployment(name = DEPLOYMENT_3_NAME, testable = false)
    public static Archive<?> aDifferentRealmDeployment() {
        return createDeployment(DEPLOYMENT_3_NAME, ANOTHER_KEYCLOAK_REALM_NAME);
    }

    private static Archive<?> createDeployment(final String deploymentName, final String realmName) {
        //visit http://localhost:8179/auth/realms/foobar/.well-known/openid-configuration with running keycloak for values
        final String mpProperties = "mp.jwt.verify.publickey.location=http://localhost:8179/realms/%1$s/protocol/openid-connect/certs%n"
                +
                "mp.jwt.verify.issuer=http://localhost:8179/realms/%1$s";

        return ShrinkWrap.create(WebArchive.class, deploymentName + ".war")
                .addClass(SecuredJaxRsEndpoint.class)
                .addClass(JaxRsTestApplication.class)
                .addAsManifestResource(
                        new StringAsset(String.format(mpProperties, realmName)),
                        "microprofile-config.properties");
    }

    @ArquillianResource
    @OperateOnDeployment(DEPLOYMENT_1_NAME)
    URL deployment1Url;

    @ArquillianResource
    @OperateOnDeployment(DEPLOYMENT_2_NAME)
    URL deployment2Url;

    @ArquillianResource
    @OperateOnDeployment(DEPLOYMENT_3_NAME)
    URL deployment3Url;

    /**
     * @tpTestDetails A high level scenario in which a Keycloak instance is used to generate a JWT which will be
     *                supplied to the server. Two identical applications are deployed, and the same user JWT will be used to
     *                authenticate against both the deployments.
     * @tpPassCrit Both applications accept the same JWT and return its raw form to the client.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void bothDeploymentsAccessSecuredEndpointWithSameRealmKeycloakProvidedJwt() {
        final String username = "qux";
        final String password = "foo";
        keycloakConfigurator.addUser(username, password);
        final String rawToken = keycloakConfigurator.getRawJwtFromKeyCloakForTestUser(username, password);

        given().header("Authorization", "Bearer " + rawToken)
                .when().get(deployment1Url.toExternalForm() + Endpoints.SECURED_ENDPOINT)
                .then()
                .body(equalTo(rawToken))
                .statusCode(200);

        given().header("Authorization", "Bearer " + rawToken)
                .when().get(deployment2Url.toExternalForm() + Endpoints.SECURED_ENDPOINT)
                .then()
                .body(equalTo(rawToken))
                .statusCode(200);
    }

    /**
     * @tpTestDetails A high level scenario in which a Keycloak instance is used to generate a JWT which will be
     *                supplied to the server.
     *                A JWT generated by a dedicated Keycloak realm will be used to authenticate against the deployment which is
     *                configured to use such realm, demonstrating that parallel deployments relating to different realms work
     *                independently.
     * @tpPassCrit The application accepts the JWT and returns its raw form to the client.
     * @tpSince JBoss EAP XP 6
     */
    @Test
    public void authenticationSucceedsOnAnotherDeploymentWithAnotherRealmJWT() {
        final String username = "anotherRealm_user1";
        final String password = "anotherRealm_user1_s3cret!";
        anotherKeycloakConfigurator.addUser(username, password);
        final String rawToken = anotherKeycloakConfigurator.getRawJwtFromKeyCloakForTestUser(username, password);

        given().header("Authorization", "Bearer " + rawToken)
                .when().get(deployment3Url.toExternalForm() + Endpoints.SECURED_ENDPOINT)
                .then()
                .body(equalTo(rawToken))
                .statusCode(200);
    }

    /**
     * @tpTestDetails A high level scenario in which a Keycloak instance is used to generate a JWT which will be
     *                supplied to the server.
     *                A JWT generated by a Keycloak realm will be used to authenticate against a deployment which is configured
     *                to use
     *                a key provided by a different Keycloak realm.
     * @tpPassCrit The application reports HTTP 500 and the server logs contain a {@code SRJWT11003 DEBUG} message,
     *             indicating that the key is unavailable.
     * @tpSince JBoss EAP XP 6
     */
    @Test
    public void authenticationFailsOnDeploymentOneWithAnotherRealmJWT() throws ConfigurationException {
        final String username = "anotherRealm_user2";
        final String password = "anotherRealm_user2_s3cret!";
        anotherKeycloakConfigurator.addUser(username, password);
        final String rawToken = anotherKeycloakConfigurator.getRawJwtFromKeyCloakForTestUser(username, password);

        given().header("Authorization", "Bearer " + rawToken)
                .when().get(deployment1Url.toExternalForm() + Endpoints.SECURED_ENDPOINT)
                .then()
                .statusCode(500);

        try (final OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
            final LogChecker logChecker = new ModelNodeLogChecker(client, 10, true);
            Assert.assertTrue(logChecker.logContains("SRJWT11003"));
        } catch (IOException e) {
            throw new IllegalStateException("A error occurred while trying to retrieve the server logs", e);
        }
    }

    private static boolean isContainerReady(int port) {
        try {
            URL url = new URL("http://" + KEYCLOAK_INSTANCE_HOSTNAME + ":" + port + "/health/ready");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            boolean ready = connection.getResponseCode() == HttpURLConnection.HTTP_OK;
            if (ready) {
                System.out.println(
                        "Let's wait addional 10 seconds before the post-start tasks (like creation of admin user) on container are done.");
                Thread.sleep(10000L);
            }
            return ready;
        } catch (Exception ex) {
            return false;
        }
    }

}
