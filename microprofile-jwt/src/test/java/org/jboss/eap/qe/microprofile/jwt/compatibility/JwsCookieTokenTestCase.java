package org.jboss.eap.qe.microprofile.jwt.compatibility;

import static io.restassured.RestAssured.given;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.eap.qe.microprofile.jwt.EnableJwtSubsystemSetupTask;
import org.jboss.eap.qe.microprofile.jwt.auth.tool.JwsHelper;
import org.jboss.eap.qe.microprofile.jwt.auth.tool.JwtDefaultClaimValues;
import org.jboss.eap.qe.microprofile.jwt.testapp.Endpoints;
import org.jboss.eap.qe.microprofile.jwt.testapp.Roles;
import org.jboss.eap.qe.microprofile.jwt.testapp.jaxrs.JaxRsTestApplication;
import org.jboss.eap.qe.microprofile.jwt.testapp.jaxrs.JwtRbacTestEndpoint;
import org.jboss.eap.qe.microprofile.jwt.testapp.jaxrs.SecuredJaxRsEndpoint;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * MP JWT 1.2 introduced the support for JWT token cookies (see https://github.com/eclipse/microprofile-jwt-auth/issues/93);
 * this test verifies that:
 *
 * - JWT token cookies are correctly used for granting/denying authentication and authorization
 * - misconfiguration of cookie name, correctly leads to denying authentication
 * - missing roles in roles claim, correctly leads to denying authorization
 *
 * both RS256 and ES256 signed JWT tokens cookies are tested
 *
 * @author tborgato
 */
// TODO: there is space for future improvements e.g. testing that when expired JWT is supplied in cookie, authentication is denied
@RunAsClient
@RunWith(Arquillian.class)
@ServerSetup(EnableJwtSubsystemSetupTask.class)
public class JwsCookieTokenTestCase {

    private static final String RS256_DEPLOYMENT = "JwsCookieTokenTestCase-first-deployment";
    private static final String ES256_DEPLOYMENT = "JwsCookieTokenTestCase-second-deployment";

    @Deployment(name = RS256_DEPLOYMENT)
    public static WebArchive createDeploymentRS256() {
        return ShrinkWrap.create(WebArchive.class, RS256_DEPLOYMENT + ".war")
                .setWebXML(JwsCookieTokenTestCase.class.getClassLoader().getResource("activate-roles.web.xml"))
                .addClass(SecuredJaxRsEndpoint.class)
                .addClass(JwtRbacTestEndpoint.class)
                .addClass(JaxRsTestApplication.class)
                .addAsManifestResource(
                        JwsCookieTokenTestCase.class.getClassLoader().getResource("mp-config-cookie-RS256.properties"),
                        "microprofile-config.properties")
                .addAsManifestResource(JwsCookieTokenTestCase.class.getClassLoader().getResource("pki/RS256/key.public.pem"),
                        "key.public.pem");
    }

    @Deployment(name = ES256_DEPLOYMENT)
    public static WebArchive createDeploymentES256() {
        return ShrinkWrap.create(WebArchive.class, ES256_DEPLOYMENT + ".war")
                .setWebXML(JwsCookieTokenTestCase.class.getClassLoader().getResource("activate-roles.web.xml"))
                .addClass(SecuredJaxRsEndpoint.class)
                .addClass(JwtRbacTestEndpoint.class)
                .addClass(JaxRsTestApplication.class)
                .addAsManifestResource(
                        JwsCookieTokenTestCase.class.getClassLoader().getResource("mp-config-cookie-ES256.properties"),
                        "microprofile-config.properties")
                .addAsManifestResource(JwsCookieTokenTestCase.class.getClassLoader().getResource("pki/ES256/key.public.pem"),
                        "key.public.pem");
    }

    /**
     * @tpTestDetails Test invokes a JAX-RS endpoint which expects authentication and authorization provided by an
     *                RS signed JWS token supplied as cookie containing the required claims for authorization;
     * @tpPassCrit the JAX-RS endpoint correctly authenticates and authorizes the request
     * @tpSince EAP 7.4.0.CD23
     */
    @Test
    public void supplyJwtCookieTokenRS256ToServerTest(@ArquillianResource @OperateOnDeployment(RS256_DEPLOYMENT) URL url)
            throws Exception {
        final URL jswPrivateKeyUrl = JwsCookieTokenTestCase.class.getClassLoader()
                .getResource("pki/RS256/key.private.pkcs8.pem");
        if (jswPrivateKeyUrl == null) {
            throw new IllegalStateException("JWS Private key wasn't found in resources!");
        }

        JwsHelper jwsHelper = new JwsHelper();
        JsonWebSignature jsonWebSignature = jwsHelper.generateProperSignedJwt(
                AlgorithmIdentifiers.RSA_USING_SHA256,
                jswPrivateKeyUrl.toURI(),
                "k1",
                JwtDefaultClaimValues.ISSUER,
                JwtDefaultClaimValues.AUDIENCE,
                JwtDefaultClaimValues.SUBJECT,
                Arrays.asList(Roles.MONITOR),
                Collections.EMPTY_MAP);
        String jws = jsonWebSignature.getCompactSerialization();

        // test everything works fine when configuration is correct
        given().cookie("jws-correct-cookie", jws)
                .when().get(url.toExternalForm() + Endpoints.RBAC_ENDPOINT + "/" + Roles.MONITOR)
                .then()
                .statusCode(200);
    }

    /**
     * @tpTestDetails Test invokes a JAX-RS endpoint which expects authentication and authorization provided by an
     *                RS signed JWS token supplied as cookie containing the required claims for authorization;
     *                the cookie has a name different from the name configured in the JAX-RS endpoint
     * @tpPassCrit the JAX-RS endpoint fails authentication
     * @tpSince EAP 7.4.0.CD23
     */
    @Ignore("https://issues.redhat.com/browse/JBEAP-20948")
    @Test
    public void supplyJwtCookieTokenRS256WithWrongNameToServerTest(
            @ArquillianResource @OperateOnDeployment(RS256_DEPLOYMENT) URL url) throws Exception {
        final URL jswPrivateKeyUrl = JwsCookieTokenTestCase.class.getClassLoader()
                .getResource("pki/RS256/key.private.pkcs8.pem");
        if (jswPrivateKeyUrl == null) {
            throw new IllegalStateException("JWS Private key wasn't found in resources!");
        }

        JwsHelper jwsHelper = new JwsHelper();
        JsonWebSignature jsonWebSignature = jwsHelper.generateProperSignedJwt(
                AlgorithmIdentifiers.RSA_USING_SHA256,
                jswPrivateKeyUrl.toURI(),
                "k1",
                JwtDefaultClaimValues.ISSUER,
                JwtDefaultClaimValues.AUDIENCE,
                JwtDefaultClaimValues.SUBJECT,
                Arrays.asList(Roles.MONITOR),
                Collections.EMPTY_MAP);
        String jws = jsonWebSignature.getCompactSerialization();

        given().cookie("jws-wrong-cookie", jws)
                .when().get(url.toExternalForm() + Endpoints.RBAC_ENDPOINT + "/" + Roles.MONITOR)
                .then()
                .statusCode(401);
    }

    /**
     * @tpTestDetails Test invokes a JAX-RS endpoint which expects authentication and authorization provided by an
     *                RS signed JWS token supplied as cookie containing the required claims for authorization;
     *                the JWS token is missing the required role to access JAX-RS endpoint
     * @tpPassCrit the JAX-RS endpoint denies authorization
     * @tpSince EAP 7.4.0.CD23
     */
    @Test
    public void supplyJwtCookieTokenRS256WithMissingRoleToServerTest(
            @ArquillianResource @OperateOnDeployment(RS256_DEPLOYMENT) URL url) throws Exception {
        final URL jswPrivateKeyUrl = JwsCookieTokenTestCase.class.getClassLoader()
                .getResource("pki/RS256/key.private.pkcs8.pem");
        if (jswPrivateKeyUrl == null) {
            throw new IllegalStateException("JWS Private key wasn't found in resources!");
        }

        JwsHelper jwsHelper = new JwsHelper();
        JsonWebSignature jsonWebSignature = jwsHelper.generateProperSignedJwt(
                AlgorithmIdentifiers.RSA_USING_SHA256,
                jswPrivateKeyUrl.toURI(),
                "k1",
                JwtDefaultClaimValues.ISSUER,
                JwtDefaultClaimValues.AUDIENCE,
                JwtDefaultClaimValues.SUBJECT,
                Arrays.asList(Roles.MONITOR),
                Collections.EMPTY_MAP);
        String jws = jsonWebSignature.getCompactSerialization();

        given().cookie("jws-correct-cookie", jws)
                .when().get(url.toExternalForm() + Endpoints.RBAC_ENDPOINT + "/" + Roles.DIRECTOR)
                .then()
                .statusCode(403);
    }

    /**
     * @tpTestDetails Test invokes a JAX-RS endpoint which expects authentication and authorization provided by an
     *                ES encoded JWS token supplied as cookie containing the required claims for authorization;
     * @tpPassCrit the JAX-RS endpoint correctly authenticates and authorizes the request
     * @tpSince EAP 7.4.0.CD23
     */
    @Test
    public void supplyJwtCookieTokenES256ToServerTest(@ArquillianResource @OperateOnDeployment(ES256_DEPLOYMENT) URL url)
            throws Exception {
        final URL jswPrivateKeyUrl = JwsCookieTokenTestCase.class.getClassLoader()
                .getResource("pki/ES256/key.private.pkcs8.pem");
        if (jswPrivateKeyUrl == null) {
            throw new IllegalStateException("JWS Private key wasn't found in resources!");
        }

        JwsHelper jwsHelper = new JwsHelper();
        JsonWebSignature jsonWebSignature = jwsHelper.generateProperSignedJwt(
                AlgorithmIdentifiers.ECDSA_USING_P256_CURVE_AND_SHA256,
                jswPrivateKeyUrl.toURI(),
                "k1",
                JwtDefaultClaimValues.ISSUER,
                JwtDefaultClaimValues.AUDIENCE,
                JwtDefaultClaimValues.SUBJECT,
                Arrays.asList(Roles.MONITOR),
                Collections.EMPTY_MAP);
        String jws = jsonWebSignature.getCompactSerialization();

        // test everything works fine when configuration is correct
        given().cookie("jws-correct-cookie", jws)
                .when().get(url.toExternalForm() + Endpoints.RBAC_ENDPOINT + "/" + Roles.MONITOR)
                .then()
                .statusCode(200);
    }

    /**
     * @tpTestDetails Test invokes a JAX-RS endpoint which expects authentication and authorization provided by an
     *                ES encoded JWS token supplied as cookie containing the required claims for authorization;
     *                the cookie has a name different from the name configured in the JAX-RS endpoint
     * @tpPassCrit the JAX-RS endpoint fails authentication
     * @tpSince EAP 7.4.0.CD23
     */
    @Ignore("https://issues.redhat.com/browse/JBEAP-20948")
    @Test
    public void supplyJwtCookieTokenES256WithWrongNameToServerTest(
            @ArquillianResource @OperateOnDeployment(ES256_DEPLOYMENT) URL url) throws Exception {
        final URL jswPrivateKeyUrl = JwsCookieTokenTestCase.class.getClassLoader()
                .getResource("pki/ES256/key.private.pkcs8.pem");
        if (jswPrivateKeyUrl == null) {
            throw new IllegalStateException("JWS Private key wasn't found in resources!");
        }

        JwsHelper jwsHelper = new JwsHelper();
        JsonWebSignature jsonWebSignature = jwsHelper.generateProperSignedJwt(
                AlgorithmIdentifiers.ECDSA_USING_P256_CURVE_AND_SHA256,
                jswPrivateKeyUrl.toURI(),
                "k1",
                JwtDefaultClaimValues.ISSUER,
                JwtDefaultClaimValues.AUDIENCE,
                JwtDefaultClaimValues.SUBJECT,
                Arrays.asList(Roles.MONITOR),
                Collections.EMPTY_MAP);
        String jws = jsonWebSignature.getCompactSerialization();

        given().cookie("jws-wrong-cookie", jws)
                .when().get(url.toExternalForm() + Endpoints.RBAC_ENDPOINT + "/" + Roles.MONITOR)
                .then()
                .statusCode(401);
    }

    /**
     * @tpTestDetails Test invokes a JAX-RS endpoint which expects authentication and authorization provided by an
     *                ES encoded JWS token supplied as cookie containing the required claims for authorization;
     *                the JWS token is missing the required role to access JAX-RS endpoint
     * @tpPassCrit the JAX-RS endpoint denies authorization
     * @tpSince EAP 7.4.0.CD23
     */
    @Test
    public void supplyJwtCookieTokenES256WithMissingRoleToServerTest(
            @ArquillianResource @OperateOnDeployment(ES256_DEPLOYMENT) URL url) throws Exception {
        final URL jswPrivateKeyUrl = JwsCookieTokenTestCase.class.getClassLoader()
                .getResource("pki/ES256/key.private.pkcs8.pem");
        if (jswPrivateKeyUrl == null) {
            throw new IllegalStateException("JWS Private key wasn't found in resources!");
        }

        JwsHelper jwsHelper = new JwsHelper();
        JsonWebSignature jsonWebSignature = jwsHelper.generateProperSignedJwt(
                AlgorithmIdentifiers.ECDSA_USING_P256_CURVE_AND_SHA256,
                jswPrivateKeyUrl.toURI(),
                "k1",
                JwtDefaultClaimValues.ISSUER,
                JwtDefaultClaimValues.AUDIENCE,
                JwtDefaultClaimValues.SUBJECT,
                Arrays.asList(Roles.MONITOR),
                Collections.EMPTY_MAP);
        String jws = jsonWebSignature.getCompactSerialization();

        given().cookie("jws-correct-cookie", jws)
                .when().get(url.toExternalForm() + Endpoints.RBAC_ENDPOINT + "/" + Roles.DIRECTOR)
                .then()
                .statusCode(403);
    }
}
