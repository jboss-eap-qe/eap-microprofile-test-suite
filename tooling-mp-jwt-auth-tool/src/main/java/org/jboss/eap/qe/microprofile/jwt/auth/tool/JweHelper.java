package org.jboss.eap.qe.microprofile.jwt.auth.tool;

import java.net.URI;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;
import java.util.Map;

import org.jose4j.base64url.Base64Url;
import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jwe.ContentEncryptionAlgorithmIdentifiers;
import org.jose4j.jwe.JsonWebEncryption;
import org.jose4j.jwe.KeyManagementAlgorithmIdentifiers;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.jwt.consumer.JwtContext;

/**
 * Utility wrapper for the jose4j library for creating and reading JWE containing a nested (signed and encrypted) JWT
 *
 * Inspired by
 * https://bitbucket.org/b_c/jose4j/wiki/JWT%20Examples#markdown-header-producing-and-consuming-a-nested-signed-and-encrypted-jwt
 *
 * @author tborgato
 */
public class JweHelper {

    private JwsHelper jwsHelper;
    private RsaKeyHelper rsaHelper;
    private EcKeyHelper ecHelper;

    private JwsHelper getJwsHelper() {
        if (jwsHelper == null)
            jwsHelper = new JwsHelper();
        return jwsHelper;
    }

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
     * Generates a JWE with a nested signed JWT
     *
     * @param jwsAlgorithm Algorithm used to sign the nested JWT
     * @param jwsPrivateKey location of the private key used to sign the JWT
     * @param kid Key identifier
     * @param issuer "issuer" claim
     * @param audience "audience" claim
     * @param subject "subject" claim
     * @param groups "groups" claim
     * @param customClaims custom claims
     * @param jweAlgAlgorithm Algorithm used to generate the CEK
     * @param jweEncAlgorithm Algorithm used to generate the key pair used to encrypt/de-encrypt the CEK
     * @param jwePublicKey location of the public key used to encrypt the CEK
     * @param jwePublicKeyKid Key ID of public key used to encrypt the CEK
     * @return the JWE representation
     * @throws Exception
     */
    // TODO: replace with builder pattern
    public JsonWebEncryption generateJwe(
            // JWS
            final String jwsAlgorithm,
            final URI jwsPrivateKey,
            final String kid,
            final String issuer,
            final String audience,
            final String subject,
            final List<String> groups,
            final Map<String, Object> customClaims,
            // JWE
            final String jweAlgAlgorithm,
            final String jweEncAlgorithm,
            final URI jwePublicKey,
            final String jwePublicKeyKid) throws Exception {
        JsonWebSignature jws = getJwsHelper().generateProperSignedJwt(
                jwsAlgorithm,
                jwsPrivateKey,
                kid,
                issuer,
                audience,
                subject,
                groups,
                customClaims);
        return generateJwe(jws.getCompactSerialization(), jweAlgAlgorithm, jweEncAlgorithm, jwePublicKey, jwePublicKeyKid);
    }

    /**
     * Generates a JWE with a nested signed JWT
     *
     * @param innerJws the nested signed JWT
     * @param jweAlgorithm Algorithm used to generate the CEK
     * @param jweEncAlgorithm Algorithm used to generate the key pair used to encrypt/de-encrypt the CEK
     * @param jwePublicKeyUri location of the private key used to de-encrypt the CEK
     * @param jwePublicKeyKid Key ID of public key used to encrypt the CEK
     * @return the JWE representation
     * @throws Exception
     */
    // TODO: replace with builder pattern
    public JsonWebEncryption generateJwe(
            final String innerJws,
            final String jweAlgorithm,
            final String jweEncAlgorithm,
            final URI jwePublicKeyUri,
            final String jwePublicKeyKid) throws Exception {
        PublicKey jwePublicKey = getRsaHelper().readPublicKey(jwePublicKeyUri.normalize());

        // Support for decrypting JWT tokens which have been encrypted using RSA-OAEP and A256GCM algorithms and
        // contain the claims or inner-signed JWT tokens

        // The outer JWT is a JWE
        JsonWebEncryption jwe = new JsonWebEncryption();

        // The output of the ECDH-ES key agreement will encrypt a randomly generated content encryption key
        // (== KeyManagementAlgorithmIdentifiers.RSA_OAEP)
        if (jweAlgorithm != null) {
            jwe.setAlgorithmHeaderValue(jweAlgorithm);
        }

        // The content encryption key is used to encrypt the payload
        // with a composite AES-CBC / HMAC SHA2 encryption algorithm
        // (== ContentEncryptionAlgorithmIdentifiers.AES_256_GCM)
        if (jweEncAlgorithm != null) {
            jwe.setEncryptionMethodHeaderParameter(jweEncAlgorithm);
        }

        // We encrypt to the receiver using their public key
        jwe.setKey(jwePublicKey);
        jwe.setKeyIdHeaderValue(jwePublicKeyKid);

        // A nested Signed JWT requires that the cty (Content Type) header be set to "JWT" in the outer JWT
        jwe.setContentTypeHeaderValue("JWT");

        // The inner Signed JWT is the payload of the outer JWT
        jwe.setPayload(innerJws);
        return jwe;
    }

