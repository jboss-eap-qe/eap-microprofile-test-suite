package org.jboss.eap.qe.microprofile.health;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.eap.qe.microprofile.health.tools.HealthUrlProvider;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.ConfigurationException;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.restassured.http.ContentType;

@RunAsClient
@RunWith(Arquillian.class)
@ServerSetup({ DisableDefaultHealthProceduresSetupTask.class })
public class MimeTypeHealthTest {

    @Deployment(testable = false)
    public static Archive<?> deployment() {
        return ShrinkWrap.create(WebArchive.class, MimeTypeHealthTest.class.getSimpleName() + ".war")
                .addClasses(BothHealthCheck.class, LivenessHealthCheck.class, ReadinessHealthCheck.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    /**
     * @tpTestDetails Negative scenario where health check contains customer data.
     *                HTTP GET request to {@code /health}, {@code /health/live} and {@code /health/ready} endpoints accepts
     *                invalid MIME type -
     *                application/x-json. MP Health should not care about requested MIME type and always produce JSON.
     * @tpPassCrit Overall and the health check status is up and checks contains expected data.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testHealthXJSON() throws ConfigurationException {
        test("application/x-json");
    }

    /**
     * @tpTestDetails Negative scenario where health check contains customer data.
     *                HTTP GET request to {@code /health}, {@code /health/live} and {@code /health/ready} endpoints accepts
     *                invalid MIME type -
     *                application/json-p. MP Health should not care about requested MIME type and always produce JSON.
     * @tpPassCrit Overall and the health check status is up and checks contains expected data.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testHealthJSONP() throws ConfigurationException {
        test("application/json-p");
    }

    /**
     * @tpTestDetails Negative scenario where health check contains customer data.
     *                HTTP GET request to {@code /health}, {@code /health/live} and {@code /health/ready} endpoints accepts
     *                invalid MIME type -
     *                text/plain. MP Health should not care about requested MIME type and always produce JSON.
     * @tpPassCrit Overall and the health check status is up and checks contains expected data.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testHealthTextPlain() throws ConfigurationException {
        test("text/plain");
    }

    /**
     * @tpTestDetails Negative scenario where health check contains customer data.
     *                HTTP GET request to {@code /health}, {@code /health/live} and {@code /health/ready} endpoints accepts
     *                invalid MIME type -
     *                text/csv. MP Health should not care about requested MIME type and always produce JSON.
     * @tpPassCrit Overall and the health check status is up and checks contains expected data.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testHealthTextCSV() throws ConfigurationException {
        test("text/csv");
    }

    /**
     * @tpTestDetails Negative scenario where health check contains customer data.
     *                HTTP GET request to {@code /health}, {@code /health/live} and {@code /health/ready} endpoints accepts
     *                invalid MIME type -
     *                model/vml. MP Health should not care about requested MIME type and always produce JSON.
     * @tpPassCrit Overall and the health check status is up and checks contains expected data.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testHealthModelVML() throws ConfigurationException {
        test("model/vml");
    }

    /**
     * @tpTestDetails Negative scenario where health check contains customer data.
     *                HTTP GET request to {@code /health}, {@code /health/live} and {@code /health/ready} endpoints accepts
     *                invalid MIME type -
     *                text/json. MP Health should not care about requested MIME type and always produce JSON.
     * @tpPassCrit Overall and the health check status is up and checks contains expected data.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testHealtTextJSON() throws ConfigurationException {
        test("text/json");
    }

    private void test(String mimeType) throws ConfigurationException {
        testHealthEndpoint(mimeType);
        testLiveEndpoint(mimeType);
        testReadyEndpoint(mimeType);
    }

    private void testHealthEndpoint(String mimeType) throws ConfigurationException {
        given()
                .baseUri(HealthUrlProvider.healthEndpoint())
                .accept(mimeType)
                .get()
                .then()
                .contentType(ContentType.JSON)
                .header("Content-Type", containsString("application/json"))
                .body("status", is("UP"),
                        "checks", hasSize(4), // BothHealthCheck contains both annotations: @Liveness and @Readiness
                        "checks.status", hasItems("UP"),
                        "checks.status", not(hasItems("DOWN")),
                        "checks.name", containsInAnyOrder("both", "live", "ready", "both"),
                        "checks.data", hasSize(4),
                        "checks.data[0].key", is("value"),
                        "checks.data[0..3].key", hasItems("value"));
    }

    private void testLiveEndpoint(String mimeType) throws ConfigurationException {
        given()
                .baseUri(HealthUrlProvider.liveEndpoint())
                .accept(mimeType)
                .get()
                .then()
                .contentType(ContentType.JSON)
                .header("Content-Type", containsString("application/json"))
                .body("status", is("UP"),
                        "checks", hasSize(2),
                        "checks.status", hasItems("UP"),
                        "checks.name", containsInAnyOrder("both", "live"),
                        "checks.data", hasSize(2),
                        "checks.data[0..1].key", hasItems("value"));
    }

    private void testReadyEndpoint(String mimeType) throws ConfigurationException {
        given()
                .baseUri(HealthUrlProvider.readyEndpoint())
                .accept(mimeType)
                .get()
                .then()
                .contentType(ContentType.JSON)
                .header("Content-Type", containsString("application/json"))
                .body("status", is("UP"),
                        "checks", hasSize(2),
                        "checks.status", hasItems("UP"),
                        "checks.name", containsInAnyOrder("both", "ready"),
                        "checks.data", hasSize(2),
                        "checks.data[0..1].key", hasItems("value"));
    }
}
