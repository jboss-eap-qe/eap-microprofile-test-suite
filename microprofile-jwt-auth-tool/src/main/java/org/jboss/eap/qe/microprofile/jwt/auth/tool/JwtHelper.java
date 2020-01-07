package org.jboss.eap.qe.microprofile.jwt.auth.tool;

import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;

/**
 * Sugar syntax for generating JWTs with re-used values
 */
public final class JwtHelper {
    private final RsaKeyTool keyTool;
    private final String issuer;

    public JwtHelper(final RsaKeyTool keyTool, final String issuer) {
        this.keyTool = keyTool;
        this.issuer = issuer;
    }

    /**
     * Generates a spec compliant base64-encoded signed JWT that expires after one hour and has the claims "sub" and
     * "preferred_username" set to "FAKE_USER".
     *
     * @return a base64-encoded signed JWT token.
     */
    public JsonWebToken generateProperSignedJwt() {
        return generateProperSignedJwt("FAKE_USER");
    }

    /**
     * Generates a spec compliant base64-encoded signed JWT that expires after one hour and has the claims "sub" and
     * "preferred_username" set to the provided subject string.
     *
     * @param subject string to use for "sub" and "preferred_username".
     * @return a base64-encoded signed JWT token.
     */
    public JsonWebToken generateProperSignedJwt(final String subject) {
        final Instant now = Instant.now();
        final Instant later = now.plus(1, ChronoUnit.HOURS);

        final JoseHeader joseHeader = new JoseHeader(keyTool.getJwkKeyId());

        final JwtClaims jwtClaims = new JwtClaims.Builder()
                .jwtId(UUID.randomUUID().toString())
                .audience("microprofile-jwt-testsuite")
                .subject(subject)
                .userPrincipalName(subject)
                .issuer(this.issuer)
                .issuedAtTime(now.getEpochSecond())
                .expirationTime(later.getEpochSecond())
                .groups(new HashSet<>(Arrays.asList("group1", "group2")))
                .customClaim("preferred_username", subject)
                .build();

        try {
            return new JsonWebToken.Builder()
                    .joseHeader(joseHeader)
                    .jwtClaims(jwtClaims)
                    .signature(Signature.getInstance("SHA256withRSA"))
                    .privateKey(this.keyTool.getPrivateKey())
                    .build();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA256 algorithm is not supported by JVM!", e);
        }
    }

    /**
     * Generate JWT which claims are missing closing "}"
     * 
     * @param subject string to use for "sub" and "preferred_username".
     * @return a base64-encoded signed corrupted JWT token.
     */
    public JsonWebToken generateJwtJsonCorrupted(final String subject) {
        final Instant now = Instant.now();
        final Instant later = now.plus(1, ChronoUnit.HOURS);

        final JoseHeader joseHeader = new JoseHeader(keyTool.getJwkKeyId());

        final String jwtClaimsString = "{\n" +
                "\"jti\":\"" + UUID.randomUUID().toString() + "\",\n" +
                "\"sub\":\"" + subject + "\",\n" +
                "\"groups\":[\n" +
                "\"group2\",\n" +
                "\"group1\"\n" +
                "],\n" +
                "\"aud\":\"microprofile-jwt-testsuite\",\n" +
                "\"iss\":\"issuer\",\n" +
                "\"iat\":" + now.getEpochSecond() + ",\n" +
                "\"exp\":" + later.getEpochSecond() + ",\n" +
                "\"upn\":\"" + subject + "\",\n" +
                "\"preferred_username\":\"" + subject + "\"\n" +
                "\n"; //intentionally missing closing "}"

        try {
            return new JsonWebToken.Builder()
                    .joseHeader(joseHeader)
                    .jwtClaims(jwtClaimsString)
                    .signature(Signature.getInstance("SHA256withRSA"))
                    .privateKey(this.keyTool.getPrivateKey())
                    .build();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA256 algorithm is not supported by JVM!", e);
        }
    }

    public static JsonWebToken generateProperSignedJwtWithClaims(final RsaKeyTool keyTool, final JwtClaims claims) {
        final JoseHeader joseHeader = new JoseHeader(keyTool.getJwkKeyId());

        try {
            return new JsonWebToken.Builder()
                    .joseHeader(joseHeader)
                    .jwtClaims(claims)
                    .signature(Signature.getInstance("SHA256withRSA"))
                    .privateKey(keyTool.getPrivateKey())
                    .build();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA256 algorithm is not supported by JVM!", e);
        }
    }
}
