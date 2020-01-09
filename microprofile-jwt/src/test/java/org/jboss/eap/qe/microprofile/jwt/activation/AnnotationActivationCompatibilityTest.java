package org.jboss.eap.qe.microprofile.jwt.activation;

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
import org.jboss.eap.qe.microprofile.jwt.testapp.Endpoints;
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
 * Set of tests which are verifying that {@code @LoginConfig} annotation works as it should - activating the JWT
 * subsystem when configured properly.
 */
@RunWith(Arquillian.class)
public class AnnotationActivationCompatibilityTest {

    private static RsaKeyTool keyTool;

    @BeforeClass
    public static void beforeClass() throws URISyntaxException {
        final URL privateKeyUrl = AnnotationActivationCompatibilityTest.class.getClassLoader()
                .getResource("pki/key.private.pkcs8.pem");
        if (privateKeyUrl == null) {
            throw new IllegalStateException("Private key wasn't found in resources!");
        }
        keyTool = RsaKeyTool.newKeyTool(privateKeyUrl.toURI());
    }

    @Deployment
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(WebArchive.class, AnnotationActivationCompatibilityTest.class.getSimpleName() + ".war")
                .addClass(SecuredJaxRsEndpoint.class)
                .addClass(JaxRsTestApplication.class)
                .addAsManifestResource(
                        AnnotationActivationCompatibilityTest.class.getClassLoader().getResource("mp-config-basic.properties"),
                        "microprofile-config.properties")
                .addAsManifestResource(
                        AnnotationActivationCompatibilityTest.class.getClassLoader().getResource("pki/key.public.pem"),
                        "key.public.pem");
    }

    /**
     * @tpTestDetails Test that subsystem was correctly activated by using {@code @LoginConfig(authMethod = "MP-JWT",
     *         realmName = "MP-JWT")}.
     * @tpPassCrit Same token which was sent on server is received in response.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @RunAsClient
    public void testSameTokenReceived(@ArquillianResource URL url) {
        JsonWebToken token = new JwtHelper(keyTool, "issuer").generateProperSignedJwt();

        RestAssured.given().header("Authorization", "Bearer " + token.getRawValue())
                .when().get(url.toExternalForm() + Endpoints.SECURED_ENDPOINT)
                .then().body(equalTo(token.getRawValue()));
    }

}
