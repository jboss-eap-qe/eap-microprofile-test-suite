package org.jboss.eap.qe.microprofile.jwt.compatibility;

import static io.restassured.RestAssured.given;

import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.eap.qe.microprofile.jwt.EnableJwtSubsystemSetupTask;
import org.jboss.eap.qe.microprofile.jwt.security.rbac.jwe.JweTestCase;
import org.jboss.eap.qe.microprofile.jwt.testapp.Endpoints;
import org.jboss.eap.qe.microprofile.jwt.testapp.jaxrs.JaxRsTestApplication;
import org.jboss.eap.qe.microprofile.jwt.testapp.jaxrs.SecuredJaxRsEndpoint;
import org.jboss.eap.qe.microprofile.jwt.testapp.servlet.SecuredServlet;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * The @see org.eclipse.microprofile.auth.LoginConfig annotation provides the same information as the web.xml login-config
 * element; this tests verifies that, when present, the following behaviours are the same for both {@code MP-JWT} and
 * {@code BASIC}
 * authentication:
 * <ul>
 * <li>no authentication provided, be it JWT token or BASIC, leads to HTTP 200</li>
 * <li>wrong authentication provided, be it JWT token or BASIC, leads to HTTP 401</li>
 * </ul>
 *
 * @author tborgato
 */
@RunAsClient
@RunWith(Arquillian.class)
@ServerSetup(EnableJwtSubsystemSetupTask.class)
public class MissingJwtTest {

    private static final String MP_JWT_AUTHENTICATION_DEPLOYMENT = "MissingJwtTest-first-deployment";
    private static final String BASIC_AUTHENTICATION_DEPLOYMENT = "MissingJwtTest-second-deployment";

    @Deployment(name = MP_JWT_AUTHENTICATION_DEPLOYMENT)
    public static WebArchive createDeploymentMpJwtAuth() {
        return ShrinkWrap.create(WebArchive.class, MP_JWT_AUTHENTICATION_DEPLOYMENT + ".war")
                .addClass(SecuredJaxRsEndpoint.class)
                .addClass(JaxRsTestApplication.class)
                .addAsManifestResource(MissingJwtTest.class.getClassLoader().getResource("mp-config-basic-RS256.properties"),
                        "microprofile-config.properties")
                .addAsManifestResource(MissingJwtTest.class.getClassLoader().getResource("pki/RS256/key.public.pem"),
                        "key.public.pem");
    }

    @Deployment(name = BASIC_AUTHENTICATION_DEPLOYMENT)
    public static WebArchive createDeploymentBasiAuth() {
        return ShrinkWrap.create(WebArchive.class, BASIC_AUTHENTICATION_DEPLOYMENT + ".war")
                .setWebXML(JweTestCase.class.getClassLoader().getResource("basic-authentication.web.xml"))
                .addClass(SecuredServlet.class);
    }

    /**
     * @tpTestDetails Supply no JWT to server
     * @tpPassCrit Expect HTTP/200
     * @tpSince EAP 7.4.0.CD23
     */
    @Test
    public void supplyNoJwtToServerTest(@ArquillianResource @OperateOnDeployment(MP_JWT_AUTHENTICATION_DEPLOYMENT) URL url) {
        given()
                .when().get(url.toExternalForm() + Endpoints.SECURED_ENDPOINT + "/protected")
                .then()
                .statusCode(200);
    }

    /**
     * @tpTestDetails Supply an invalid JWT to server
     * @tpPassCrit Invalid JWT is rejected and user receives 401/Unauthorized
     * @tpSince EAP 7.4.0.CD23
     */
    @Test
    public void supplyInvalidJwtToServerTest(
            @ArquillianResource @OperateOnDeployment(MP_JWT_AUTHENTICATION_DEPLOYMENT) URL url) {
        given()
                .header("Authorization", "Bearer X")
                .when().get(url.toExternalForm() + Endpoints.SECURED_ENDPOINT + "/protected")
                .then()
                .statusCode(401);
    }

    /**
     * @tpTestDetails Supply invalid BASIC authentication to server
     * @tpPassCrit Expect 401/Unauthorized
     * @tpSince EAP 7.4.0.CD23
     */
    @Test
    public void supplyInvalidBasicAuthToServerTest(
            @ArquillianResource @OperateOnDeployment(BASIC_AUTHENTICATION_DEPLOYMENT) URL url) {
        given()
                .header("Authorization", "Basic sOmEwRoNgAuThEnThIcAtIoN")
                .when().get(url.toExternalForm() + "SecuredServlet")
                .then()
                .statusCode(401);
    }

    /**
     * @tpTestDetails Supply no authentication to server
     * @tpPassCrit 200 is returned by server since no authentication attempt was made
     * @tpSince EAP 7.4.0.CD23
     */
    @Test
    public void supplyNoBasicAuthToServerTest(
            @ArquillianResource @OperateOnDeployment(BASIC_AUTHENTICATION_DEPLOYMENT) URL url) {
        given()
                .when().get(url.toExternalForm() + "SecuredServlet")
                .then()
                .statusCode(200);
    }

}
