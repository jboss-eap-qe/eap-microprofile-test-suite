package org.jboss.eap.qe.microprofile.jwt.tools;

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

    public JsonWebToken generateProperSignedJwt() {
        return generateProperSignedJwt("FAKE_USER");
    }

    /**
     * Generates a base64-encoded signed JWT that expires after one hour and has the claims "sub" and
     * "preferred_username" set to the provided subject string.
     *
     * @param subject string to use for "sub" and "preferred_username".
     * @return a base64-encoded signed JWT token.
     */
    public JsonWebToken generateProperSignedJwt(final String subject) {
        final Instant now = Instant.now();
        final Instant later = now.plus(1, ChronoUnit.HOURS);

        final JoseHeader joseHeader = JoseHeader.validWithKeyId(keyTool.getJwkKeyId());

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
                    .privateKey(this.keyTool.getPrivateKey())
                    .signature(Signature.getInstance("SHA256withRSA"))
                    .build();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA256 algorithm is not supported by JVM!", e);
        }
    }
}
