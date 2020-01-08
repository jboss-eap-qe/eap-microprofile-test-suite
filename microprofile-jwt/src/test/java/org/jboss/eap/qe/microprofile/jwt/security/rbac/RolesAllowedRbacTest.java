package org.jboss.eap.qe.microprofile.jwt.security.rbac;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.eap.qe.microprofile.jwt.auth.tool.JsonWebToken;
import org.jboss.eap.qe.microprofile.jwt.auth.tool.JwtHelper;
import org.jboss.eap.qe.microprofile.jwt.auth.tool.RsaKeyTool;
import org.jboss.eap.qe.microprofile.jwt.testapp.Roles;
import org.jboss.eap.qe.microprofile.jwt.testapp.jaxrs.JaxRsTestApplication;
import org.jboss.eap.qe.microprofile.jwt.testapp.jaxrs.JwtRbacTestEndpoint;
import org.jboss.eap.qe.microprofile.jwt.testapp.jaxrs.SecuredJaxRsEndpoint;
import org.jboss.eap.qe.microprofile.jwt.testapp.jaxrs.UnsecuredJaxRsEndpoint;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Testing mapping of {@code groups} claim to application server roles using {@code @RolesAllowed}, {@code @PermitAll}
 * and {@code @DenyAll} annotations.
 */
@RunAsClient
@RunWith(Arquillian.class)
public class RolesAllowedRbacTest {

    private static RsaKeyTool keyTool;

