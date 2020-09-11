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
public class HealthTest {

    @Deployment(testable = false)
    public static Archive<?> deployment() {
        return ShrinkWrap.create(WebArchive.class, HealthTest.class.getSimpleName() + ".war")
                .addClasses(BothHealthCheck.class, LivenessHealthCheck.class, ReadinessHealthCheck.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    /**
     * @tpTestDetails Customer scenario where health checks exposed under {@code /health} endpoint contain customer data.
     * @tpPassCrit Overall and the health check status is up and checks contains expected data.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testHealthEndpoint() throws ConfigurationException {
        get(HealthUrlProvider.healthEndpoint()).then()
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

    /**
     * @tpTestDetails Customer scenario where liveness checks exposed under {@code /health/live} endpoint contain customer data.
     * @tpPassCrit Overall and the liveness check status is up and checks contains expected data.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testLivenessEndpoint() throws ConfigurationException {
        get(HealthUrlProvider.liveEndpoint()).then()
                .contentType(ContentType.JSON)
                .header("Content-Type", containsString("application/json"))
                .body("status", is("UP"),
                        "checks", hasSize(2),
                        "checks.status", hasItems("UP"),
                        "checks.name", containsInAnyOrder("both", "live"),
                        "checks.data", hasSize(2),
                        "checks.data[0..1].key", hasItems("value"));
    }

    /**
     * @tpTestDetails Customer scenario where readiness checks exposed under {@code /health/ready} endpoint contain customer
     *                data.
     * @tpPassCrit Overall and the readiness check status is up and checks contains expected data.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testReadinessEndpoint() throws ConfigurationException {
        get(HealthUrlProvider.readyEndpoint()).then()
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