    /**
     * Generates a valid JWE and then removes the "alg" header: used in tests to verify JWE without "alg" header is not valid
     * 
     * @param jwsAlgorithm Algorithm used to sign the nested JWT token
     * @param jwsPrivateKey location of the private key used to sign the JWT token
     * @param kid Key ID of the private key used to sign the JWT token
     * @param issuer "issuer" claim
     * @param audience "audience" claim
     * @param subject "subject" claim
     * @param groups "groups" claims
     * @param customClaims custom claims
     * @param jweAlgAlgorithm Algorithm used to generate the CEK
     * @param jweEncAlgorithm Algorithm used to generate the key pair used to encrypt/de-encrypt the CEK
     * @param jwePublicKey location of the public key used to encrypt the CEK
     * @param jwePublicKeyKid Key ID of public key used to encrypt the CEK
     * @return the base64 encoded JEW token
     * @throws Exception
     */
    // TODO: replace with builder pattern
    public String generateJweWithoutAlgHeader(
            // JWS
            final String jwsAlgorithm,
            final URI jwsPrivateKey,
            final String kid,
            final String issuer,
            final String audience,
            final String subject,
            final List<String> groups,
            final Map<String, Object> customClaims,
            // JWE
            final String jweAlgAlgorithm,
            final String jweEncAlgorithm,
            final URI jwePublicKey,
            final String jwePublicKeyKid) throws Exception {
        JsonWebEncryption jsonWebEncryption = generateJwe(
                jwsAlgorithm,
                jwsPrivateKey,
                kid,
                issuer,
                audience,
                subject,
                groups,
                customClaims,
                jweAlgAlgorithm,
                jweEncAlgorithm,
                jwePublicKey,
                jwePublicKeyKid);
        // remove "alg" from {"alg":"RSA-OAEP","enc":"A256GCM","kid":"k2","cty":"JWT"}
        String newHeaders = jsonWebEncryption.getHeaders().getFullHeaderAsJsonString().replaceFirst("\"alg\":\"[^\"]+\",?", "");
        // swap the jwe header with one missing "alg"
        String jwe = jsonWebEncryption.getCompactSerialization();
        String[] jweTokens = jwe.split("\\.");
        Base64Url base64url = new Base64Url();
        jweTokens[0] = base64url.base64UrlEncodeUtf8ByteRepresentation(newHeaders);
        return String.join(".", jweTokens);
    }

    /**
     * Generates a valid JWE and then removes the "enc" header: used in tests to verify JWE without "alg" header is not valid
     *
     * @param jwsAlgorithm Algorithm used to sign the nested JWT
     * @param jwsPrivateKey location of the private key used to sign the JWT token
     * @param kid Key ID of the private key used to sign the JWT token
     * @param issuer "issuer" claim
     * @param audience "audience" claim
     * @param subject "subject" claim
     * @param groups "groups" claims
     * @param customClaims custom claims
     * @param jweAlgAlgorithm Algorithm used to generate the CEK
     * @param jweEncAlgorithm Algorithm used to generate the key pair used to encrypt/de-encrypt the CEK
     * @param jwePublicKey location of the public key used to encrypt the CEK
     * @param jwePublicKeyKid Key ID of public key used to encrypt the CEK
     * @return the base64 encoded JEW token
     * @throws Exception
     */
    // TODO: replace with builder pattern
    public String generateJweWithoutEncHeader(
            // JWS
            final String jwsAlgorithm,
            final URI jwsPrivateKey,
            final String kid,
            final String issuer,
            final String audience,
            final String subject,
            final List<String> groups,
            final Map<String, Object> customClaims,
            // JWE
            final String jweAlgAlgorithm,
            final String jweEncAlgorithm,
            final URI jwePublicKey,
            final String jwePublicKeyKid) throws Exception {
        JsonWebEncryption jsonWebEncryption = generateJwe(
                jwsAlgorithm,
                jwsPrivateKey,
                kid,
                issuer,
                audience,
                subject,
                groups,
                customClaims,
                jweAlgAlgorithm,
                jweEncAlgorithm,
                jwePublicKey,
                jwePublicKeyKid);
        // remove "alg" from {"alg":"RSA-OAEP","enc":"A256GCM","kid":"k2","cty":"JWT"}
        String newHeaders = jsonWebEncryption.getHeaders().getFullHeaderAsJsonString().replaceFirst("\"enc\":\"[^\"]+\",?", "");
        // swap the jwe header with one missing "alg"
        String jwe = jsonWebEncryption.getCompactSerialization();
        String[] jweTokens = jwe.split("\\.");
        Base64Url base64url = new Base64Url();
        jweTokens[0] = base64url.base64UrlEncodeUtf8ByteRepresentation(newHeaders);
        return String.join(".", jweTokens);
    }

