package org.jboss.eap.qe.microprofile.jwt.keycloak;

import static io.restassured.RestAssured.given;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;

import org.jboss.eap.qe.microprofile.jwt.testapp.Roles;
import org.junit.rules.ExternalResource;

/**
 * Keycloak configurator specifically for JWT purposes. It configures a realm, a client and then allows adding users and
 * generating JWTs for them.
 */
//See https://medium.com/@bcarunmail/securing-rest-api-using-keycloak-and-spring-oauth2-6ddf3a1efcc2
public class KeycloakConfigurator extends ExternalResource {

    private final String realmName;

    private final String clientId;

    private final String adminUsername;
    private final String adminPassword;

    private final URL baseApiUrl;

    private String rawAdminToken;

    @Override
    public void before() {
        rawAdminToken = authorizeAndObtainRawJwtForAdminInterface(adminUsername, adminPassword);
        createRealmOnKeycloak(this.realmName);
        createClientOnKeycloak(this.realmName, this.clientId);
        createClientRole(this.realmName, this.clientId, "USER");
        createGroupOnKeycloak(Roles.MONITOR);
    }

    @Override
    public void after() {
        //do nothing
    }

    private String authorizeAndObtainRawJwtForAdminInterface(final String username, final String password) {
        final String jsonResponse = given().header("Content-Type", "application/x-www-form-urlencoded")
                .param("username", username)
                .param("password", password)
                .param("grant_type", "password")
                .param("client_id", "admin-cli")
                .post(this.baseApiUrl.toExternalForm() + "/realms/master/protocol/openid-connect/token")
                .body().asString();
        return extractRawTokenFromJson(jsonResponse);
    }

    private void createRealmOnKeycloak(final String realmName) {
        given().header("Authorization", "Bearer " + this.rawAdminToken)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .body(Json.createObjectBuilder()
                        .add("realm", realmName)
                        .add("displayName", realmName)
                        .add("enabled", true)
                        .build().toString())
                .post(this.baseApiUrl + "/admin/realms")
                .then()
                .statusCode(201);

        // From Keycloak 24 on, VerifyProfile action is enabled by default for new users in the realm, and it may require users
        // to update profile on the first login, which we don't need for tests.
        // https://www.keycloak.org/docs/latest/upgrading/index.html#verify-profile-required-action-enabled-by-default
        // Let's disable (by simply deleting it) it as upstream is disabling it in tests too
        // https://github.com/keycloak/keycloak/pull/26561/files#diff-dbb790e97fd89883d9bb2731bfeadf8276d937fda21a474d2df08bddfd39c654R518-R529
        given().header("Authorization", "Bearer " + this.rawAdminToken)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .delete(this.baseApiUrl + "/admin/realms/" + realmName + "/authentication/required-actions/VERIFY_PROFILE")
                .then()
                .statusCode(204);
    }

    private void createClientOnKeycloak(final String realmName, final String clientId) {
        given().header("Authorization", "Bearer " + this.rawAdminToken)
                .header("Content-Type", "application/json")
                .body(Json.createObjectBuilder()
                        .add("id", clientId)
                        .add("clientId", clientId)
                        .add("protocol", "openid-connect")
                        .add("redirectUris", Json.createArrayBuilder().add("http://localhost:8085")).build().toString())
                .post(this.baseApiUrl.toExternalForm() + "/admin/realms/" + realmName + "/clients")
                .then()
                .statusCode(201);
    }

    private void createClientRole(String realmName, String clientId, String roleName) {
        given().header("Authorization", "Bearer " + this.rawAdminToken)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .body(Json.createObjectBuilder()
                        .add("name", roleName)
                        .build().toString())
                .post(this.baseApiUrl.toExternalForm() + "/admin/realms/" + realmName + "/clients/" + clientId + "/roles")
                .then()
                .statusCode(201);
    }

    private void createGroupOnKeycloak(final String groupName) {
        given().header("Authorization", "Bearer " + this.rawAdminToken)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .body(Json.createObjectBuilder()
                        .add("name", groupName)
                        .build().toString())
                .post(this.baseApiUrl.toExternalForm() + "/admin/realms/" + realmName + "/groups")
                .then()
                .statusCode(201);
    }

