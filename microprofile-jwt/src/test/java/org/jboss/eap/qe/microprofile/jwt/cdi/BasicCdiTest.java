package org.jboss.eap.qe.microprofile.jwt.cdi;

import io.restassured.RestAssured;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.eap.qe.microprofile.jwt.testapp.jaxrs.JaxRsBasicEndpoint;
import org.jboss.eap.qe.microprofile.jwt.testapp.jaxrs.JaxRsTestApplication;
import org.jboss.eap.qe.microprofile.jwt.tools.JsonWebToken;
import org.jboss.eap.qe.microprofile.jwt.tools.JwtHelper;
import org.jboss.eap.qe.microprofile.jwt.tools.RsaKeyTool;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URISyntaxException;
import java.net.URL;

import static org.hamcrest.CoreMatchers.equalTo;


/**
 * Just verify a raw token value can be injected into a variable in an application class a compare it has expected value
 */
@RunWith(Arquillian.class)
public class BasicCdiTest {

    private static RsaKeyTool keyTool;

    @BeforeClass
    public static void beforeClass() throws URISyntaxException {
        final URL privateKeyUrl = BasicCdiTest.class.getClassLoader().getResource("pki/key.private.pkcs8.pem");
        if (privateKeyUrl == null) {
            throw new IllegalStateException("Private key wasn't found in resources!");
        }
        keyTool = RsaKeyTool.newKeyTool(privateKeyUrl.toURI());
    }

    @Deployment
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(WebArchive.class)
                .addClass(JaxRsBasicEndpoint.class)
                .addClass(JaxRsTestApplication.class)
                .addAsManifestResource(BasicCdiTest.class.getClassLoader().getResource("mp-config-basic.properties"), "microprofile-config.properties")
                .addAsManifestResource(BasicCdiTest.class.getClassLoader().getResource("pki/key.public.pem"), "key.public.pem");
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
                .when().get(url.toExternalForm() + "basic-endpoint")
                .then().body(equalTo(token.getRawValue()));
    }

}
