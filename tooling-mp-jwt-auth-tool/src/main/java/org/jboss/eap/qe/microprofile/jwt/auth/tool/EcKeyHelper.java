package org.jboss.eap.qe.microprofile.jwt.auth.tool;

import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;

import org.jose4j.jwk.EcJwkGenerator;
import org.jose4j.jwk.EllipticCurveJsonWebKey;
import org.jose4j.keys.EllipticCurves;
import org.jose4j.lang.JoseException;

public class EcKeyHelper extends AbstractKeyLoader<ECPrivateKey, ECPublicKey> {
    public EcKeyHelper() {
        this.algorithm = "EC";
    }

    /**
     * Generate an EC key pair wrapped in a JWK
     *
     * @return
     * @throws JoseException
     */
    public EllipticCurveJsonWebKey generate() throws JoseException {
        EllipticCurveJsonWebKey ecJsonWebKey = EcJwkGenerator.generateJwk(EllipticCurves.P256);
        return ecJsonWebKey;
    }
}
