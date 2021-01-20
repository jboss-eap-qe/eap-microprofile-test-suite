package org.jboss.eap.qe.microprofile.jwt.auth.tool;

import java.util.Arrays;
import java.util.Collections;

import org.jose4j.jwe.ContentEncryptionAlgorithmIdentifiers;
import org.jose4j.jwe.JsonWebEncryption;
import org.jose4j.jwe.KeyManagementAlgorithmIdentifiers;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jwt.consumer.JwtContext;
import org.junit.Assert;
import org.junit.Test;

public class JweHelperTest {

    /**
     * @tpTestDetails Creates a JWE with a nested JTW signed with RS algorihtm and then extracts the JTW from the JWE
     * @tpPassCrit Expect a non null and properly formatted JWE and that the extracted JWE contains some original claims
     * @tpSince EAP 7.4.0.CD23
     */
    @Test
    public void testJweWithNestedRsaJwsCreation() throws Exception {
        JweHelper jweHelper = new JweHelper();
        JsonWebEncryption jsonWebEncryption = jweHelper.generateJwe(
                AlgorithmIdentifiers.RSA_USING_SHA256,
                JweHelperTest.class.getClassLoader().getResource("pki/RS256/key.private.pkcs8.pem").toURI(),
                "k1",
                JwtDefaultClaimValues.ISSUER,
                JwtDefaultClaimValues.AUDIENCE,
                JwtDefaultClaimValues.SUBJECT,
                Arrays.asList("group1", "group2"),
                Collections.singletonMap("custom_claim", "custom_claim_value"),
                KeyManagementAlgorithmIdentifiers.RSA_OAEP, // RSA-OAEP
                ContentEncryptionAlgorithmIdentifiers.AES_256_GCM,
                JweHelperTest.class.getClassLoader().getResource("pki/RSA-OAEP/key.public.pem").toURI(),
                "k2");
        String jwe = jsonWebEncryption.getCompactSerialization();
        Assert.assertNotNull("JWT is null!", jwe);
        Assert.assertEquals("JWT badly formatted!", 5, jwe.split("\\.").length);

        JwtContext jwtContext = jweHelper.extractNestedJwt(
                jwe,
                KeyManagementAlgorithmIdentifiers.RSA_OAEP, // RSA-OAEP
                ContentEncryptionAlgorithmIdentifiers.AES_256_GCM,
                JweHelperTest.class.getClassLoader().getResource("pki/RSA-OAEP/key.private.pkcs8.pem").toURI(),
                AlgorithmIdentifiers.RSA_USING_SHA256, // RS256
                JweHelperTest.class.getClassLoader().getResource("pki/RS256/key.public.pem").toURI(),
                JwtDefaultClaimValues.ISSUER,
                JwtDefaultClaimValues.AUDIENCE);
        org.jose4j.jwt.JwtClaims jwtClaims = jwtContext.getJwtClaims();
        Assert.assertEquals("Wrong value for claim Issuer", jwtClaims.getIssuer(), JwtDefaultClaimValues.ISSUER);
        Assert.assertEquals("Wrong value for claim Audience", jwtClaims.getAudience().get(0), JwtDefaultClaimValues.AUDIENCE);
    }

    /**
     * @tpTestDetails Creates a JWE with a nested JTW signed with EC algorihtm and then extracts the JTW from the JWE
     * @tpPassCrit Expect a non null and properly formatted JWE and that the extracted JWE contains some original claims
     * @tpSince EAP 7.4.0.CD23
     */
    @Test
    public void testJweWithNestedEcJwsCreation() throws Exception {
        JweHelper jweHelper = new JweHelper();
        JsonWebEncryption jsonWebEncryption = jweHelper.generateJwe(
                AlgorithmIdentifiers.ECDSA_USING_P256_CURVE_AND_SHA256,
                JweHelperTest.class.getClassLoader().getResource("pki/ES256/key.private.pkcs8.pem").toURI(),
                "k1",
                JwtDefaultClaimValues.ISSUER,
                JwtDefaultClaimValues.AUDIENCE,
                JwtDefaultClaimValues.SUBJECT,
                Arrays.asList("group1", "group2"),
                Collections.singletonMap("custom_claim", "custom_claim_value"),
                KeyManagementAlgorithmIdentifiers.RSA_OAEP, // RSA-OAEP
                ContentEncryptionAlgorithmIdentifiers.AES_256_GCM,
                JweHelperTest.class.getClassLoader().getResource("pki/RSA-OAEP/key.public.pem").toURI(),
                "k2");
        String jwe = jsonWebEncryption.getCompactSerialization();
        Assert.assertNotNull("JWT is null!", jwe);
        Assert.assertEquals("JWT badly formatted!", 5, jwe.split("\\.").length);

        JwtContext jwtContext = jweHelper.extractNestedJwt(
                jwe,
                KeyManagementAlgorithmIdentifiers.RSA_OAEP, // RSA-OAEP
                ContentEncryptionAlgorithmIdentifiers.AES_256_GCM,
                JweHelperTest.class.getClassLoader().getResource("pki/RSA-OAEP/key.private.pkcs8.pem").toURI(),
                AlgorithmIdentifiers.ECDSA_USING_P256_CURVE_AND_SHA256,
                JwsHelperTest.class.getClassLoader().getResource("pki/ES256/key.public.pem").toURI(),
                JwtDefaultClaimValues.ISSUER,
                JwtDefaultClaimValues.AUDIENCE);
        org.jose4j.jwt.JwtClaims jwtClaims = jwtContext.getJwtClaims();
        Assert.assertEquals("Wrong value for claim Issuer", jwtClaims.getIssuer(), JwtDefaultClaimValues.ISSUER);
        Assert.assertEquals("Wrong value for claim Audience", jwtClaims.getAudience().get(0), JwtDefaultClaimValues.AUDIENCE);
    }
}
