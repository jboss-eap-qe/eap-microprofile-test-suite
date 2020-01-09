package org.jboss.eap.qe.microprofile.jwt.security.publickeylocation;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.eap.qe.microprofile.jwt.auth.tool.JsonWebToken;
import org.jboss.eap.qe.microprofile.jwt.auth.tool.JwtHelper;
import org.jboss.eap.qe.microprofile.jwt.auth.tool.RsaKeyTool;
import org.jboss.eap.qe.microprofile.jwt.testapp.Endpoints;
import org.jboss.eap.qe.microprofile.jwt.testapp.jaxrs.JaxRsTestApplication;
import org.jboss.eap.qe.microprofile.jwt.testapp.jaxrs.SecuredJaxRsEndpoint;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.undertow.Undertow;
import io.undertow.util.Headers;

/**
 * Set of tests verifying functionality of {@code mp.jwt.verify.publickey.location} property.
 */
@RunAsClient
@RunWith(Arquillian.class)
public class PublicKeyLocationPropertyTestCase {

    private static final String DEPLOYMENT_KEY_FROM_HTTP = "deployment-obtaining-key-from-http";
    private static final String DEPLOYMENT_KEY_FROM_HTTP_INVALID = "deployment-obtaining-key-from-http-invalid";

    private static final String DEPLOYMENT_KEY_FROM_FILE = "deployment-obtaining-key-from-file";
    private static final String DEPLOYMENT_KEY_FROM_FILE_INVALID = "deployment-obtaining-key-from-file-invalid";

    private static Undertow server;

    private static RsaKeyTool keyTool;

    @BeforeClass
    public static void beforeClass() throws URISyntaxException {
        keyTool = RsaKeyTool.newKeyTool(getFileFromResources("pki/key.private.pkcs8.pem"));

        server = Undertow.builder()
                .addHttpListener(8123, "localhost")
                .setHandler(exchange -> {
                    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                    exchange.getResponseSender().send(readLineByLine(Paths.get(getFileFromResources("pki/key.public.pem"))));
                })
                .build();
        server.start();
    }

    @AfterClass
    public static void afterClass() {
        server.stop();
    }

    @Deployment(name = DEPLOYMENT_KEY_FROM_HTTP)
    public static WebArchive createDeploymentObtainingKeyFromHttp() {
        return initDeployment("http://localhost:8123/",
                DEPLOYMENT_KEY_FROM_HTTP + PublicKeyLocationPropertyTestCase.class.getSimpleName() + ".war");
    }

    @Deployment(name = DEPLOYMENT_KEY_FROM_HTTP_INVALID)
    public static WebArchive createDeploymentObtainingKeyFromHttpInvalidUrl() {
        return initDeployment("http://localhost:1111/",
                DEPLOYMENT_KEY_FROM_HTTP_INVALID + PublicKeyLocationPropertyTestCase.class.getSimpleName() + ".war");
    }

    @Deployment(name = DEPLOYMENT_KEY_FROM_FILE)
    public static WebArchive createDeploymentObtainingKeyFromFile() throws URISyntaxException, MalformedURLException {
        return initDeployment(getFileFromResources("pki/key.public.pem").toURL().toExternalForm(),
                DEPLOYMENT_KEY_FROM_FILE + PublicKeyLocationPropertyTestCase.class.getSimpleName() + ".war");
    }

    @Deployment(name = DEPLOYMENT_KEY_FROM_FILE_INVALID)
    public static WebArchive createDeploymentObtainingKeyFromFileInvalidUrl() {
        return initDeployment("file://foobarqux/key-which-doesnt-exist.pem",
                DEPLOYMENT_KEY_FROM_FILE_INVALID + PublicKeyLocationPropertyTestCase.class.getSimpleName() + ".war");
    }

