package org.jboss.eap.qe.microprofile.jwt.security.keyselection;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

import java.net.URISyntaxException;
import java.net.URL;

import javax.json.Json;
import javax.json.JsonObject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.eap.qe.microprofile.jwt.auth.tool.JsonWebToken;
import org.jboss.eap.qe.microprofile.jwt.auth.tool.JwtHelper;
import org.jboss.eap.qe.microprofile.jwt.auth.tool.RsaKeyTool;
import org.jboss.eap.qe.microprofile.jwt.testapp.Endpoints;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Set of tests verifying key selection functionality. See chapter 9.2.3. JSON Web Key Set (JWKS) of MP-JWT 1.1
 * This set of tests uses {@code mp.jwt.verify.publickey.location} property to configure public keys on server.
 */
@RunAsClient
@RunWith(Arquillian.class)
public class MultipleConfiguredPublicKeysSelectionLocationPropTest {

    private static final String DEPLOYMENT_WITH_BASE64_JWKS_LOCATION_PROPERTY = "deployment-base64-jwks-location";
    private static final String DEPLOYMENT_WITH_JSON_JWKS_LOCATION_PROPERTY = "deployment-json-jwks-location";

    /**
     * Colors in names of following variables are just replacing numbers for easier identification
     */
    private static final String BLUE_KEY = "blue-key";
    private static final String ORANGE_KEY = "orange-key";
    private static final String PINK_KEY = "pink-key";

    private static RsaKeyTool rsaKeyToolOrange = initializeKeyTool("pki/key.private.pkcs8.pem", ORANGE_KEY);
    private static RsaKeyTool rsaKeyToolBlue = initializeKeyTool("pki/key4096.private.pkcs8.pem", BLUE_KEY);
    private static RsaKeyTool rsaKeyToolPink = initializeKeyTool("pki/key2048_2.private.pkcs8.pem", PINK_KEY);

    private static JsonObject JWKS_DOCUMENT = Json.createObjectBuilder()
            .add("keys", Json.createArrayBuilder()
                    .add(rsaKeyToolOrange.getJwkPublicKeyObject())
                    .add(rsaKeyToolBlue.getJwkPublicKeyObject())
                    .build())
            .build();

    @Deployment(name = DEPLOYMENT_WITH_BASE64_JWKS_LOCATION_PROPERTY)
    public static WebArchive createDeploymentWithBase64JwksLocation() {
        return new DeploymentBuilder(DEPLOYMENT_WITH_BASE64_JWKS_LOCATION_PROPERTY
                + MultipleConfiguredPublicKeysSelectionLocationPropTest.class.getSimpleName() + ".war")
                        .jwksObject(JWKS_DOCUMENT)
                        .passAsInlineValue(false)
                        .base64encodedJwks(true)
                        .build();
    }

    @Deployment(name = DEPLOYMENT_WITH_JSON_JWKS_LOCATION_PROPERTY)
    public static WebArchive createDeploymentWithJsonJwksLocation() {
        return new DeploymentBuilder(DEPLOYMENT_WITH_JSON_JWKS_LOCATION_PROPERTY
                + MultipleConfiguredPublicKeysSelectionLocationPropTest.class.getSimpleName() + ".war")
                        .jwksObject(JWKS_DOCUMENT)
                        .passAsInlineValue(false)
                        .base64encodedJwks(false)
                        .build();
    }

