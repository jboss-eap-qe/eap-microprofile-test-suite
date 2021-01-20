package org.jboss.eap.qe.microprofile.jwt.auth.tool;

import org.junit.Test;

public class EcKeyHelperTest {

    /**
     * @tpTestDetails Reads a private key from a pem file
     * @tpPassCrit Expect no Exceptions
     * @tpSince EAP 7.4.0.CD23
     */
    @Test
    public void readPrivateKey() throws Exception {
        EcKeyHelper loader = new EcKeyHelper();
        loader.readPrivateKey(EcKeyHelperTest.class.getClassLoader().getResource("pki/ES256/key.private.pkcs8.pem").toURI());
    }

    /**
     * @tpTestDetails Reads a public key from a pem file
     * @tpPassCrit Expect no Exceptions
     * @tpSince EAP 7.4.0.CD23
     */
    @Test
    public void readPublicKey() throws Exception {
        EcKeyHelper loader = new EcKeyHelper();
        loader.readPublicKey(EcKeyHelperTest.class.getClassLoader().getResource("pki/ES256/key.public.pem").toURI());
    }
}
