package org.jboss.eap.qe.microprofile.jwt.auth.tool;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.security.KeyFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import org.jose4j.base64url.internal.apache.commons.codec.binary.Base64;

/**
 * Utility class for reading private and public keys from pem files;
 * Note the keys have to be in {@code pkcs8} format (Public Key Cryptography Standards #8 (PKCS#8) PEM format)
 * which is thr format supported by MP JWT (see "Supported Public Key Formats" in "Eclipse MicroProfile Interoperable
 * JWT RBAC");
 *
 * @param <PRIVATE_KEY> : Private Key interface for the algorithm in use
 * @param <PUBLIC_KEY> : Public Key interface for the algorithm in use
 * @author tborgato
 */
public abstract class AbstractKeyLoader<PRIVATE_KEY, PUBLIC_KEY> {

    protected String algorithm;

    /**
     * Read a private key in {@code pkcs8} format from the specified location
     *
     * @param pkcs8Key location of the file containing the key
     * @return
     * @throws Exception
     */
    public PRIVATE_KEY readPrivateKey(final URI pkcs8Key) throws Exception {
        assert pkcs8Key != null : "Private Key URI is null";
        try (final InputStreamReader isr = new InputStreamReader(new FileInputStream(new File(pkcs8Key)), UTF_8);
                final BufferedReader bufferedFileReader = new BufferedReader(isr)) {
            final StringBuilder key = new StringBuilder();
            String line;
            while ((line = bufferedFileReader.readLine()) != null) {
                key.append(line.trim());
            }
            String privateKeyPEM = key.toString()
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replaceAll(System.lineSeparator(), "")
                    .replace("-----END PRIVATE KEY-----", "");

            byte[] encoded = Base64.decodeBase64(privateKeyPEM);

            KeyFactory keyFactory = KeyFactory.getInstance(algorithm);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
            return (PRIVATE_KEY) keyFactory.generatePrivate(keySpec);
        }
    }

    /**
     * Read a public key in {@code pkcs8} format from the specified location
     *
     * @param pemKey
     * @return location of the file containing the key
     * @throws Exception
     */
    public PUBLIC_KEY readPublicKey(final URI pemKey) throws Exception {
        assert pemKey != null : "Public Key URI is null";
        try (final InputStreamReader isr = new InputStreamReader(new FileInputStream(new File(pemKey)), UTF_8);
                final BufferedReader bufferedFileReader = new BufferedReader(isr)) {
            final StringBuilder key = new StringBuilder();
            String line;
            while ((line = bufferedFileReader.readLine()) != null) {
                key.append(line.trim());
            }
            String publicKeyPEM = key.toString()
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replaceAll(System.lineSeparator(), "")
                    .replace("-----END PUBLIC KEY-----", "");

            byte[] encoded = Base64.decodeBase64(publicKeyPEM);

            KeyFactory keyFactory = KeyFactory.getInstance(algorithm);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
            return (PUBLIC_KEY) keyFactory.generatePublic(keySpec);
        }
    }
}
