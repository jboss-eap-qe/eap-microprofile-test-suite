package org.jboss.eap.qe.microprofile.jwt.security.keyproperties;

import static org.hamcrest.CoreMatchers.equalTo;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.eap.qe.microprofile.jwt.auth.tool.JsonWebToken;
import org.jboss.eap.qe.microprofile.jwt.auth.tool.JwtHelper;
import org.jboss.eap.qe.microprofile.jwt.auth.tool.RsaKeyTool;
import org.jboss.eap.qe.microprofile.jwt.testapp.Endpoints;
import org.jboss.eap.qe.microprofile.jwt.testapp.jaxrs.JaxRsTestApplication;
import org.jboss.eap.qe.microprofile.jwt.testapp.jaxrs.SecuredJaxRsEndpoint;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.restassured.RestAssured;

@RunAsClient
@RunWith(Arquillian.class)
public class KeySizeTestCase {

    private static final String BITS_512_KEY_DEPLOYMENT = "512-bits-key-deployment";
    private static final String BITS_1024_KEY_DEPLOYMENT = "1024-bits-key-deployment";
    private static final String BITS_2048_KEY_DEPLOYMENT = "2048-bits-key-deployment";
    private static final String BITS_4096_KEY_DEPLOYMENT = "4096-bits-key-deployment";

    @Deployment(name = BITS_512_KEY_DEPLOYMENT)
    public static WebArchive create512keyDeployment() {
        return ShrinkWrap.create(WebArchive.class, BITS_512_KEY_DEPLOYMENT + ".war")
                .addClass(SecuredJaxRsEndpoint.class)
                .addClass(JaxRsTestApplication.class)
                .addAsManifestResource(KeySizeTestCase.class.getClassLoader().getResource("mp-config-basic.properties"),
                        "microprofile-config.properties")
                .addAsManifestResource(KeySizeTestCase.class.getClassLoader().getResource("pki/key512.public.pem"),
                        "key.public.pem");
    }

    @Deployment(name = BITS_1024_KEY_DEPLOYMENT)
    public static WebArchive create1024keyDeployment() {
        return ShrinkWrap.create(WebArchive.class, BITS_1024_KEY_DEPLOYMENT + ".war")
                .addClass(SecuredJaxRsEndpoint.class)
                .addClass(JaxRsTestApplication.class)
                .addAsManifestResource(KeySizeTestCase.class.getClassLoader().getResource("mp-config-basic.properties"),
                        "microprofile-config.properties")
                .addAsManifestResource(KeySizeTestCase.class.getClassLoader().getResource("pki/key1024.public.pem"),
                        "key.public.pem");
    }

    @Deployment(name = BITS_2048_KEY_DEPLOYMENT)
    public static WebArchive create2048keyDeployment() {
        return ShrinkWrap.create(WebArchive.class, BITS_2048_KEY_DEPLOYMENT + ".war")
                .addClass(SecuredJaxRsEndpoint.class)
                .addClass(JaxRsTestApplication.class)
                .addAsManifestResource(KeySizeTestCase.class.getClassLoader().getResource("mp-config-basic.properties"),
                        "microprofile-config.properties")
                .addAsManifestResource(KeySizeTestCase.class.getClassLoader().getResource("pki/key.public.pem"),
                        "key.public.pem");
    }

    @Deployment(name = BITS_4096_KEY_DEPLOYMENT)
    public static WebArchive create4096keyDeployment() {
        return ShrinkWrap.create(WebArchive.class, BITS_4096_KEY_DEPLOYMENT + ".war")
                .addClass(SecuredJaxRsEndpoint.class)
                .addClass(JaxRsTestApplication.class)
                .addAsManifestResource(KeySizeTestCase.class.getClassLoader().getResource("mp-config-basic.properties"),
                        "microprofile-config.properties")
                .addAsManifestResource(KeySizeTestCase.class.getClassLoader().getResource("pki/key4096.public.pem"),
                        "key.public.pem");
    }

