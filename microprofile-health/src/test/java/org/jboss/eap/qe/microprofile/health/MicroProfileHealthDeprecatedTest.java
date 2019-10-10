package org.jboss.eap.qe.microprofile.health;

import io.restassured.RestAssured;
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
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.Matchers.*;

@RunAsClient
@RunWith(Arquillian.class)
public class MicroProfileHealthDeprecatedTest {

    @ContainerResource
    ManagementClient managementClient;

    @Deployment(testable = false)
    public static Archive<?> deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "MicroProfileHealthCheckDeprecatedTest.war")
                .addClasses(DeprecatedHealthCheck.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return war;
    }

    @Test
    public void testDeprecatedHealthCheck() {
        final String healthURL = "http://" + managementClient.getMgmtAddress() + ":" + managementClient.getMgmtPort() + "/health";

        RestAssured.when().get(healthURL).then()
                .contentType(ContentType.JSON)
                .header("Content-Type", containsString("application/json"))
                .body("status", is("UP"),
                        "checks.status", containsInAnyOrder("UP"),
                        "checks.name", containsInAnyOrder("deprecated-health"));

        RestAssured.when().get(healthURL + "/live").then()
                .contentType(ContentType.JSON)
                .header("Content-Type", containsString("application/json"))
                .body("status", is("UP"),
                        "checks.status", is(empty()),
                        "checks.name", is(empty()));

        RestAssured.when().get(healthURL + "/ready").then()
                .contentType(ContentType.JSON)
                .header("Content-Type", containsString("application/json"))
                .body("status", is("UP"),
                        "checks.status", is(empty()),
                        "checks.name", is(empty()));
    }

}
