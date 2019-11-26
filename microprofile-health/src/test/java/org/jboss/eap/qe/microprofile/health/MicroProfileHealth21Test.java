package org.jboss.eap.qe.microprofile.health;

import io.restassured.http.ContentType;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

@RunAsClient
@RunWith(Arquillian.class)
public class MicroProfileHealth21Test {

    @ContainerResource
    ManagementClient managementClient;

    String healthURL;
    @Before
    public void composeHealthEndpointURL() {
        healthURL = "http://" + managementClient.getMgmtAddress() + ":" + managementClient.getMgmtPort() + "/health";
    }

    @Deployment(testable = false)
    public static Archive<?> deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "MicroProfileHealth21Test.war")
                .addClasses(DeprecatedHealthCheck.class, BothHealthCheck.class, LivenessHealthCheck.class, ReadinessHealthCheck.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return war;
    }

    @Test
    public void testHealthEndpoint() {
        get(healthURL).then()
                .contentType(ContentType.JSON)
                .header("Content-Type", containsString("application/json"))
                .body("status", is("UP"),
                        "checks", hasSize(5), // BothHealthCheck contains both annotations: @Liveness and @Readiness
                        "checks.status", hasItems("UP"),
                        "checks.status", not(hasItems("DOWN")),
                        "checks.name", containsInAnyOrder("deprecated-health", "both", "live", "ready", "both"),
                        "checks.data", hasSize(5),
                        "checks.data[0].key", is("value"),
                        "checks.data[0..4].key", hasItems("value"));
    }

    @Test
    public void testLivenessEndpoint() {
        get(healthURL + "/live").then()
                .contentType(ContentType.JSON)
                .header("Content-Type", containsString("application/json"))
                .body("status", is("UP"),
                        "checks", hasSize(2),
                        "checks.status", hasItems("UP"),
                        "checks.name", containsInAnyOrder("both", "live"),
                        "checks.data", hasSize(2),
                        "checks.data[0..1].key", hasItems("value"));
    }
    @Test
    public void testReadinessEndpoint() {
        get(healthURL + "/ready").then()
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