    /**
     * @tpTestDetails A JWT signed by a "orange" private key is send to the server which has configured public key to be
     *                a JSON Web Key set in JSON. There are multiple keys in this set and correct one must be chosen based on
     *                {@code kid} value in JOSE header of JWT.
     * @tpPassCrit Correct public key from the JWKS is chosen and client receives token raw value in response.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @OperateOnDeployment(DEPLOYMENT_WITH_JSON_JWKS_LOCATION_PROPERTY)
    public void testJwtSignedByOrangeKeyJsonPk(@ArquillianResource URL url) {
        JsonWebToken token = new JwtHelper(rsaKeyToolOrange).generateProperSignedJwt();

        given().header("Authorization", "Bearer " + token.getRawValue())
                .when().get(url.toExternalForm() + Endpoints.SECURED_ENDPOINT)
                .then()
                .statusCode(200)
                .body(equalTo(token.getRawValue()));
    }

    /**
     * @tpTestDetails A JWT signed by a "blue" private key is send to the server which has configured public key to be a
     *                JSON Web Key set in JSON. There are multiple keys in this set and correct one must be chosen based on
     *                {@code kid} value in JOSE header of JWT.
     * @tpPassCrit Correct public key from the JWKS is chosen and client receives token raw value in response.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @OperateOnDeployment(DEPLOYMENT_WITH_JSON_JWKS_LOCATION_PROPERTY)
    public void testJwtSignedByBlueKeyJsonPk(@ArquillianResource URL url) {
        JsonWebToken token = new JwtHelper(rsaKeyToolBlue).generateProperSignedJwt();

        given().header("Authorization", "Bearer " + token.getRawValue())
                .when().get(url.toExternalForm() + Endpoints.SECURED_ENDPOINT)
                .then()
                .statusCode(200)
                .body(equalTo(token.getRawValue()));
    }

    /**
     * @tpTestDetails A JWT signed by a "pink" private key is send to the server which has configured public key to be a
     *                JSON Web Key set in JSON. There are multiple keys in this set and there is no "pink" public key among
     *                them.
     * @tpPassCrit JWt is rejected and user receives 401/forbidden because there is no matching configured public key on
     *             the server.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @OperateOnDeployment(DEPLOYMENT_WITH_JSON_JWKS_LOCATION_PROPERTY)
    public void testJwtSignedByPinkKeyJsonPk(@ArquillianResource URL url) {
        JsonWebToken token = new JwtHelper(rsaKeyToolPink).generateProperSignedJwt();

        given().header("Authorization", "Bearer " + token.getRawValue())
                .when().get(url.toExternalForm() + Endpoints.SECURED_ENDPOINT)
                .then()
                .statusCode(401);
    }

    /**
     * @tpTestDetails A JWT signed by a "orange" private key is send to the server which has configured public key to be
     *                a JSON Web Key set Base64 encoded. There are multiple keys in this set and correct one must be chosen
     *                based on {@code kid} value in JOSE header of JWT.
     * @tpPassCrit Correct public key from the JWKS is chosen and client receives token raw value in response.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @OperateOnDeployment(DEPLOYMENT_WITH_BASE64_JWKS_LOCATION_PROPERTY)
    public void testJwtSignedByOrangeKeyBase64(@ArquillianResource URL url) {
        JsonWebToken token = new JwtHelper(rsaKeyToolOrange).generateProperSignedJwt();

        given().header("Authorization", "Bearer " + token.getRawValue())
                .when().get(url.toExternalForm() + Endpoints.SECURED_ENDPOINT)
                .then()
                .statusCode(200)
                .body(equalTo(token.getRawValue()));
    }

    /**
     * @tpTestDetails A JWT signed by a "blue" private key is send to the server which has configured public key to be
     *                a JSON Web Key set Base64 encoded. There are multiple keys in this set and correct one must be
     *                chosen based on {@code kid} value in JOSE header of JWT.
     * @tpPassCrit Correct public key from the JWKS is chosen and client receives token raw value in response.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @OperateOnDeployment(DEPLOYMENT_WITH_BASE64_JWKS_LOCATION_PROPERTY)
    public void testJwtSignedByBlueKeyBase64(@ArquillianResource URL url) {
        JsonWebToken token = new JwtHelper(rsaKeyToolBlue).generateProperSignedJwt();

        given().header("Authorization", "Bearer " + token.getRawValue())
                .when().get(url.toExternalForm() + Endpoints.SECURED_ENDPOINT)
                .then()
                .statusCode(200)
                .body(equalTo(token.getRawValue()));
    }

    /**
     * @tpTestDetails A JWT signed by a "pink" private key is send to the server which has configured public key to be a
     *                JSON Web Key set Base64 encoded. There are multiple keys in this set and there is no "pink" public
     *                key among them.
     * @tpPassCrit JWt is rejected and user receives 401/forbidden because there is no matching configured public key on
     *             the server.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @OperateOnDeployment(DEPLOYMENT_WITH_BASE64_JWKS_LOCATION_PROPERTY)
    public void testJwtSignedByPinkKeyBase64Pk(@ArquillianResource URL url) {
        JsonWebToken token = new JwtHelper(rsaKeyToolPink).generateProperSignedJwt();

        given().header("Authorization", "Bearer " + token.getRawValue())
                .when().get(url.toExternalForm() + Endpoints.SECURED_ENDPOINT)
                .then()
                .statusCode(401);
    }

    private static URL getFileFromResources(final String filePath) {
        final URL privateKeyUrl = MultipleConfiguredPublicKeysSelectionLocationPropTest.class.getClassLoader()
                .getResource(filePath);
        if (privateKeyUrl == null) {
            throw new IllegalStateException("File wasn't found in resources!");
        }
        return privateKeyUrl;
    }

    private static RsaKeyTool initializeKeyTool(final String filePath, final String keyId) {
        try {
            return RsaKeyTool.newKeyTool(getFileFromResources(filePath).toURI(), keyId);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

}
