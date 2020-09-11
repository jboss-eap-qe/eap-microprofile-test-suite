package org.jboss.eap.qe.microprofile.health;

import static io.restassured.RestAssured.get;
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
public class SimplifiedHealthTest {

    @Deployment(testable = false)
    public static Archive<?> deployment() {
        return ShrinkWrap.create(WebArchive.class, SimplifiedHealthTest.class + ".war")
                .addClasses(SimplifiedHealthCheck.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    /**
     * @tpTestDetails Integration test for MP Health 2.1 health check on {@code /health} endpoint.
     * @tpPassCrit Overall and the health check status is up.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testHealthEndpoint() throws ConfigurationException {
        get(HealthUrlProvider.healthEndpoint()).then()
                .contentType(ContentType.JSON)
                .header("Content-Type", containsString("application/json"))
                .body("status", is("UP"),
                        "checks", hasSize(2), // SimplifiedHealthCheck contains both annotations: @Liveness and @Readiness
                        "checks.status", hasItems("UP"),
                        "checks.status", not(hasItems("DOWN")),
                        "checks.name", containsInAnyOrder("simplified", "simplified"));
    }

    /**
     * @tpTestDetails Integration test for MP Health 2.1 on {@code /health/live} endpoint.
     * @tpPassCrit Overall and the health check status is up.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testLivenessEndpoint() throws ConfigurationException {
        get(HealthUrlProvider.liveEndpoint()).then()
                .contentType(ContentType.JSON)
                .header("Content-Type", containsString("application/json"))
                .body("status", is("UP"),
                        "checks", hasSize(1),
                        "checks.status", hasItems("UP"),
                        "checks.name", containsInAnyOrder("simplified"));
    }

    /**
     * @tpTestDetails Integration test for MP Health 2.1 on {@code /health/ready} endpoint.
     * @tpPassCrit Overall and the health check status is up.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testReadinessEndpoint() throws ConfigurationException {
        get(HealthUrlProvider.readyEndpoint()).then()
                .contentType(ContentType.JSON)
                .header("Content-Type", containsString("application/json"))
                .body("status", is("UP"),
                        "checks", hasSize(1),
                        "checks.status", hasItems("UP"),
                        "checks.name", containsInAnyOrder("simplified"));
    }
}
