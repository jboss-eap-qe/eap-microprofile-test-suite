package org.jboss.eap.qe.microprofile.jwt.security.keyproperties;

import static io.restassured.RestAssured.given;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
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
import org.jboss.eap.qe.microprofile.jwt.security.publickeylocation.PublicKeyPropertyTestCase;
import org.jboss.eap.qe.microprofile.jwt.testapp.Endpoints;
import org.jboss.eap.qe.microprofile.jwt.testapp.jaxrs.JaxRsTestApplication;
import org.jboss.eap.qe.microprofile.jwt.testapp.jaxrs.SecuredJaxRsEndpoint;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.ConfigurationException;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.jboss.eap.qe.microprofile.tooling.server.log.ModelNodeLogChecker;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;

/**
 * Test verifying that when corrupted public key is supplied to the server, there is proper message printed in log
 * informing about this
 */
@RunAsClient
@RunWith(Arquillian.class)
@ServerSetup(EnableJwtSubsystemSetupTask.class)
public class CorruptedKeyTest {

    private static RsaKeyTool keyTool;

    @BeforeClass
    public static void beforeClass() throws URISyntaxException {
        final URL privateKeyUrl = PublicKeyPropertyTestCase.class.getClassLoader()
                .getResource("pki/key.private.pkcs8.pem");
        if (privateKeyUrl == null) {
            throw new IllegalStateException("Private key wasn't found in resources!");
        }
        keyTool = RsaKeyTool.newKeyTool(privateKeyUrl.toURI());
    }

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        //this really isn't valid base64 encoded public key
        final String mpProperties = "mp.jwt.verify.publickey=foobarqux\n" +
                "mp.jwt.verify.issuer=issuer";

        return ShrinkWrap.create(WebArchive.class, CorruptedKeyTest.class.getSimpleName() + ".war")
                .addClass(JaxRsTestApplication.class)
                .addClass(SecuredJaxRsEndpoint.class)
                .addAsManifestResource(new StringAsset(mpProperties), "microprofile-config.properties");
    }

    /**
     * @tpTestDetails A deployment which has configured corrupted public key is deployed on server.
     * @tpPassCrit Verify, that error 500 is reported to user in that case and it is also noted in log.
     * @tpSince EAP 7.4.0.CD19
     */
    @Ignore("https://issues.redhat.com/browse/WFLY-13164")
    @Test
    public void verifyErrorIsShownInLog(@ArquillianResource URL url) throws ConfigurationException, IOException {
        final JsonWebToken token = new JwtHelper(keyTool).generateProperSignedJwt();

        given().header("Authorization", "Bearer " + token.getRawValue())
                .when().get(url.toExternalForm() + Endpoints.SECURED_ENDPOINT)
                .then()
                .statusCode(500);

        try (final OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
            assertTrue("There is no error shown in log informing user that key is corrupted",
                    new ModelNodeLogChecker(client, 20).logContains("UT005023"));
        }
    }

}
