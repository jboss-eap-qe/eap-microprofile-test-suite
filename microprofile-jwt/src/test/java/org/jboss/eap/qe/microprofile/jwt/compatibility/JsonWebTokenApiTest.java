package org.jboss.eap.qe.microprofile.jwt.compatibility;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;

import org.eclipse.microprofile.jwt.Claims;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.eap.qe.microprofile.jwt.EnableJwtSubsystemSetupTask;
import org.jboss.eap.qe.microprofile.jwt.auth.tool.JwsHelper;
import org.jboss.eap.qe.microprofile.jwt.auth.tool.JwtDefaultClaimValues;
import org.jboss.eap.qe.microprofile.jwt.testapp.Endpoints;
import org.jboss.eap.qe.microprofile.jwt.testapp.Roles;
import org.jboss.eap.qe.microprofile.jwt.testapp.jaxrs.JaxRsTestApplication;
import org.jboss.eap.qe.microprofile.jwt.testapp.jaxrs.SecuredJaxRsEndpoint;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * As of MicroProfile JWT 1.2 A convenience method has been added to allow retrieving claims from JsonWebToken by using
 * the Claims enum; this test verifies the new API works correctly
 * (see https://github.com/eclipse/microprofile-jwt-auth/issues/154)
 *
 * @author tborgato
 */
@RunAsClient
@RunWith(Arquillian.class)
@ServerSetup(EnableJwtSubsystemSetupTask.class)
public class JsonWebTokenApiTest {

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, JsonWebTokenApiTest.class.getSimpleName() + ".war")
                .addClass(SecuredJaxRsEndpoint.class)
                .addClass(JaxRsTestApplication.class)
                .addAsManifestResource(
                        JsonWebTokenApiTest.class.getClassLoader().getResource("mp-config-basic-ES256.properties"),
                        "microprofile-config.properties")
                .addAsManifestResource(JsonWebTokenApiTest.class.getClassLoader().getResource("pki/ES256/key.public.pem"),
                        "key.public.pem");
    }

    /**
     * A valid signed JWT token is sent to the server and, in the JAX-RS endpoint, the new API method {@code getClaim} is
     * used to extract a claim
     *
     * @tpTestDetails Test invokes a JAX-RS endpoint which leverages the new API method to get the claim passed as parameter
     * @tpPassCrit new API method returns the correct claim
     * @tpSince EAP 7.4.0.CD23
     */
    @Test
    public void testGetClaim(@ArquillianResource URL url) throws Exception {
        final URL jswPrivateKeyUrl = JsonWebTokenApiTest.class.getClassLoader()
                .getResource("pki/ES256/key.private.pkcs8.pem");
        if (jswPrivateKeyUrl == null) {
            throw new IllegalStateException("JWS Private key wasn't found in resources!");
        }

        JwsHelper jwsHelper = new JwsHelper();
        JsonWebSignature jsonWebSignature = jwsHelper.generateProperSignedJwt(
                AlgorithmIdentifiers.ECDSA_USING_P256_CURVE_AND_SHA256,
                jswPrivateKeyUrl.toURI(),
                "k1",
                JwtDefaultClaimValues.ISSUER,
                JwtDefaultClaimValues.AUDIENCE,
                JwtDefaultClaimValues.SUBJECT,
                Arrays.asList(Roles.MONITOR),
                Collections.EMPTY_MAP);
        String jws = jsonWebSignature.getCompactSerialization();

        // test everything works fine when configuration is correct
        given().header("Authorization", "Bearer " + jws)
                .when().get(url.toExternalForm() + Endpoints.SECURED_ENDPOINT + "/" + Claims.iss)
                .then()
                .body(containsString(JwtDefaultClaimValues.ISSUER));
    }
}
