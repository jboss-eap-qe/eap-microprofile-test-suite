package org.jboss.eap.qe.microprofile.health;

import static io.restassured.RestAssured.get;
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
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.restassured.http.ContentType;

/**
 * Multiple deployment scenario.
 */
@RunWith(Arquillian.class)
@ServerSetup({ DisableDefaultHealthProceduresSetupTask.class })
public class MultiDeploymentHealthTest {

    @Deployment(name = "deployment1", order = 1, testable = false)
    public static Archive<?> deployment1() {
        return ShrinkWrap.create(WebArchive.class, MultiDeploymentHealthTest.class.getSimpleName() + "-1.war")
                .addClasses(BothHealthCheck.class)
                .addAsManifestResource(new StringAsset("mp.health.disable-default-procedures=true"),
                        "microprofile-config.properties")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Deployment(name = "deployment2", order = 2, testable = false)
    public static Archive<?> deployment2() {
        return ShrinkWrap.create(WebArchive.class, MultiDeploymentHealthTest.class.getSimpleName() + "-2.war")
                .addClasses(LivenessHealthCheck.class)
                .addAsManifestResource(new StringAsset("mp.health.disable-default-procedures=true"),
                        "microprofile-config.properties")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    /**
     * @tpTestDetails Customer multi-deployment scenario. HealthChecks from more deployments are registered and exposed
     *                under {@code /health} endpoint.
     * @tpPassCrit Overall and the health check status is up.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @RunAsClient
    public void testHealthEndpoint() throws ConfigurationException {
        get(HealthUrlProvider.healthEndpoint()).then()
                .contentType(ContentType.JSON)
                .header("Content-Type", containsString("application/json"))
                .body("status", is("UP"),
                        "checks", hasSize(3),
                        "checks.status", hasItems("UP"),
                        "checks.status", not(hasItems("DOWN")));
    }

    /**
     * @tpTestDetails Customer multi-deployment scenario. Liveness checks from more deployments are registered and exposed
     *                under {@code /health/live} endpoint.
     * @tpPassCrit Overall and the health check status is up.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @RunAsClient
    public void testLivenessEndpoint() throws ConfigurationException {
        get(HealthUrlProvider.liveEndpoint()).then()
                .contentType(ContentType.JSON)
                .header("Content-Type", containsString("application/json"))
                .body("status", is("UP"),
                        "checks", hasSize(2),
                        "checks.status", hasItems("UP"));
    }

    /**
     * @tpTestDetails Customer multi-deployment scenario. Readiness checks from more deployments are registered and exposed
     *                under {@code /health/ready} endpoint.
     * @tpPassCrit Overall and the health check status is up.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @RunAsClient
    public void testReadinessEndpoint() throws ConfigurationException {
        get(HealthUrlProvider.readyEndpoint()).then()
                .contentType(ContentType.JSON)
                .header("Content-Type", containsString("application/json"))
                .body("status", is("UP"),
                        "checks", hasSize(1),
                        "checks.status", hasItems("UP"));
    }

}