    /**
     * Create new user on Keycloak server. Realm name and client ID are pre-defined.
     *
     * @param username a username
     * @param password a password
     */
    public void addUser(final String username, final String password) {
        given().header("Authorization", "Bearer " + this.rawAdminToken)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .body(Json.createObjectBuilder()
                        .add("username", username)
                        .add("enabled", true)
                        .add("credentials", Json.createArrayBuilder()
                                .add(Json.createObjectBuilder()
                                        .add("temporary", false)
                                        .add("type", "password")
                                        .add("value", password)))
                        .add("clientRoles",
                                Json.createObjectBuilder().add(this.clientId, Json.createArrayBuilder().add("USER")))
                        .add("groups", Json.createArrayBuilder().add(Roles.MONITOR))
                        .build().toString())
                .post(this.baseApiUrl + "/admin/realms/" + this.realmName + "/users")
                .then()
                .statusCode(201);
    }

    /**
     * Generate raw JWT for a user
     *
     * @param username a username
     * @param password a matching password
     * @return a raw JWT
     */
    public String getRawJwtFromKeyCloak(final String username, final String password) {
        return extractRawTokenFromJson(given().header("Content-Type", "application/x-www-form-urlencoded")
                .param("grant_type", "password")
                .param("username", username)
                .param("password", password)
                .param("client_id", this.clientId)
                .post(this.baseApiUrl.toExternalForm() + "/realms/" + this.realmName + "/protocol/openid-connect/token")
                .body()
                .asString());
    }

    private String extractRawTokenFromJson(String jsonResponse) {
        final JsonReader reader = Json.createReader(new StringReader(jsonResponse));
        final JsonObject jsonTokenObject = reader.readObject();
        reader.close();
        return jsonTokenObject.getString("access_token");
    }

    public KeycloakConfigurator(Builder builder) {
        this.realmName = builder.realmName;
        this.clientId = builder.clientId;
        this.adminPassword = builder.adminPassword;
        this.adminUsername = builder.adminUsername;
        try {
            this.baseApiUrl = new URL("http", builder.keycloakBindAddress, builder.keycloakHttpPort, "");
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Malformed URL!", e);
        }
    }

    public static final class Builder {

        private final String realmName;

        private String clientId;

        private String adminUsername;
        private String adminPassword;

        private int keycloakHttpPort;
        private String keycloakBindAddress;

        public Builder(final String realmName) {
            this.realmName = realmName;
        }

        /**
         * Set ID of keycloak client which will be used for both client ID and its name
         *
         * @param clientId client ID
         * @return instance of this builder
         */
        public Builder clientId(final String clientId) {
            this.clientId = clientId;
            return this;
        }

        /**
         * Set username for Keycloak administrator
         *
         * @param adminUsername a username
         * @return instance of this builder
         */
        public Builder adminUsername(final String adminUsername) {
            this.adminUsername = adminUsername;
            return this;
        }

        /**
         * Set password for Keycloak administrator
         *
         * @param adminPassword a adminPassword
         * @return instance of this builder
         */
        public Builder adminPassword(final String adminPassword) {
            this.adminPassword = adminPassword;
            return this;
        }

        /**
         * A hostname on which the Keycloak instance is available
         *
         * @param keycloakBindAddress a hostname
         * @return instance of this builder
         */
        public Builder keycloakBindAddress(final String keycloakBindAddress) {
            this.keycloakBindAddress = keycloakBindAddress;
            return this;
        }

        /**
         * A port on which the Keycloak instance is available
         *
         * @param keycloakHttpPort a port
         * @return instance of this builder
         */
        public Builder keycloakHttpPort(final int keycloakHttpPort) {
            this.keycloakHttpPort = keycloakHttpPort;
            return this;
        }

        public KeycloakConfigurator build() {
            return new KeycloakConfigurator(this);
        }

    }
}