    /**
     * Given a base 64 encoded JWS token, extracts the JWT inside it
     *
     * @param jwe JWE token
     * @param jweAlgorithm Algorithm used to generate the CEK
     * @param jweEncAlgorithm Algorithm used to generate the key pair used to encrypt/de-encrypt the CEK
     * @param jwePrivateKeyUri location of the private key used to de-encrypt the CEK
     * @param jwsAlgorithm Algorithm used to sign the nested JWT
     * @param jwsPublicKeyUri location of the public key used to verify the nested JWT signature
     * @param issuer "issuer" claim
     * @param audience "audience" claim
     * @return the nested JWT
     * @throws Exception
     */
    // TODO: replace with builder pattern
    public JwtContext extractNestedJwt(
            // JWE
            final String jwe,
            final String jweAlgorithm,
            final String jweEncAlgorithm,
            final URI jwePrivateKeyUri,
            // JWS
            final String jwsAlgorithm,
            final URI jwsPublicKeyUri,
            final String issuer,
            final String audience) throws Exception {
        PrivateKey jwePrivateKey = getRsaHelper().readPrivateKey(jwePrivateKeyUri.normalize());
        PublicKey jwsPublicKey;
        switch (jwsAlgorithm) {
            case AlgorithmIdentifiers.RSA_USING_SHA256:
                jwsPublicKey = getRsaHelper().readPublicKey(jwsPublicKeyUri.normalize());
                break;
            case AlgorithmIdentifiers.ECDSA_USING_P256_CURVE_AND_SHA256:
                jwsPublicKey = getEcHelper().readPublicKey(jwsPublicKeyUri.normalize());
                break;
            default:
                throw new IllegalArgumentException("Unknown algorithm " + jwsAlgorithm);
        }

        // Use JwtConsumerBuilder to construct an appropriate JwtConsumer, which will
        // be used to validate and process the JWT.
        // The specific validation requirements for a JWT are context dependent, however,
        // it typically advisable to require a (reasonable) expiration time, a trusted issuer, and
        // and audience that identifies your system as the intended recipient.
        // It is also typically good to allow only the expected algorithm(s) in the given context
        AlgorithmConstraints jwsAlgConstraints = new AlgorithmConstraints(AlgorithmConstraints.ConstraintType.PERMIT,
                jwsAlgorithm);

        // Algorithm used to generate the symmetric key (Content Encryption Key - CEK) which is used to encrypt the payload
        AlgorithmConstraints jweAlgConstraints = new AlgorithmConstraints(AlgorithmConstraints.ConstraintType.PERMIT,
                jweAlgorithm != null ? jweAlgorithm : KeyManagementAlgorithmIdentifiers.RSA_OAEP);

        // Algorithm used generate the key pair used to encrypt/de-encrypt the CEK
        AlgorithmConstraints jweEncConstraints = new AlgorithmConstraints(AlgorithmConstraints.ConstraintType.PERMIT,
                jweEncAlgorithm != null ? jweEncAlgorithm : ContentEncryptionAlgorithmIdentifiers.AES_256_GCM);

        JwtConsumerBuilder jwtConsumerBuilder = new JwtConsumerBuilder()
                .setRequireExpirationTime() // the JWT must have an expiration time
                .setMaxFutureValidityInMinutes(300) // but the  expiration time can't be too crazy
                .setRequireSubject() // the JWT must have a subject claim
                .setVerificationKey(jwsPublicKey) // verify the signature with the sender's public key
                .setJwsAlgorithmConstraints(jwsAlgConstraints) // limits the acceptable signature algorithm(s)
                .setDecryptionKey(jwePrivateKey) // decrypt with the receiver's private key
                .setJweAlgorithmConstraints(jweAlgConstraints) // limits acceptable encryption key establishment algorithm(s)
                .setJweContentEncryptionAlgorithmConstraints(jweEncConstraints); // limits acceptable content encryption algorithm(s)
        if (issuer != null)
            jwtConsumerBuilder.setExpectedIssuer(issuer); // whom the JWT needs to have been issued by
        if (audience != null)
            jwtConsumerBuilder.setExpectedAudience(audience); // to whom the JWT is intended for

        JwtConsumer jwtConsumer = jwtConsumerBuilder.build();

        return jwtConsumer.process(jwe);
    }
}
