package org.jboss.eap.qe.microprofile.jwt.auth.tool;

import java.util.Arrays;
import java.util.Collections;

import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.consumer.JwtContext;
import org.junit.Assert;
import org.junit.Test;

public class JwsHelperTest {

    /**
     * @tpTestDetails Creates a JWT signed with RS algorihtm and then do the reverse to obtain back the original JWT
     * @tpPassCrit Expect a non null and properly formatted JWS and that the extracted JWE contains some original claims
     * @tpSince EAP 7.4.0.CD23
     */
    @Test
    public void testRsaJwsCreation() throws Exception {
        JwsHelper jwsHelper = new JwsHelper();
        JsonWebSignature jsonWebSignature = jwsHelper.generateProperSignedJwt(
                AlgorithmIdentifiers.RSA_USING_SHA256,
                JwsHelperTest.class.getClassLoader().getResource("pki/RS256/key.private.pkcs8.pem").toURI(),
                "k1",
                JwtDefaultClaimValues.ISSUER,
                JwtDefaultClaimValues.AUDIENCE,
                JwtDefaultClaimValues.SUBJECT,
                Arrays.asList("group1", "group2"),
                Collections.singletonMap("custom_claim", "custom_claim_value"));

        // Sign the JWS and produce the compact serialization or the complete JWT/JWS
        // representation, which is a string consisting of three dot ('.') separated
        // base64url-encoded parts in the form Header.Payload.Signature
        // If you wanted to encrypt it, you can simply set this jwt as the payload
        // of a JsonWebEncryption object and set the cty (Content Type) header to "jwt".
        String jws = jsonWebSignature.getCompactSerialization();
        Assert.assertNotNull("JWS is null!", jws);
        Assert.assertEquals("JWS badly formatted!", 3, jws.split("\\.").length);

        JwtContext jwtContext = jwsHelper.extractJwt(
                jws,
                AlgorithmIdentifiers.RSA_USING_SHA256,
                JwsHelperTest.class.getClassLoader().getResource("pki/RS256/key.public.pem").toURI(),
                JwtDefaultClaimValues.ISSUER,
                JwtDefaultClaimValues.AUDIENCE);
        org.jose4j.jwt.JwtClaims jwtClaims = jwtContext.getJwtClaims();
        Assert.assertEquals("Wrong value for claim Issuer", jwtClaims.getIssuer(), JwtDefaultClaimValues.ISSUER);
        Assert.assertEquals("Wrong value for claim Audience", jwtClaims.getAudience().get(0), JwtDefaultClaimValues.AUDIENCE);
        Assert.assertEquals("Wrong value for claim Subject", jwtClaims.getSubject(), JwtDefaultClaimValues.SUBJECT);
    }

    /**
     * @tpTestDetails Creates a JWT signed with EC algorihtm and then do the reverse to obtain back the original JWT
     * @tpPassCrit Expect a non null and properly formatted JWS and that the extracted JWE contains some original claims
     * @tpSince EAP 7.4.0.CD23
     */
    @Test
    public void testEcJwsCreation() throws Exception {
        JwsHelper jwsHelper = new JwsHelper();
        JsonWebSignature jsonWebSignature = jwsHelper.generateProperSignedJwt(
                AlgorithmIdentifiers.ECDSA_USING_P256_CURVE_AND_SHA256,
                JwsHelperTest.class.getClassLoader().getResource("pki/ES256/key.private.pkcs8.pem").toURI(),
                "k1",
                JwtDefaultClaimValues.ISSUER,
                JwtDefaultClaimValues.AUDIENCE,
                JwtDefaultClaimValues.SUBJECT,
                Arrays.asList("group1", "group2"),
                Collections.singletonMap("custom_claim", "custom_claim_value"));

        // Sign the JWS and produce the compact serialization or the complete JWT/JWS
        // representation, which is a string consisting of three dot ('.') separated
        // base64url-encoded parts in the form Header.Payload.Signature
        // If you wanted to encrypt it, you can simply set this jwt as the payload
        // of a JsonWebEncryption object and set the cty (Content Type) header to "jwt".
        String jws = jsonWebSignature.getCompactSerialization();
        Assert.assertNotNull("JWS is null!", jws);
        Assert.assertEquals("JWS badly formatted!", 3, jws.split("\\.").length);

        JwtContext jwtContext = jwsHelper.extractJwt(
                jws,
                AlgorithmIdentifiers.ECDSA_USING_P256_CURVE_AND_SHA256,
                JwsHelperTest.class.getClassLoader().getResource("pki/ES256/key.public.pem").toURI(),
                JwtDefaultClaimValues.ISSUER,
                JwtDefaultClaimValues.AUDIENCE);
        org.jose4j.jwt.JwtClaims jwtClaims = jwtContext.getJwtClaims();
        Assert.assertEquals("Wrong value for claim Issuer", jwtClaims.getIssuer(), JwtDefaultClaimValues.ISSUER);
        Assert.assertEquals("Wrong value for claim Audience", jwtClaims.getAudience().get(0), JwtDefaultClaimValues.AUDIENCE);
        Assert.assertEquals("Wrong value for claim Subject", jwtClaims.getSubject(), JwtDefaultClaimValues.SUBJECT);
    }
}
