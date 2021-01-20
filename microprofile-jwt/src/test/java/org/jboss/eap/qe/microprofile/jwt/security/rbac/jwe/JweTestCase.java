package org.jboss.eap.qe.microprofile.jwt.security.rbac.jwe;

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
import org.jboss.eap.qe.microprofile.jwt.auth.tool.JweHelper;
import org.jboss.eap.qe.microprofile.jwt.auth.tool.JwtDefaultClaimValues;
import org.jboss.eap.qe.microprofile.jwt.testapp.Endpoints;
import org.jboss.eap.qe.microprofile.jwt.testapp.Roles;
import org.jboss.eap.qe.microprofile.jwt.testapp.jaxrs.JaxRsTestApplication;
import org.jboss.eap.qe.microprofile.jwt.testapp.jaxrs.JwtRbacTestEndpoint;
import org.jboss.eap.qe.microprofile.jwt.testapp.jaxrs.SecuredJaxRsEndpoint;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jose4j.jwe.ContentEncryptionAlgorithmIdentifiers;
import org.jose4j.jwe.JsonWebEncryption;
import org.jose4j.jwe.KeyManagementAlgorithmIdentifiers;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * MP JWT 1.2 introduced the support for decrypting JWT tokens which have been encrypted using RSA-OAEP and
 * A256GCM algorithms and contain the claims or inner-signed JWT tokens (see
 * https://github.com/eclipse/microprofile-jwt-auth/issues/58)
 * this test verifies that:
 *
 * <ul>
 * <li>JWE (JSON Web Encryption) token are correctly used for granting/denying authentication and authorization</li>
 * <li>missing headers "alg" or "enc", correctly leads to denying authentication</li>
 * <li>missing roles in roles claim, correctly leads to denying authorization</li>
 * </ul>
 *
 * both RS256 and ES256 signed JWT tokens cookies are tested
 *
 * @author tborgato
 */
@RunAsClient
@RunWith(Arquillian.class)
@ServerSetup(EnableJwtSubsystemSetupTask.class)
public class JweTestCase {

    private static final String RS256_DEPLOYMENT = "JweTestCase-first-deployment";
    private static final String ES256_DEPLOYMENT = "JweTestCase-second-deployment";

    @Deployment(name = RS256_DEPLOYMENT)
    public static WebArchive createDeploymentRS256() {
        return ShrinkWrap.create(WebArchive.class, RS256_DEPLOYMENT + ".war")
                .setWebXML(JweTestCase.class.getClassLoader().getResource("activate-roles.web.xml"))
                .addClass(SecuredJaxRsEndpoint.class)
                .addClass(JaxRsTestApplication.class)
                .addClass(JwtRbacTestEndpoint.class)
                .addAsManifestResource(JweTestCase.class.getClassLoader().getResource("mp-config-jwe-RS256.properties"),
                        "microprofile-config.properties")
                .addAsManifestResource(JweTestCase.class.getClassLoader().getResource("pki/RS256/key.public.pem"),
                        "key.public.pem")
                .addAsManifestResource(JweTestCase.class.getClassLoader().getResource("pki/RSA-OAEP/key.private.pkcs8.pem"),
                        "key.private.pkcs8.pem");
    }

    @Deployment(name = ES256_DEPLOYMENT)
    public static WebArchive createDeploymentES256() {
        return ShrinkWrap.create(WebArchive.class, ES256_DEPLOYMENT + ".war")
                .setWebXML(JweTestCase.class.getClassLoader().getResource("activate-roles.web.xml"))
                .addClass(SecuredJaxRsEndpoint.class)
                .addClass(JaxRsTestApplication.class)
                .addClass(JwtRbacTestEndpoint.class)
                .addAsManifestResource(JweTestCase.class.getClassLoader().getResource("mp-config-jwe-ES256.properties"),
                        "microprofile-config.properties")
                .addAsManifestResource(JweTestCase.class.getClassLoader().getResource("pki/ES256/key.public.pem"),
                        "key.public.pem")
                .addAsManifestResource(JweTestCase.class.getClassLoader().getResource("pki/RSA-OAEP/key.private.pkcs8.pem"),
                        "key.private.pkcs8.pem");
    }

    /**
     * @tpTestDetails Test invokes a JAX-RS endpoint which expects authentication and authorization provided by a JWE token
     *                passing a valid JWE token containing an RS encrypted JWT
     * @tpPassCrit the JAX-RS endpoint grants authentication/authorization
     * @tpSince EAP 7.4.0.CD23
     */
    @Test
    public void supplyJweWithNestedRsaJwsToServerTest(@ArquillianResource @OperateOnDeployment(RS256_DEPLOYMENT) URL url)
            throws Exception {
        final URL jswPrivateKeyUrl = JweTestCase.class.getClassLoader().getResource("pki/RS256/key.private.pkcs8.pem");
        if (jswPrivateKeyUrl == null) {
            throw new IllegalStateException("JWS Private key wasn't found in resources!");
        }
        final URL jwePublicKeyUrl = JweTestCase.class.getClassLoader().getResource("pki/RSA-OAEP/key.public.pem");
        if (jwePublicKeyUrl == null) {
            throw new IllegalStateException("JWE Public key wasn't found in resources!");
        }

        JweHelper jweHelper = new JweHelper();
        JsonWebEncryption jsonWebEncryption = jweHelper.generateJwe(
                AlgorithmIdentifiers.RSA_USING_SHA256,
                jswPrivateKeyUrl.toURI(),
                "k1",
                JwtDefaultClaimValues.ISSUER,
                JwtDefaultClaimValues.AUDIENCE,
                JwtDefaultClaimValues.SUBJECT,
                Arrays.asList(Roles.MONITOR),
                Collections.singletonMap("custom_claim", "custom_claim_value"),
                KeyManagementAlgorithmIdentifiers.RSA_OAEP, // RSA-OAEP
                ContentEncryptionAlgorithmIdentifiers.AES_256_GCM,
                jwePublicKeyUrl.toURI(),
                "k2");
        String jwe = jsonWebEncryption.getCompactSerialization();

        given().header("Authorization", "Bearer " + jwe)
                .when().get(url.toExternalForm() + Endpoints.RBAC_ENDPOINT + "/" + Roles.MONITOR)
                .then()
                .statusCode(200);
    }

    /**
     * @tpTestDetails Test invokes a JAX-RS endpoint which expects authentication and authorization provided by a JWE token
     *                passing a valid JWE token containing an RS encrypted JWT missing the required role for that endpoint
     * @tpPassCrit the JAX-RS endpoint denies authorization
     * @tpSince EAP 7.4.0.CD23
     */
    @Test
    public void supplyJweWithNestedRsaJwsWithMissingRoleToServerTest(
            @ArquillianResource @OperateOnDeployment(RS256_DEPLOYMENT) URL url)
            throws Exception {
        final URL jswPrivateKeyUrl = JweTestCase.class.getClassLoader().getResource("pki/RS256/key.private.pkcs8.pem");
        if (jswPrivateKeyUrl == null) {
            throw new IllegalStateException("JWS Private key wasn't found in resources!");
        }
        final URL jwePublicKeyUrl = JweTestCase.class.getClassLoader().getResource("pki/RSA-OAEP/key.public.pem");
        if (jwePublicKeyUrl == null) {
            throw new IllegalStateException("JWE Public key wasn't found in resources!");
        }

        JweHelper jweHelper = new JweHelper();
        JsonWebEncryption jsonWebEncryption = jweHelper.generateJwe(
                AlgorithmIdentifiers.RSA_USING_SHA256,
                jswPrivateKeyUrl.toURI(),
                "k1",
                JwtDefaultClaimValues.ISSUER,
                JwtDefaultClaimValues.AUDIENCE,
                JwtDefaultClaimValues.SUBJECT,
                Arrays.asList(Roles.MONITOR),
                Collections.singletonMap("custom_claim", "custom_claim_value"),
                KeyManagementAlgorithmIdentifiers.RSA_OAEP, // RSA-OAEP
                ContentEncryptionAlgorithmIdentifiers.AES_256_GCM,
                jwePublicKeyUrl.toURI(),
                "k2");
        String jwe = jsonWebEncryption.getCompactSerialization();

        given().header("Authorization", "Bearer " + jwe)
                .when().get(url.toExternalForm() + Endpoints.RBAC_ENDPOINT + "/" + Roles.DIRECTOR)
                .then()
                .statusCode(403);
    }

    /**
     * The JWT must have the JOSE "alg" and "enc" headers that indicate that the token was encrypted
     * using the RSA-OAEP and A256GCM algorithms when the service endpoint expects encrypted
     * tokens.
     *
     * @tpTestDetails Test invokes a JAX-RS endpoint which expects authentication and authorization provided by a JWE token
     *                passing an invalid JWE token (the token is missing the mandatory "enc" header)
     * @tpPassCrit the JAX-RS endpoint denies authorization
     * @tpSince EAP 7.4.0.CD23
     */
    @Test
    public void supplyJweWithNestedRsaJwsWithoutEncHeaderToServerTest(
            @ArquillianResource @OperateOnDeployment(RS256_DEPLOYMENT) URL url)
            throws Exception {
        final URL jswPrivateKeyUrl = JweTestCase.class.getClassLoader().getResource("pki/RS256/key.private.pkcs8.pem");
        if (jswPrivateKeyUrl == null) {
            throw new IllegalStateException("JWS Private key wasn't found in resources!");
        }
        final URL jwePublicKeyUrl = JweTestCase.class.getClassLoader().getResource("pki/RSA-OAEP/key.public.pem");
        if (jwePublicKeyUrl == null) {
            throw new IllegalStateException("JWE Public key wasn't found in resources!");
        }

        JweHelper jweHelper = new JweHelper();
        String jweWithoutEncHeader = jweHelper.generateJweWithoutEncHeader(
                AlgorithmIdentifiers.RSA_USING_SHA256,
                jswPrivateKeyUrl.toURI(),
                "k1",
                JwtDefaultClaimValues.ISSUER,
                JwtDefaultClaimValues.AUDIENCE,
                JwtDefaultClaimValues.SUBJECT,
                Arrays.asList(Roles.MONITOR),
                Collections.singletonMap("custom_claim", "custom_claim_value"),
                KeyManagementAlgorithmIdentifiers.RSA_OAEP, // RSA-OAEP
                ContentEncryptionAlgorithmIdentifiers.AES_256_GCM,
                jwePublicKeyUrl.toURI(),
                "k2");

        given().header("Authorization", "Bearer " + jweWithoutEncHeader)
                .when().get(url.toExternalForm() + Endpoints.RBAC_ENDPOINT + "/" + Roles.MONITOR)
                .then()
                .statusCode(401);
    }

    /**
     * @tpTestDetails Test invokes a JAX-RS endpoint which expects authentication and authorization provided by a JWE token
     *                passing a valid JWE token containing an ES encrypted JWT
     * @tpPassCrit the JAX-RS endpoint grants authentication/authorization
     * @tpSince EAP 7.4.0.CD23
     */
    @Test
    public void supplyJweWithNestedEsJwsToServerTest(@ArquillianResource @OperateOnDeployment(ES256_DEPLOYMENT) URL url)
            throws Exception {
        final URL jswPrivateKeyUrl = JweTestCase.class.getClassLoader().getResource("pki/ES256/key.private.pkcs8.pem");
        if (jswPrivateKeyUrl == null) {
            throw new IllegalStateException("JWS Private key wasn't found in resources!");
        }
        final URL jwePublicKeyUrl = JweTestCase.class.getClassLoader().getResource("pki/RSA-OAEP/key.public.pem");
        if (jwePublicKeyUrl == null) {
            throw new IllegalStateException("JWE Public key wasn't found in resources!");
        }

        JweHelper jweHelper = new JweHelper();
        JsonWebEncryption jsonWebEncryption = jweHelper.generateJwe(
                AlgorithmIdentifiers.ECDSA_USING_P256_CURVE_AND_SHA256,
                jswPrivateKeyUrl.toURI(),
                "k1",
                JwtDefaultClaimValues.ISSUER,
                JwtDefaultClaimValues.AUDIENCE,
                JwtDefaultClaimValues.SUBJECT,
                Arrays.asList(Roles.MONITOR),
                Collections.singletonMap("custom_claim", "custom_claim_value"),
                KeyManagementAlgorithmIdentifiers.RSA_OAEP, // RSA-OAEP
                ContentEncryptionAlgorithmIdentifiers.AES_256_GCM,
                jwePublicKeyUrl.toURI(),
                "k2");
        String jwe = jsonWebEncryption.getCompactSerialization();

        given().header("Authorization", "Bearer " + jwe)
                .when().get(url.toExternalForm() + Endpoints.RBAC_ENDPOINT + "/" + Roles.MONITOR)
                .then()
                .statusCode(200);
    }

    /**
     * @tpTestDetails Test invokes a JAX-RS endpoint which expects authentication and authorization provided by a JWE token
     *                passing a valid JWE token containing an ES encrypted JWT missing the required role for that endpoint
     * @tpPassCrit the JAX-RS endpoint denies authorization
     * @tpSince EAP 7.4.0.CD23
     */
    @Test
    public void supplyJweWithNestedEsJwsWithMissingRoleToServerTest(
            @ArquillianResource @OperateOnDeployment(ES256_DEPLOYMENT) URL url)
            throws Exception {
        final URL jswPrivateKeyUrl = JweTestCase.class.getClassLoader().getResource("pki/ES256/key.private.pkcs8.pem");
        if (jswPrivateKeyUrl == null) {
            throw new IllegalStateException("JWS Private key wasn't found in resources!");
        }
        final URL jwePublicKeyUrl = JweTestCase.class.getClassLoader().getResource("pki/RSA-OAEP/key.public.pem");
        if (jwePublicKeyUrl == null) {
            throw new IllegalStateException("JWE Public key wasn't found in resources!");
        }

        JweHelper jweHelper = new JweHelper();
        JsonWebEncryption jsonWebEncryption = jweHelper.generateJwe(
                AlgorithmIdentifiers.ECDSA_USING_P256_CURVE_AND_SHA256,
                jswPrivateKeyUrl.toURI(),
                "k1",
                JwtDefaultClaimValues.ISSUER,
                JwtDefaultClaimValues.AUDIENCE,
                JwtDefaultClaimValues.SUBJECT,
                Arrays.asList(Roles.MONITOR),
                Collections.singletonMap("custom_claim", "custom_claim_value"),
                KeyManagementAlgorithmIdentifiers.RSA_OAEP, // RSA-OAEP
                ContentEncryptionAlgorithmIdentifiers.AES_256_GCM,
                jwePublicKeyUrl.toURI(),
                "k2");
        String jwe = jsonWebEncryption.getCompactSerialization();

        given().header("Authorization", "Bearer " + jwe)
                .when().get(url.toExternalForm() + Endpoints.RBAC_ENDPOINT + "/" + Roles.DIRECTOR)
                .then()
                .statusCode(403);
    }

    /**
     * The JWT must have the JOSE "alg" and "enc" headers that indicate that the token was encrypted
     * using the RSA-OAEP and A256GCM algorithms when the service endpoint expects encrypted
     * tokens.
     *
     * @tpTestDetails Test invokes a JAX-RS endpoint which expects authentication and authorization provided by a JWE token
     *                passing an invalid JWE token (the token is missing the mandatory "alg" header)
     * @tpPassCrit the JAX-RS endpoint denies authorization
     * @tpSince EAP 7.4.0.CD23
     */
    @Test
    public void supplyJweWithNestedEsJwsWithoutAlgHeaderToServerTest(
            @ArquillianResource @OperateOnDeployment(ES256_DEPLOYMENT) URL url)
            throws Exception {
        final URL jswPrivateKeyUrl = JweTestCase.class.getClassLoader().getResource("pki/ES256/key.private.pkcs8.pem");
        if (jswPrivateKeyUrl == null) {
            throw new IllegalStateException("JWS Private key wasn't found in resources!");
        }
        final URL jwePublicKeyUrl = JweTestCase.class.getClassLoader().getResource("pki/RSA-OAEP/key.public.pem");
        if (jwePublicKeyUrl == null) {
            throw new IllegalStateException("JWE Public key wasn't found in resources!");
        }

        JweHelper jweHelper = new JweHelper();
        String jweWithoutAlgHeader = jweHelper.generateJweWithoutAlgHeader(
                AlgorithmIdentifiers.ECDSA_USING_P256_CURVE_AND_SHA256,
                jswPrivateKeyUrl.toURI(),
                "k1",
                JwtDefaultClaimValues.ISSUER,
                JwtDefaultClaimValues.AUDIENCE,
                JwtDefaultClaimValues.SUBJECT,
                Arrays.asList(Roles.MONITOR),
                Collections.singletonMap("custom_claim", "custom_claim_value"),
                KeyManagementAlgorithmIdentifiers.RSA_OAEP, // RSA-OAEP
                ContentEncryptionAlgorithmIdentifiers.AES_256_GCM,
                jwePublicKeyUrl.toURI(),
                "k2");

        given().header("Authorization", "Bearer " + jweWithoutAlgHeader)
                .when().get(url.toExternalForm() + Endpoints.RBAC_ENDPOINT + "/" + Roles.MONITOR)
                .then()
                .statusCode(401);
    }
}