    @BeforeClass
    public static void beforeClass() throws URISyntaxException {
        final URL privateKeyUrl = RolesAllowedRbacTest.class.getClassLoader().getResource("pki/key.private.pkcs8.pem");
        if (privateKeyUrl == null) {
            throw new IllegalStateException("Private key wasn't found in resources!");
        }
        keyTool = RsaKeyTool.newKeyTool(privateKeyUrl.toURI());
    }

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, RolesAllowedRbacTest.class.getSimpleName() + ".war")
                .setWebXML(RolesAllowedRbacTest.class.getClassLoader().getResource("activate-roles.web.xml"))
                .addClass(SecuredJaxRsEndpoint.class)
                .addClass(UnsecuredJaxRsEndpoint.class)
                .addClass(JwtRbacTestEndpoint.class)
                .addClass(JaxRsTestApplication.class)
                .addAsManifestResource(RolesAllowedRbacTest.class.getClassLoader().getResource("mp-config-basic.properties"),
                        "microprofile-config.properties")
                .addAsManifestResource(RolesAllowedRbacTest.class.getClassLoader().getResource("pki/key.public.pem"),
                        "key.public.pem");
    }

    /**
     * @tpTestDetails Provide a JWT which has {@code groups} claim set to "monitor" to the server and access an endpoint
     *                which is configured to be accessible only for "monitor" role.
     * @tpPassCrit The {@code groups} claim is mapped to application server roles and endpoint returns expected
     *             response with "monitor" role name.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void monitorAccessMonitorPath(@ArquillianResource URL url) {
        final JsonWebToken token = new JwtHelper(keyTool, "issuer")
                .generateProperSignedJwt(Collections.singleton(Roles.MONITOR));

        given().header("Authorization", "Bearer " + token.getRawValue())
                .when()
                .get(url.toExternalForm() + "rbac-endpoint/" + Roles.MONITOR)
                .then().body(equalTo(Roles.MONITOR));
    }

    /**
     * @tpTestDetails Provide a JWT which has {@code groups} claim set to "monitor" to the server and access an endpoint
     *                which is configured to be accessible only for "admin" role.
     * @tpPassCrit The {@code groups} claim is mapped to application server roles and user receives a 403/forbidden
     *             response because it hasn't the role needed to access this endpoint
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void monitorAccessAdminPath(@ArquillianResource URL url) {
        final JsonWebToken token = new JwtHelper(keyTool, "issuer")
                .generateProperSignedJwt(Collections.singleton(Roles.MONITOR));

        given().header("Authorization", "Bearer " + token.getRawValue())
                .when()
                .get(url.toExternalForm() + "rbac-endpoint/" + Roles.ADMIN)
                .then()
                .statusCode(403);
    }

    /**
     * @tpTestDetails Provide a JWT which has {@code groups} claim set to empty value to the server and access an
     *                endpoint which is configured to be accessible only for "admin" role.
     * @tpPassCrit The {@code groups} claim is mapped to application server roles and user receives a 403/forbidden
     *             response because it hasn't the role needed to access this endpoint
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void noRoleAccessAdminPath(@ArquillianResource URL url) {
        final JsonWebToken token = new JwtHelper(keyTool, "issuer")
                .generateProperSignedJwt(Collections.emptySet());

        given().header("Authorization", "Bearer " + token.getRawValue())
                .when()
                .get(url.toExternalForm() + "rbac-endpoint/" + Roles.ADMIN)
                .then()
                .statusCode(403);
    }

    /**
     * @tpTestDetails Provide a JWT which has {@code groups} claim set to "monitor" to the server and access an endpoint
     *                which is configured to be accessible for "admin" or "director" role.
     * @tpPassCrit The {@code groups} claim is mapped to application server roles and user receives a 403/forbidden
     *             response because it hasn't the role needed to access this endpoint
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void monitorAccessAdminDirectorPath(@ArquillianResource URL url) {
        final JsonWebToken token = new JwtHelper(keyTool, "issuer")
                .generateProperSignedJwt(Collections.singleton(Roles.MONITOR));

        given().header("Authorization", "Bearer " + token.getRawValue())
                .when()
                .get(url.toExternalForm() + "rbac-endpoint/" + Roles.ADMIN + Roles.DIRECTOR)
                .then()
                .statusCode(403);
    }

    /**
     * @tpTestDetails Provide a JWT which has {@code groups} claim set to "admin" to the server and access an endpoint
     *                which is configured to be accessible for "admin" or "director" role.
     * @tpPassCrit The {@code groups} claim is mapped to application server roles and user receives a response 200/OK
     *             and a body with "admindirector"
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void adminAccessAdminDirectorPath(@ArquillianResource URL url) {
        final JsonWebToken token = new JwtHelper(keyTool, "issuer")
                .generateProperSignedJwt(Collections.singleton(Roles.ADMIN));

        given().header("Authorization", "Bearer " + token.getRawValue())
                .when()
                .get(url.toExternalForm() + "rbac-endpoint/" + Roles.ADMIN + Roles.DIRECTOR)
                .then()
                .body(equalTo(Roles.ADMIN + Roles.DIRECTOR));
    }

    /**
     * @tpTestDetails Provide a JWT which has {@code groups} claim set to "monitor" to the server and access an endpoint
     *                which is configured to be accessible for noone.
     * @tpPassCrit User receives a 403/forbidden.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void monitorAccessDenyAllPath(@ArquillianResource URL url) {
        final JsonWebToken token = new JwtHelper(keyTool, "issuer")
                .generateProperSignedJwt(Collections.singleton(Roles.MONITOR));

        given().header("Authorization", "Bearer " + token.getRawValue())
                .when()
                .get(url.toExternalForm() + "rbac-endpoint/deny-all")
                .then()
                .statusCode(403);
    }

    /**
     * @tpTestDetails Access unsecured endpoint without role.
     * @tpPassCrit Client receives 200/OK and "hello" in response.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void nonAuthenticatedAccessPermitAllPath(@ArquillianResource URL url) {
        get(url.toExternalForm() + "unsecured-endpoint")
                .then()
                .statusCode(200)
                .and()
                .body(equalTo("hello"));
    }

    /**
     * @tpTestDetails Provide a JWT which has {@code groups} claim set to "admin" to the server and access an endpoint
     *                which is configured to be accessible to anyone.
     * @tpPassCrit Client receives 200/OK and "hello" in response.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void adminAccessPermitAllPath(@ArquillianResource URL url) {
        final JsonWebToken token = new JwtHelper(keyTool, "issuer")
                .generateProperSignedJwt(Collections.singleton(Roles.ADMIN));

        given().header("Authorization", "Bearer " + token.getRawValue())
                .when()
                .get(url.toExternalForm() + "unsecured-endpoint")
                .then()
                .body(equalTo("hello"));
    }

    /**
     * @tpTestDetails Non authenticated user accesses a path available only for "admin" or "director" roles.
     * @tpPassCrit Client receives 403/forbidden.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void nonAuthenticatedAccessAdminDirectorPath(@ArquillianResource URL url) {
        get(url.toExternalForm() + "rbac-endpoint/" + Roles.ADMIN + Roles.DIRECTOR)
                .then()
                .statusCode(403);
    }

    /**
     * @tpTestDetails Non authenticated user accesses a path not available to anyone.
     * @tpPassCrit Client receives 403/forbidden.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void nonAuthenticatedAccessDenyAllPath(@ArquillianResource URL url) {
        get(url.toExternalForm() + "rbac-endpoint/deny-all")
                .then()
                .statusCode(403);
    }
}
