package org.jboss.eap.qe.microprofile.jwt.compatibility;

import static io.restassured.RestAssured.given;

import java.net.URISyntaxException;
import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.eap.qe.microprofile.jwt.EnableJwtSubsystemRule;
import org.jboss.eap.qe.microprofile.jwt.auth.tool.JsonWebToken;
import org.jboss.eap.qe.microprofile.jwt.auth.tool.JwtHelper;
import org.jboss.eap.qe.microprofile.jwt.auth.tool.RsaKeyTool;
import org.jboss.eap.qe.microprofile.jwt.testapp.Endpoints;
import org.jboss.eap.qe.microprofile.jwt.testapp.jaxrs.JaxRsTestApplication;
import org.jboss.eap.qe.microprofile.jwt.testapp.jaxrs.SecuredJaxRsEndpoint;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Issuer (iss) claim is specific amongst other claims. When JWT is sent to the server, value of issuer claim is
 * compared to values set to server which should be expected. If there is no match such JWT is rejected.
 */
@RunAsClient
@RunWith(Arquillian.class)
public class JwtIssuerValueTest {

    @ClassRule
    public static EnableJwtSubsystemRule enableJwtSubsystemRule = new EnableJwtSubsystemRule();

    private static RsaKeyTool keyTool;

    @BeforeClass
    public static void beforeClass() throws URISyntaxException {
        final URL privateKeyUrl = JwtIssuerValueTest.class.getClassLoader().getResource("pki/key.private.pkcs8.pem");
        if (privateKeyUrl == null) {
            throw new IllegalStateException("Private key wasn't found in resources!");
        }

        keyTool = RsaKeyTool.newKeyTool(privateKeyUrl.toURI());
    }

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, JwtIssuerValueTest.class.getSimpleName() + ".war")
                .addClass(SecuredJaxRsEndpoint.class)
                .addClass(JaxRsTestApplication.class)
                .addAsManifestResource(JwtIssuerValueTest.class.getClassLoader().getResource("mp-config-basic.properties"),
                        "microprofile-config.properties")
                .addAsManifestResource(JwtIssuerValueTest.class.getClassLoader().getResource("pki/key.public.pem"),
                        "key.public.pem");
    }

    /**
     * @tpTestDetails Supply a JWT with unset issuer claim to the server.
     * @tpPassCrit JWT is rejected and user receives 401/Unauthorized.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void supplyJwtWithUnsetIssuerClaimTest(@ArquillianResource URL url) {
        final JsonWebToken jwt = new JwtHelper(keyTool, "").generateProperSignedJwt();

        given().header("Authorization", "Bearer " + jwt.getRawValue())
                .when().get(url.toExternalForm() + Endpoints.SECURED_ENDPOINT)
                .then()
                .statusCode(401);
    }

    /**
     * @tpTestDetails Supply a JWT with issuer claim value set to unexpected value. This value generally doesn't match
     *                the value set to the runtime which will be used for comparision meaning this JWT should be
     *                rejected.
     * @tpPassCrit JWT is rejected and user receives 401/Unauthorized.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void supplyJwtWithIncorrectIssuerClaimTest(@ArquillianResource URL url) {
        final JsonWebToken jwt = new JwtHelper(keyTool, "fooqux42").generateProperSignedJwt();

        given().header("Authorization", "Bearer " + jwt.getRawValue())
                .when().get(url.toExternalForm() + Endpoints.SECURED_ENDPOINT)
                .then()
                .statusCode(401);
    }

}