    /**
     * @tpTestDetails Test specification compatibility by authenticating against server with a token signed with a valid
     *                key 1024 bits long. Proper public key is supplied to server.
     * @tpPassCrit Authentication is successful and client receives raw token value in response.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @OperateOnDeployment(BITS_1024_KEY_DEPLOYMENT)
    public void testJwtSignedBy1024bitsKey(@ArquillianResource URL url) throws URISyntaxException {
        final RsaKeyTool rsaKeyTool = RsaKeyTool.newKeyTool(getPrivateKey("key1024.private.pkcs8.pem"));

        JsonWebToken token = new JwtHelper(rsaKeyTool).generateProperSignedJwt();

        RestAssured.given().header("Authorization", "Bearer " + token.getRawValue())
                .when().get(url.toExternalForm() + Endpoints.SECURED_ENDPOINT)
                .then().body(equalTo(token.getRawValue()));
    }

    /**
     * @tpTestDetails Test specification compatibility by authenticating against server with a token signed with a valid
     *                key 2048 bits long. Proper public key is supplied to server.
     * @tpPassCrit Authentication is successful and client receives raw token value in response.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @OperateOnDeployment(BITS_2048_KEY_DEPLOYMENT)
    public void testJwtSignedBy2048bitsKey(@ArquillianResource URL url) throws URISyntaxException {
        final RsaKeyTool rsaKeyTool = RsaKeyTool.newKeyTool(getPrivateKey("key.private.pkcs8.pem"));

        JsonWebToken token = new JwtHelper(rsaKeyTool).generateProperSignedJwt();

        RestAssured.given().header("Authorization", "Bearer " + token.getRawValue())
                .when().get(url.toExternalForm() + Endpoints.SECURED_ENDPOINT)
                .then().body(equalTo(token.getRawValue()));
    }

    /**
     * @tpTestDetails Test specification compatibility by authenticating against server with a token signed with a valid
     *                key 2048 bits long. Proper public key is supplied to server.
     * @tpPassCrit Authentication is successful and client receives raw token value in response.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @OperateOnDeployment(BITS_4096_KEY_DEPLOYMENT)
    public void testJwtSignedBy4096bitsKey(@ArquillianResource URL url) throws URISyntaxException {
        final RsaKeyTool rsaKeyTool = RsaKeyTool.newKeyTool(getPrivateKey("key4096.private.pkcs8.pem"));

        JsonWebToken token = new JwtHelper(rsaKeyTool).generateProperSignedJwt();

        RestAssured.given().header("Authorization", "Bearer " + token.getRawValue())
                .when().get(url.toExternalForm() + Endpoints.SECURED_ENDPOINT)
                .then().body(equalTo(token.getRawValue()));
    }

    /**
     * @tpTestDetails Test specification compatibility by authenticating against server with a token signed with a valid
     *                key 512 bits long. This is a negative test scenario and should not succeed since MP-JWT 1.1 supports only
     *                1024
     *                and 2048 bits long keys. Proper public key is supplied to server.
     * @tpPassCrit Authentication is not successful and client receives "unauthorized" response.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @OperateOnDeployment(BITS_512_KEY_DEPLOYMENT)
    public void testJwtSignedBy512bitsKey(@ArquillianResource URL url) throws URISyntaxException {
        final RsaKeyTool rsaKeyTool = RsaKeyTool.newKeyTool(getPrivateKey("key512.private.pkcs8.pem"));

        final JsonWebToken token = new JwtHelper(rsaKeyTool).generateProperSignedJwt();

        RestAssured.given()
                .header("Authorization", "Bearer " + token.getRawValue())
                .when().get(url.toExternalForm() + Endpoints.SECURED_ENDPOINT)
                .then()
                .body(equalTo("<html><head><title>Error</title></head><body>Unauthorized</body></html>"))
                .and()
                .statusCode(401);
    }

    private URI getPrivateKey(final String fileName) throws URISyntaxException {
        final URL privateKeyUrl = KeySizeTestCase.class.getClassLoader().getResource("pki/" + fileName);
        if (privateKeyUrl == null) {
            throw new IllegalStateException("Private key wasn't found in resources!");
        }
        return privateKeyUrl.toURI();
    }

}
