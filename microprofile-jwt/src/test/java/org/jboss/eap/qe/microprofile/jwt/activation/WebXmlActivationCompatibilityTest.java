package org.jboss.eap.qe.microprofile.jwt.activation;

import static org.hamcrest.CoreMatchers.equalTo;

import java.net.URISyntaxException;
import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.eap.qe.microprofile.jwt.EnableJwtSubsystemSetupTask;
import org.jboss.eap.qe.microprofile.jwt.auth.tool.JsonWebToken;
import org.jboss.eap.qe.microprofile.jwt.auth.tool.JwtHelper;
import org.jboss.eap.qe.microprofile.jwt.auth.tool.RsaKeyTool;
import org.jboss.eap.qe.microprofile.jwt.testapp.Endpoints;
import org.jboss.eap.qe.microprofile.jwt.testapp.jaxrs.NonJwtJaxRsTestApplication;
import org.jboss.eap.qe.microprofile.jwt.testapp.jaxrs.SecuredJaxRsEndpoint;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.restassured.RestAssured;

/**
 * Set of tests which are verifying that JWT subsystem can be activated by configuring {@code auth-method} in app's
 * web.xml.
 */
@RunWith(Arquillian.class)
@ServerSetup(EnableJwtSubsystemSetupTask.class)
public class WebXmlActivationCompatibilityTest {

    private static RsaKeyTool keyTool;

    @BeforeClass
    public static void beforeClass() throws URISyntaxException {
        final URL privateKeyUrl = WebXmlActivationCompatibilityTest.class.getClassLoader()
                .getResource("pki/RS256/key.private.pkcs8.pem");
        if (privateKeyUrl == null) {
            throw new IllegalStateException("Private key wasn't found in resources!");
        }
        keyTool = RsaKeyTool.newKeyTool(privateKeyUrl.toURI());
    }

    @Deployment(testable = false)
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(WebArchive.class, WebXmlActivationCompatibilityTest.class.getSimpleName() + ".war")
                .addClass(SecuredJaxRsEndpoint.class)
                .addClass(NonJwtJaxRsTestApplication.class)
                .setWebXML(
                        WebXmlActivationCompatibilityTest.class.getClassLoader().getResource("activation-web.xml"))
                .addAsManifestResource(
                        WebXmlActivationCompatibilityTest.class.getClassLoader()
                                .getResource("mp-config-basic-RS256.properties"),
                        "microprofile-config.properties")
                .addAsManifestResource(
                        WebXmlActivationCompatibilityTest.class.getClassLoader().getResource("pki/RS256/key.public.pem"),
                        "key.public.pem");
    }

    /**
     * @tpTestDetails Test that subsystem was correctly activated by setting {@code auth-method} in {@code web.xml} to
     *                {@code MP-JWT}.
     * @tpPassCrit Same token which was sent on server is received in response.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @RunAsClient
    public void testSameTokenReceived(@ArquillianResource URL url) {
        JsonWebToken token = new JwtHelper(keyTool).generateProperSignedJwt();

        RestAssured.given().header("Authorization", "Bearer " + token.getRawValue())
                .when().get(url.toExternalForm() + Endpoints.SECURED_ENDPOINT)
                .then().body(equalTo(token.getRawValue()));
    }

}
