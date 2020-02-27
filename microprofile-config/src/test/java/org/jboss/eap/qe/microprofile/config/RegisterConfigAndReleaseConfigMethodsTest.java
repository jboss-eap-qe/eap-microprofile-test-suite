package org.jboss.eap.qe.microprofile.config;

import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.jboss.eap.qe.microprofile.config.testapp.ResolverConfigSource.PROPERTY_NAME;
import static org.jboss.eap.qe.microprofile.config.testapp.ResolverConfigSource.UPDATED_PROPERTY_VALUE;

import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.eap.qe.microprofile.config.testapp.ResolverConfigSource;
import org.jboss.eap.qe.microprofile.config.testapp.jaxrs.JaxRsTestApplication;
import org.jboss.eap.qe.microprofile.config.testapp.jaxrs.ResolverEndPoint;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test cases for ConfigProviderResolver class from MP Config
 *
 * This test should be stored in TCK.
 * But it is not stored in TCK. This test can be removed from this TS once it will be stored in TCK. See
 * https://github.com/eclipse/microprofile-config/issues/522
 */
@RunWith(Arquillian.class)
@RunAsClient
public class RegisterConfigAndReleaseConfigMethodsTest {

    private final static String DEPLOYMENT_NAME = RegisterConfigAndReleaseConfigMethodsTest.class.getSimpleName();
    private final static String ORIGINAL_PROPERTY_VALUE = "original";

    @Deployment(testable = false)
    public static Archive<?> createCentralDeployment() {
        return ShrinkWrap.create(
                WebArchive.class,
                String.format("%s.war", DEPLOYMENT_NAME))
                .addClasses(
                        JaxRsTestApplication.class,
                        ResolverEndPoint.class,
                        ResolverConfigSource.class)
                .addAsManifestResource(new StringAsset(String.format("%s=%s", PROPERTY_NAME, ORIGINAL_PROPERTY_VALUE)),
                        "microprofile-config.properties")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    /**
     * @tpTestDetails Register a new configuration which include values from a custom configuration resolver
     *                using ConfigProviderResolver#registerConfig method.
     *                Verify that this configuration is set as a default and then release this configuration by calling
     *                ConfigProviderResolver#releaseConfig which will unbind this custom configuration from the application.
     *                Verify that the default configuration is in use again.
     * @tpPassCrit Verifies that the Config objects returns correct values with respect to registering and releasing default
     *             or custom Config object
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void checkRegisterConfigAndReleaseConfigMethods(@ArquillianResource URL baseURL) {
        get(baseURL.toExternalForm() + "resolver/oneConfigSource")
                .then()
                .statusCode(200)
                .body(equalToIgnoringCase(ORIGINAL_PROPERTY_VALUE + UPDATED_PROPERTY_VALUE + ORIGINAL_PROPERTY_VALUE));
    }

    /**
     * @tpTestDetails Register a new configuration which include values from a custom configuration resolver
     *                using ConfigProviderResolver#registerConfig method. This Config contains custom ConfigSource.
     *                Try to register second config with the same custom ConfigSource. Exception is expected.
     *                Read property from first config. Release it.
     *                Read property from second config.
     * @tpPassCrit Verifies exception is thrown during registration of second Config object. Verifies that both Config objects
     *             returns properties.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void checkTwoConfigsWithTheSameConfigSource(@ArquillianResource URL baseURL) {
        get(baseURL.toExternalForm() + "resolver/twoConfigSources")
                .then()
                .statusCode(200)
                .body(equalToIgnoringCase(UPDATED_PROPERTY_VALUE + UPDATED_PROPERTY_VALUE));
    }
}
