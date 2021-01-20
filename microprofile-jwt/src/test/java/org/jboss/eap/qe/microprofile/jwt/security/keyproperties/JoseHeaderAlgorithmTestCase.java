package org.jboss.eap.qe.microprofile.jwt.security.keyproperties;

import static org.hamcrest.CoreMatchers.equalTo;

import java.net.URISyntaxException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.UUID;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.eap.qe.microprofile.jwt.EnableJwtSubsystemSetupTask;
import org.jboss.eap.qe.microprofile.jwt.auth.tool.JoseHeader;
import org.jboss.eap.qe.microprofile.jwt.auth.tool.JsonWebToken;
import org.jboss.eap.qe.microprofile.jwt.auth.tool.JwtClaims;
import org.jboss.eap.qe.microprofile.jwt.auth.tool.JwtDefaultClaimValues;
import org.jboss.eap.qe.microprofile.jwt.auth.tool.JwtHelper;
import org.jboss.eap.qe.microprofile.jwt.auth.tool.RsaKeyTool;
import org.jboss.eap.qe.microprofile.jwt.testapp.Endpoints;
import org.jboss.eap.qe.microprofile.jwt.testapp.jaxrs.JaxRsTestApplication;
import org.jboss.eap.qe.microprofile.jwt.testapp.jaxrs.SecuredJaxRsEndpoint;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.restassured.RestAssured;

@RunAsClient
@RunWith(Arquillian.class)
@ServerSetup(EnableJwtSubsystemSetupTask.class)
public class JoseHeaderAlgorithmTestCase {

    final static private String DEFAULT_DEPLOYMENT = "default-deployment";

    private static RsaKeyTool keyTool;

    @Deployment
    public static WebArchive create2048keyDeployment() {
        return ShrinkWrap.create(WebArchive.class, DEFAULT_DEPLOYMENT + ".war")
                .addClass(SecuredJaxRsEndpoint.class)
                .addClass(JaxRsTestApplication.class)
                .addAsManifestResource(KeySizeTestCase.class.getClassLoader().getResource("mp-config-basic-RS256.properties"),
                        "microprofile-config.properties")
                .addAsManifestResource(KeySizeTestCase.class.getClassLoader().getResource("pki/RS256/key.public.pem"),
                        "key.public.pem");
    }

    @BeforeClass
    public static void beforeClass() throws URISyntaxException {
        final URL privateKeyUrl = JoseHeaderAlgorithmTestCase.class.getClassLoader()
                .getResource("pki/RS256/key.private.pkcs8.pem");
        if (privateKeyUrl == null) {
            throw new IllegalStateException("Private key wasn't found in resources!");
        }
        keyTool = RsaKeyTool.newKeyTool(privateKeyUrl.toURI());
    }

    /**
     * @tpTestDetails JOSE header with {@code alg} set to {@code RS256} must be supported. Verify, such JWT is not
     *                rejected.
     * @tpPassCrit JWT is not rejected and its raw value is returned to client.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testRSA256algorithm(@ArquillianResource URL url) {
        final JsonWebToken token = new JwtHelper(keyTool).generateProperSignedJwt();

        RestAssured.given()
                .header("Authorization", "Bearer " + token.getRawValue())
                .when().get(url.toExternalForm() + Endpoints.SECURED_ENDPOINT)
                .then()
                .body(equalTo(token.getRawValue()))
                .and()
                .statusCode(200);
    }

    /**
     * @tpTestDetails JOSE header with {@code alg} set to {@code RS384} is not supported. Verify, such JWT is rejected.
     * @tpPassCrit JWT is rejected and client receives Unauthorized/401 message.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testOutOfSpecAlgorithm(@ArquillianResource URL url) {
        final JsonWebToken token = prepareJwtWithCustomJoseHeaderSignedWithRS384(new JoseHeader(keyTool.getJwkKeyId(),
                "JWT", "RS384"), keyTool);

        RestAssured.given()
                .header("Authorization", "Bearer " + token.getRawValue())
                .when().get(url.toExternalForm() + Endpoints.SECURED_ENDPOINT)
                .then()
                .body(equalTo("<html><head><title>Error</title></head><body>Unauthorized</body></html>"))
                .and()
                .statusCode(401);
    }

    private JsonWebToken prepareJwtWithCustomJoseHeaderSignedWithRS384(final JoseHeader joseHeader, final RsaKeyTool keyTool) {
        final String subject = JwtDefaultClaimValues.SUBJECT;

        final Instant now = Instant.now();
        final Instant later = now.plus(1, ChronoUnit.HOURS);

        final JwtClaims jwtClaims = new JwtClaims.Builder()
                .jwtId(UUID.randomUUID().toString())
                .audience(JwtDefaultClaimValues.AUDIENCE)
                .subject(subject)
                .userPrincipalName(subject)
                .issuer(JwtDefaultClaimValues.ISSUER)
                .issuedAtTime(now.getEpochSecond())
                .expirationTime(later.getEpochSecond())
                .groups(Collections.emptySet())
                .build();

        try {
            return new JsonWebToken.Builder()
                    .joseHeader(joseHeader)
                    .jwtClaims(jwtClaims)
                    .signature(Signature.getInstance("SHA384withRSA"))
                    .privateKey(keyTool.getPrivateKey())
                    .build();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("RS384 algorithm is not supported by JVM!", e);
        }
    }

}
