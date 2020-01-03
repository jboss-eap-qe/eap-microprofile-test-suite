package org.jboss.eap.qe.microprofile.jwt.cdi;

import static org.hamcrest.CoreMatchers.equalTo;

import java.net.URISyntaxException;
import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.eap.qe.microprofile.jwt.auth.tool.JsonWebToken;
import org.jboss.eap.qe.microprofile.jwt.auth.tool.JwtHelper;
import org.jboss.eap.qe.microprofile.jwt.auth.tool.RsaKeyTool;
import org.jboss.eap.qe.microprofile.jwt.testapp.jaxrs.JaxRsTestApplication;
import org.jboss.eap.qe.microprofile.jwt.testapp.jaxrs.SecuredJaxRsEndpoint;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.restassured.RestAssured;

/**
 * Just verify a raw token value can be injected into a variable in an application class a compare it has expected value
 */
@RunWith(Arquillian.class)
public class ActivationCompatibilityTest {

    private static RsaKeyTool keyTool;

    @BeforeClass
    public static void beforeClass() throws URISyntaxException {
        final URL privateKeyUrl = ActivationCompatibilityTest.class.getClassLoader().getResource("pki/key.private.pkcs8.pem");
        if (privateKeyUrl == null) {
            throw new IllegalStateException("Private key wasn't found in resources!");
        }
        keyTool = RsaKeyTool.newKeyTool(privateKeyUrl.toURI());
    }

    @Deployment
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(WebArchive.class, ActivationCompatibilityTest.class.getSimpleName() + ".war")
                .addClass(SecuredJaxRsEndpoint.class)
                .addClass(JaxRsTestApplication.class)
                .addAsManifestResource(
                        ActivationCompatibilityTest.class.getClassLoader().getResource("mp-config-basic.properties"),
                        "microprofile-config.properties")
                .addAsManifestResource(ActivationCompatibilityTest.class.getClassLoader().getResource("pki/key.public.pem"),
                        "key.public.pem");
    }

    /**
     * @tpTestDetails Testing CDI specification compatibility
     * @tpPassCrit Same token which was sent on server is received in response.
     * @tpSince EAP 7.3.0.CD19
     */
    @Test
    @RunAsClient
    public void testSameTokenReceived(@ArquillianResource URL url) {
        JsonWebToken token = new JwtHelper(keyTool, "issuer").generateProperSignedJwt();

        RestAssured.given().header("Authorization", "Bearer " + token.getRawValue())
                .when().get(url.toExternalForm() + "secured-endpoint")
                .then().body(equalTo(token.getRawValue()));
    }

}
