package org.jboss.eap.qe.microprofile.jwt.auth.tool;

import static java.nio.charset.StandardCharsets.UTF_8;

import jakarta.json.Json;
import jakarta.json.JsonObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;
import java.util.Base64;

/**
 * Utility class for this test suite to return a RSA key in various formats (such as JWK, PEM, etc).
 * <p>
 * An instance of this class will prepare one single RSA key pair that is reused throughout the lifetime of the instance
 * and every method invocation will operate on the same key pair. In addition to the key pair, a key identifier ("kid")
 * for use with JWK will be generated.
 * <p>
 *
 * @author Daniel Pfeifer
 *
 * @deprecated This class is expected to be replaced by {@link org.jboss.eap.qe.microprofile.jwt.auth.tool.AbstractKeyLoader};
 *             Please consider switching to {@link org.jboss.eap.qe.microprofile.jwt.auth.tool.AbstractKeyLoader} when modifying
 *             users
 *             of this class;
 *             Please consider adding to {@link org.jboss.eap.qe.microprofile.jwt.auth.tool.AbstractKeyLoader} when in need for
 *             modifications or additions to this class;
 */
@Deprecated
public final class RsaKeyTool {
    private RSAPublicKey publicKey;
    private RSAPrivateCrtKey privateKey;
    private String keyId;

    /**
     * Returns a new {@link RsaKeyTool} fully prepared with a RSA Key Pair.
     *
     * @param pkcs8Key PKCS8-formatted private key
     * @return a {@link RsaKeyTool}.
     * @throws IllegalArgumentException thrown if RSA and/or a key-length is not supported by the JVM.
     */
    public static RsaKeyTool newKeyTool(final URI pkcs8Key) {
        return newKeyTool(pkcs8Key, "TESTSUITE_KEY_ID");
    }

    /**
     * Returns a new {@link RsaKeyTool} fully prepared with a RSA Key Pair.
     *
     * @param pkcs8Key PKCS8-formatted private key
     * @param keyId key ID which is used to identify "correct" key among others in JSON Web Key Set
     * @return a {@link RsaKeyTool}.
     * @throws IllegalArgumentException thrown if RSA and/or a key-length is not supported by the JVM.
     */
    public static RsaKeyTool newKeyTool(final URI pkcs8Key, final String keyId) {
        final RsaKeyTool rsaKeyTool = new RsaKeyTool();
        rsaKeyTool.prepare(pkcs8Key, keyId);

        return rsaKeyTool;
    }

    /**
     * Returns the "kid" (for use in JWK Objects) associated with this instance.
     *
     * @return a hex-formatted UUID string.
     */
    public String getJwkKeyId() {
        return keyId;
    }

    /**
     * Returns a JWK-formatted JSON using the public key associated with this instance.
     *
     * @return a JWK object.
     */
    public JsonObject getJwkPublicKeyObject() {
        byte[] modBytes = publicKey.getModulus().toByteArray();
        if (modBytes[0] == 0) { // if first byte is 0, we should strip it before encoding
            modBytes = Arrays.copyOfRange(modBytes, 1, modBytes.length);
        }
        final String exp = Base64.getUrlEncoder().encodeToString(publicKey.getPublicExponent().toByteArray());
        final String mod = Base64.getUrlEncoder().encodeToString(modBytes);

        return Json.createObjectBuilder()
                .add("alg", "RS256")
                .add("use", "sig")
                .add("kty", "RSA")
                .add("kid", keyId)
                .add("e", exp)
                .add("n", mod)
                .build();
    }

    /**
     * Returns a Base64-encoded representation of the public key (OpenSSL PEM).
     *
     * @return a base64-encoded public key PEM.
     */
    public String getPublicKeyPEM() {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    /**
     * Returns the private key.
     *
     * @return private key.
     */
    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    private void prepare(final URI pkcs8Key, final String keyId) {
        this.keyId = keyId;

        try (final InputStreamReader isr = new InputStreamReader(new FileInputStream(new File(pkcs8Key)), UTF_8);
                final BufferedReader bufferedFileReader = new BufferedReader(isr)) {

            final StringBuilder file = new StringBuilder();
            String line;
            while ((line = bufferedFileReader.readLine()) != null) {
                file.append(line.trim());
            }

            final String base64Key = file.toString().replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replace("\n", "")
                    .replace("\r", "");

            final PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64Key));
            final KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            privateKey = (RSAPrivateCrtKey) keyFactory.generatePrivate(pkcs8EncodedKeySpec);
            final RSAPublicKeySpec rsaPublicKeySpec = new RSAPublicKeySpec(privateKey.getModulus(),
                    privateKey.getPublicExponent());
            publicKey = (RSAPublicKey) keyFactory.generatePublic(rsaPublicKeySpec);
        } catch (final IOException e) {
            throw new IllegalArgumentException("Unreadable PKCS8 Private Key", e);
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("RSA is not supported by this JVM.", e);
        } catch (final InvalidKeySpecException e) {
            throw new IllegalArgumentException("Not a valid RSA private key.", e);
        }
    }

    private RsaKeyTool() {
        // intentionally left empty
    }
}
