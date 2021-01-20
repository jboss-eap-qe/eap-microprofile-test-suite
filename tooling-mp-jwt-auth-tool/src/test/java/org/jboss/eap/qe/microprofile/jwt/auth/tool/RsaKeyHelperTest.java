package org.jboss.eap.qe.microprofile.jwt.auth.tool;

import org.junit.Test;

public class RsaKeyHelperTest {

    /**
     * @tpTestDetails Reads a private key from a pem file
     * @tpPassCrit Expect no Exceptions
     * @tpSince EAP 7.4.0.CD23
     */
    @Test
    public void readPrivateKey() throws Exception {
        RsaKeyHelper loader = new RsaKeyHelper();
        loader.readPrivateKey(RsaKeyHelperTest.class.getClassLoader().getResource("pki/RS256/key.private.pkcs8.pem").toURI());
    }

    /**
     * @tpTestDetails Reads a public key from a pem file
     * @tpPassCrit Expect no Exceptions
     * @tpSince EAP 7.4.0.CD23
     */
    @Test
    public void readPublicKey() throws Exception {
        RsaKeyHelper loader = new RsaKeyHelper();
        loader.readPublicKey(RsaKeyHelperTest.class.getClassLoader().getResource("pki/RS256/key.public.pem").toURI());
    }
}
