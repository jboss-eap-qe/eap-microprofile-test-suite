package org.jboss.eap.qe.microprofile.jwt.auth.tool;

import java.net.URI;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;
import java.util.Map;

import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.jwt.consumer.JwtContext;

/**
 * Utility wrapper for the jose4j library for creating and reading JWS (JWT Signed Tokens)
 *
 * @author tborgato
 */
public class JwsHelper {

    private RsaKeyHelper rsaHelper;
    private EcKeyHelper ecHelper;

    private RsaKeyHelper getRsaHelper() {
        if (rsaHelper == null)
            rsaHelper = new RsaKeyHelper();
        return rsaHelper;
    }

    private EcKeyHelper getEcHelper() {
        if (ecHelper == null)
            ecHelper = new EcKeyHelper();
        return ecHelper;
    }

    /**
     * Generates a signed JWT;
     *
     * @param algorithm used to sign the JWT
     * @param privateKey used to sign the JWT
     * @param kid key ID Header Parameter
     * @param issuer JWT claim
     * @param audience JWT claim
     * @param subject JWT claim
     * @param groups JWT claim
     * @param customClaims custom JWT claims
     * @return {@link JsonWebSignature} representing the JWS
     * @throws Exception in case anything goes wrong while generating the JWS
     */
    public JsonWebSignature generateProperSignedJwt(
            final String algorithm,
            final URI privateKey,
            final String kid,
            final String issuer,
            final String audience,
            final String subject,
            final List<String> groups,
            final Map<String, Object> customClaims) throws Exception {
        assert algorithm != null : "algorithm is null";
        assert privateKey != null : "privateKey is null";
        assert issuer != null : "issuer is null";
        assert subject != null : "subject is null";
        assert groups != null : "groups is null";

        // RSA/EC private key, which will be used for signing of the JWT
        PrivateKey key;

        switch (algorithm) {
            case AlgorithmIdentifiers.RSA_USING_SHA256:
                key = getRsaHelper().readPrivateKey(privateKey.normalize());
                break;
            case AlgorithmIdentifiers.ECDSA_USING_P256_CURVE_AND_SHA256:
                key = getEcHelper().readPrivateKey(privateKey.normalize());
                break;
            default:
                throw new IllegalArgumentException("Unknown algorithm " + algorithm);
        }

        // Create the Claims, which will be the content of the JWT
        org.jose4j.jwt.JwtClaims claims = new org.jose4j.jwt.JwtClaims();
        claims.setGeneratedJwtId(); // a unique identifier for the token
        claims.setAudience(audience); // to whom the token is intended to be sent
        claims.setSubject(subject); // the subject/principal is whom the token is about
        claims.setClaim("upn", subject);
        claims.setIssuer(issuer); // who creates the token and signs it
        claims.setIssuedAtToNow(); // when the token was issued/created (now)
        claims.setExpirationTimeMinutesInTheFuture(60); // time when the token will expire (10 minutes from now)
        claims.setStringListClaim("groups", groups); // multi-valued claims work too and will end up as a JSON array
        claims.setClaim("preferred_username", subject); // additional claims/attributes about the subject can be added
        if (customClaims != null && customClaims.size() > 0) {
            for (Map.Entry<String, Object> entry : customClaims.entrySet()) {
                claims.setClaim(entry.getKey(), entry.getValue());
            }
        }

        // A JWT is a JWS and/or a JWE with JSON claims as the payload.
        // In this example it is a JWS so we create a JsonWebSignature object.
        JsonWebSignature jws = new JsonWebSignature();

        // The payload of the JWS is JSON content of the JWT Claims
        jws.setPayload(claims.toJson());

        // The JWT is signed using the private key
        jws.setKey(key);

        // Set the Key ID (kid) header because it's just the polite thing to do.
        // We only have one key in this example but a using a Key ID helps
        // facilitate a smooth key rollover process
        jws.setKeyIdHeaderValue(kid);

        // Set the signature algorithm on the JWT/JWS that will integrity protect the claims
        jws.setAlgorithmHeaderValue(algorithm);

        return jws;
    }

    /**
     * Reads and validates a signed JWT
     *
     * @param jws
     * @param algorithm
     * @param publicKey
     * @return
     * @throws Exception
     */
    public JwtContext extractJwt(
            final String jws,
            final String algorithm,
            final URI publicKey) throws Exception {
        return extractJwt(jws, algorithm, publicKey, null, null);
    }

    /**
     * Reads and validates a signed JWT
     *
     * @param jws
     * @param algorithm
     * @param publicKey
     * @param issuer
     * @param audience
     * @return
     * @throws Exception
     */
    public JwtContext extractJwt(
            final String jws,
            final String algorithm,
            final URI publicKey,
            final String issuer,
            final String audience) throws Exception {
        assert jws != null : "jws is null";
        assert algorithm != null : "algorithm is null";
        assert publicKey != null : "privateKey is null";

        // RSA/EC public key, which will be used for decrypting the JWS
        PublicKey key;

        switch (algorithm) {
            case AlgorithmIdentifiers.RSA_USING_SHA256:
                key = getRsaHelper().readPublicKey(publicKey.normalize());
                break;
            case AlgorithmIdentifiers.ECDSA_USING_P256_CURVE_AND_SHA256:
                key = getEcHelper().readPublicKey(publicKey.normalize());
                break;
            default:
                throw new IllegalArgumentException("Unknown algorithm " + algorithm);
        }

        // Use JwtConsumerBuilder to construct an appropriate JwtConsumer, which will
        // be used to validate and process the JWT.
        // The specific validation requirements for a JWT are context dependent, however,
        // it typically advisable to require a (reasonable) expiration time, a trusted issuer, and
        // and audience that identifies your system as the intended recipient.
        // If the JWT is encrypted too, you need only provide a decryption key or
        // decryption key resolver to the builder.
        JwtConsumerBuilder jwtConsumerBuilder = new JwtConsumerBuilder()
                .setRequireExpirationTime() // the JWT must have an expiration time
                .setAllowedClockSkewInSeconds(30) // allow some leeway in validating time based claims to account for clock skew
                .setRequireSubject() // the JWT must have a subject claim
                .setVerificationKey(key) // verify the signature with the public key
                .setJwsAlgorithmConstraints(AlgorithmConstraints.ConstraintType.PERMIT, algorithm); // only allow the expected signature algorithm(s) in the given context
        if (issuer != null)
            jwtConsumerBuilder.setExpectedIssuer(issuer); // whom the JWT needs to have been issued by
        if (audience != null)
            jwtConsumerBuilder.setExpectedAudience(audience); // to whom the JWT is intended for

        JwtConsumer jwtConsumer = jwtConsumerBuilder.build();

        return jwtConsumer.process(jws);
    }
}
