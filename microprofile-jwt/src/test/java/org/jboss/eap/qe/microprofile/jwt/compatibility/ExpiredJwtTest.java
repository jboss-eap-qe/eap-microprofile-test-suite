package org.jboss.eap.qe.microprofile.jwt.compatibility;

import static io.restassured.RestAssured.given;

import java.net.URISyntaxException;
import java.net.URL;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.UUID;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.eap.qe.microprofile.jwt.auth.tool.JsonWebToken;
import org.jboss.eap.qe.microprofile.jwt.auth.tool.JwtClaims;
import org.jboss.eap.qe.microprofile.jwt.auth.tool.JwtHelper;
import org.jboss.eap.qe.microprofile.jwt.auth.tool.RsaKeyTool;
import org.jboss.eap.qe.microprofile.jwt.testapp.Endpoints;
import org.jboss.eap.qe.microprofile.jwt.testapp.jaxrs.JaxRsTestApplication;
import org.jboss.eap.qe.microprofile.jwt.testapp.jaxrs.SecuredJaxRsEndpoint;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunAsClient
@RunWith(Arquillian.class)
public class ExpiredJwtTest {

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, ExpiredJwtTest.class.getSimpleName() + ".war")
                .addClass(SecuredJaxRsEndpoint.class)
                .addClass(JaxRsTestApplication.class)
                .addAsManifestResource(ExpiredJwtTest.class.getClassLoader().getResource("mp-config-basic.properties"),
                        "microprofile-config.properties")
                .addAsManifestResource(ExpiredJwtTest.class.getClassLoader().getResource("pki/key.public.pem"),
                        "key.public.pem");
    }

    /**
     * @tpTestDetails Supply an expired ({@code exp} set in past) JWT to server
     * @tpPassCrit Expired JWT is rejected and user receives 401/Unauthorized.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void supplyExpiredJwtToServerTest(@ArquillianResource URL url) throws URISyntaxException {
        final URL privateKeyUrl = ExpiredJwtTest.class.getClassLoader().getResource("pki/key.private.pkcs8.pem");
        if (privateKeyUrl == null) {
            throw new IllegalStateException("Private key wasn't found in resources!");
        }

        final RsaKeyTool keyTool = RsaKeyTool.newKeyTool(privateKeyUrl.toURI());

        final String subject = "FAKE_USER";
        final Instant now = Instant.now();
        final Instant sooner = now.minus(1, ChronoUnit.HOURS); //note the minus here setting time in past

        final JsonWebToken expiredJwt = JwtHelper.generateProperSignedJwtWithClaims(keyTool, new JwtClaims.Builder()
                .jwtId(UUID.randomUUID().toString())
                .audience("microprofile-jwt-testsuite")
                .subject(subject)
                .userPrincipalName(subject)
                .issuer("issuer")
                .issuedAtTime(now.getEpochSecond())
                .expirationTime(sooner.getEpochSecond())
                .groups(Collections.emptySet())
                .customClaim("preferred_username", subject)
                .build());

        given().header("Authorization", "Bearer " + expiredJwt.getRawValue())
                .when().get(url.toExternalForm() + Endpoints.SECURED_ENDPOINT)
                .then()
                .statusCode(401);
    }

}
