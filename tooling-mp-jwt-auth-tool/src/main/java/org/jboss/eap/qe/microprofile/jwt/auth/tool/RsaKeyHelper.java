package org.jboss.eap.qe.microprofile.jwt.auth.tool;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jwk.RsaJwkGenerator;
import org.jose4j.lang.JoseException;

public class RsaKeyHelper extends AbstractKeyLoader<RSAPrivateKey, RSAPublicKey> {

    public RsaKeyHelper() {
        this.algorithm = "RSA";
    }

    /**
     * Generate an RSA key pair wrapped in a JWK
     *
     * @param keyLength
     * @return
     * @throws JoseException
     */
    public RsaJsonWebKey generate(int keyLength) throws JoseException {
        RsaJsonWebKey rsaJsonWebKey = RsaJwkGenerator.generateJwk(keyLength);
        return rsaJsonWebKey;
    }
}