    private static WebArchive initDeployment(final String url, final String name) {
        final String mpProperties = "mp.jwt.verify.publickey.location=" + url + "\n" +
                "mp.jwt.verify.issuer=issuer";

        return ShrinkWrap.create(WebArchive.class, name)
                .addClass(JaxRsTestApplication.class)
                .addClass(SecuredJaxRsEndpoint.class)
                .addAsManifestResource(new StringAsset(mpProperties), "microprofile-config.properties");
    }

    /**
     * @tpTestDetails Send a request to server with a proper, signed JWT. The server has configured public key location
     *                aiming at HTTP location.
     * @tpPassCrit Server successfully obtains key from HTTP source and client receives raw token value in response.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testPublicKeyObtainedFromHttp(@ArquillianResource @OperateOnDeployment(DEPLOYMENT_KEY_FROM_HTTP) URL url) {
        final JsonWebToken token = new JwtHelper(keyTool, "issuer").generateProperSignedJwt();

        given().header("Authorization", "Bearer " + token.getRawValue())
                .when().get(url.toExternalForm() + Endpoints.SECURED_ENDPOINT)
                .then()
                .body(equalTo(token.getRawValue()));
    }

    /**
     * @tpTestDetails Send a request to server with a proper, signed JWT. The server has configured public key location
     *                aiming at HTTP location which doesn't exist.
     * @tpPassCrit Server fails to obtain key from HTTP source and client receives 401/unauthorized.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testPublicKeyObtainedFromHttpInvalidUrl(
            @ArquillianResource @OperateOnDeployment(DEPLOYMENT_KEY_FROM_HTTP_INVALID) URL url) {
        final JsonWebToken token = new JwtHelper(keyTool, "issuer").generateProperSignedJwt();

        given().header("Authorization", "Bearer " + token.getRawValue())
                .when().get(url.toExternalForm() + Endpoints.SECURED_ENDPOINT)
                .then()
                .statusCode(401);
    }

    /**
     * @tpTestDetails Send a request to server with a proper, signed JWT. The server has configured public key location
     *                aiming at {@code file} location.
     * @tpPassCrit Server successfully obtains key from file source and client receives raw token value in response.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testPublicKeyObtainedFromFile(@ArquillianResource @OperateOnDeployment(DEPLOYMENT_KEY_FROM_FILE) URL url) {
        final JsonWebToken token = new JwtHelper(keyTool, "issuer").generateProperSignedJwt();

        given().header("Authorization", "Bearer " + token.getRawValue())
                .when().get(url.toExternalForm() + Endpoints.SECURED_ENDPOINT)
                .then()
                .body(equalTo(token.getRawValue()));
    }

    /**
     * @tpTestDetails Send a request to server with a proper, signed JWT. The server has configured public key location
     *                aiming at {@code file} location which doesn't exist.
     * @tpPassCrit Server fails to obtain key from file source and client receives 401/unauthorized.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testPublicKeyObtainedFromFileInvalidUrl(
            @ArquillianResource @OperateOnDeployment(DEPLOYMENT_KEY_FROM_FILE_INVALID) URL url) {
        final JsonWebToken token = new JwtHelper(keyTool, "issuer").generateProperSignedJwt();

        given().header("Authorization", "Bearer " + token.getRawValue())
                .when().get(url.toExternalForm() + Endpoints.SECURED_ENDPOINT)
                .then()
                .statusCode(401);
    }

    private static URI getFileFromResources(final String filePath) throws URISyntaxException {
        final URL privateKeyUrl = PublicKeyLocationPropertyTestCase.class.getClassLoader().getResource(filePath);
        if (privateKeyUrl == null) {
            throw new IllegalStateException("File wasn't found in resources!");
        }
        return privateKeyUrl.toURI();
    }

    private static String readLineByLine(Path filePath) throws IOException {
        final StringBuilder contentBuilder = new StringBuilder();

        try (final Stream<String> stream = Files.lines(filePath, StandardCharsets.UTF_8)) {
            stream.forEach(s -> contentBuilder.append(s).append("\n"));
        }

        return contentBuilder.toString();
    }

}
