package org.jboss.eap.qe.microprofile.jwt.compatibility;

import static io.restassured.RestAssured.given;

import java.net.URISyntaxException;
import java.net.URL;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.eap.qe.microprofile.jwt.EnableJwtSubsystemSetupTask;
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
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * In chapter 4.1 of MP-JWT 1.1 minimal set of required claims for JWT is recommended in order to ensure
 * interoperability.
 */
@Ignore("WFWIP-293")
@RunAsClient
@RunWith(Arquillian.class)
@ServerSetup(EnableJwtSubsystemSetupTask.class)
public class JwtMinimalClaimSetTest {

    private static final String SUBJECT = JwtDefaultClaimValues.SUBJECT;
    private static final String ISSUER = JwtDefaultClaimValues.ISSUER;

    private static RsaKeyTool keyTool;

    @BeforeClass
    public static void beforeClass() throws URISyntaxException {
        final URL privateKeyUrl = JwtMinimalClaimSetTest.class.getClassLoader().getResource("pki/key.private.pkcs8.pem");
        if (privateKeyUrl == null) {
            throw new IllegalStateException("Private key wasn't found in resources!");
        }

        keyTool = RsaKeyTool.newKeyTool(privateKeyUrl.toURI());
    }

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, JwtMinimalClaimSetTest.class.getSimpleName() + ".war")
                .addClass(SecuredJaxRsEndpoint.class)
                .addClass(JaxRsTestApplication.class)
                .addAsManifestResource(JwtMinimalClaimSetTest.class.getClassLoader().getResource("mp-config-basic.properties"),
                        "microprofile-config.properties")
                .addAsManifestResource(JwtMinimalClaimSetTest.class.getClassLoader().getResource("pki/key.public.pem"),
                        "key.public.pem");
    }

    /**
     * @tpTestDetails Supply a JWT with missing issuer (iss) claim to the server.
     * @tpPassCrit Issuer claim is recommended by specification to be required and such JWT must be rejected and user
     *             must receive 401/Unauthorized.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void missingIssRejectedTest(@ArquillianResource URL url) {
        final Instant now = Instant.now();
        final Instant later = now.plus(1, ChronoUnit.HOURS);

        //issuer (iss) claim intentionally missing
        final JwtClaims jwtClaims = new JwtClaims.Builder()
                .jwtId(UUID.randomUUID().toString())
                .audience(JwtDefaultClaimValues.AUDIENCE)
                .subject(SUBJECT)
                .userPrincipalName(SUBJECT)
                .issuedAtTime(now.getEpochSecond())
                .expirationTime(later.getEpochSecond())
                .groups(new HashSet<>(Arrays.asList("group1", "group2")))
                .build();

        final JsonWebToken jwt = JwtHelper.generateProperSignedJwtWithClaims(keyTool, jwtClaims);

        given().header("Authorization", "Bearer " + jwt.getRawValue())
                .when().get(url.toExternalForm() + Endpoints.SECURED_ENDPOINT)
                .then()
                .statusCode(401);
    }

    /**
     * @tpTestDetails Supply a JWT with missing subject (sub) claim to the server.
     * @tpPassCrit Subject claim is recommended by specification to be required and such JWT must be rejected and user
     *             must receive 401/Unauthorized.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void missingSubRejectedTest(@ArquillianResource URL url) {
        final Instant now = Instant.now();
        final Instant later = now.plus(1, ChronoUnit.HOURS);

        //subject (sub) claim intentionally missing
        final JwtClaims jwtClaims = new JwtClaims.Builder()
                .jwtId(UUID.randomUUID().toString())
                .audience(JwtDefaultClaimValues.AUDIENCE)
                .issuer(ISSUER)
                .userPrincipalName(SUBJECT)
                .issuedAtTime(now.getEpochSecond())
                .expirationTime(later.getEpochSecond())
                .groups(new HashSet<>(Arrays.asList("group1", "group2")))
                .build();

        final JsonWebToken jwt = JwtHelper.generateProperSignedJwtWithClaims(keyTool, jwtClaims);

        given().header("Authorization", "Bearer " + jwt.getRawValue())
                .when().get(url.toExternalForm() + Endpoints.SECURED_ENDPOINT)
                .then()
                .statusCode(401);
    }

    /**
     * @tpTestDetails Supply a JWT with missing expiration time (exp) claim to the server.
     * @tpPassCrit Expiration time claim is recommended by specification to be required and such JWT must be rejected
     *             and user must receive 401/Unauthorized.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void missingExpRejectedTest(@ArquillianResource URL url) {
        final Instant now = Instant.now();

        //expiration time (exp) claim intentionally missing
        final JwtClaims jwtClaims = new JwtClaims.Builder()
                .jwtId(UUID.randomUUID().toString())
                .audience(JwtDefaultClaimValues.AUDIENCE)
                .issuer(ISSUER)
                .subject(SUBJECT)
                .userPrincipalName(SUBJECT)
                .issuedAtTime(now.getEpochSecond())
                .groups(new HashSet<>(Arrays.asList("group1", "group2")))
                .build();

        final JsonWebToken jwt = JwtHelper.generateProperSignedJwtWithClaims(keyTool, jwtClaims);

        given().header("Authorization", "Bearer " + jwt.getRawValue())
                .when().get(url.toExternalForm() + Endpoints.SECURED_ENDPOINT)
                .then()
                .statusCode(401);
    }

    /**
     * @tpTestDetails Supply a JWT with missing issued at (iat) claim to the server.
     * @tpPassCrit Issued at claim is recommended by specification to be required and such JWT must be rejected and user
     *             must receive 401/Unauthorized.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void missingIatRejectedTest(@ArquillianResource URL url) {
        final Instant now = Instant.now();
        final Instant later = now.plus(1, ChronoUnit.HOURS);

        //issued at (iat) claim intentionally missing
        final JwtClaims jwtClaims = new JwtClaims.Builder()
                .jwtId(UUID.randomUUID().toString())
                .audience(JwtDefaultClaimValues.AUDIENCE)
                .issuer(ISSUER)
                .subject(SUBJECT)
                .userPrincipalName(SUBJECT)
                .expirationTime(later.getEpochSecond())
                .groups(new HashSet<>(Arrays.asList("group1", "group2")))
                .build();

        final JsonWebToken jwt = JwtHelper.generateProperSignedJwtWithClaims(keyTool, jwtClaims);

        given().header("Authorization", "Bearer " + jwt.getRawValue())
                .when().get(url.toExternalForm() + Endpoints.SECURED_ENDPOINT)
                .then()
                .statusCode(401);
    }

    /**
     * @tpTestDetails Supply a JWT with missing JWT ID (jti) claim to the server.
     * @tpPassCrit JWT ID claim is recommended by specification to be required and such JWT must be rejected and user
     *             must receive 401/Unauthorized.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void missingJtiRejectedTest(@ArquillianResource URL url) {
        final Instant now = Instant.now();
        final Instant later = now.plus(1, ChronoUnit.HOURS);

        //JWT ID (jti) claim intentionally missing
        final JwtClaims jwtClaims = new JwtClaims.Builder()
                .audience(JwtDefaultClaimValues.AUDIENCE)
                .issuer(ISSUER)
                .subject(SUBJECT)
                .userPrincipalName(SUBJECT)
                .issuedAtTime(now.getEpochSecond())
                .expirationTime(later.getEpochSecond())
                .groups(new HashSet<>(Arrays.asList("group1", "group2")))
                .build();

        final JsonWebToken jwt = JwtHelper.generateProperSignedJwtWithClaims(keyTool, jwtClaims);

        given().header("Authorization", "Bearer " + jwt.getRawValue())
                .when().get(url.toExternalForm() + Endpoints.SECURED_ENDPOINT)
                .then()
                .statusCode(401);
    }

    /**
     * @tpTestDetails Supply a JWT with missing user principal name (upn) claim to the server.
     * @tpPassCrit User principal name claim is recommended by specification to be required and such JWT must be
     *             rejected and user must receive 401/Unauthorized.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void missingUpnRejectedTest(@ArquillianResource URL url) {
        final Instant now = Instant.now();
        final Instant later = now.plus(1, ChronoUnit.HOURS);

        //user principal name (upn) claim intentionally missing
        final JwtClaims jwtClaims = new JwtClaims.Builder()
                .jwtId(UUID.randomUUID().toString())
                .audience(JwtDefaultClaimValues.AUDIENCE)
                .issuer(ISSUER)
                .subject(SUBJECT)
                .issuedAtTime(now.getEpochSecond())
                .expirationTime(later.getEpochSecond())
                .groups(new HashSet<>(Arrays.asList("group1", "group2")))
                .build();

        final JsonWebToken jwt = JwtHelper.generateProperSignedJwtWithClaims(keyTool, jwtClaims);

        given().header("Authorization", "Bearer " + jwt.getRawValue())
                .when().get(url.toExternalForm() + Endpoints.SECURED_ENDPOINT)
                .then()
                .statusCode(401);
    }

    /**
     * @tpTestDetails Supply a JWT with missing groups claim to the server.
     * @tpPassCrit Groups claim is recommended by specification to be required and such JWT must be rejected and user
     *             must receive 401/Unauthorized.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void missingGroupsRejectedTest(@ArquillianResource URL url) {
        final Instant now = Instant.now();
        final Instant later = now.plus(1, ChronoUnit.HOURS);

        //groups claim intentionally missing
        final JwtClaims jwtClaims = new JwtClaims.Builder()
                .jwtId(UUID.randomUUID().toString())
                .audience(JwtDefaultClaimValues.AUDIENCE)
                .issuer(ISSUER)
                .subject(SUBJECT)
                .userPrincipalName(SUBJECT)
                .issuedAtTime(now.getEpochSecond())
                .expirationTime(later.getEpochSecond())
                .build();

        final JsonWebToken jwt = JwtHelper.generateProperSignedJwtWithClaims(keyTool, jwtClaims);

        given().header("Authorization", "Bearer " + jwt.getRawValue())
                .when().get(url.toExternalForm() + Endpoints.SECURED_ENDPOINT)
                .then()
                .statusCode(401);
    }

}
