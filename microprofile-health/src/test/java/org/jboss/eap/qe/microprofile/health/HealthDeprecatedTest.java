package org.jboss.eap.qe.microprofile.health;

import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

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

@RunAsClient
@RunWith(Arquillian.class)
@ServerSetup({ DisableDefaultHealthProceduresSetupTask.class })
public class HealthDeprecatedTest {

    @Deployment(testable = false)
    public static Archive<?> deployment() {
        return ShrinkWrap.create(WebArchive.class, HealthDeprecatedTest.class.getSimpleName() + ".war")
                .addClasses(DeprecatedHealthCheck.class)
                .addAsManifestResource(new StringAsset("mp.health.disable-default-procedures=true"),
                        "microprofile-config.properties")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    /**
     * @tpTestDetails backward compatibility scenario - test for deprecated however still supported annotation {@code @Health}.
     * @tpPassCrit Overall and the health check status is up.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testDeprecatedHealthAnnotation() throws ConfigurationException {

        get(HealthUrlProvider.healthEndpoint()).then()
                .contentType(ContentType.JSON)
                .header("Content-Type", containsString("application/json"))
                .body("status", is("UP"),
                        "checks.status", containsInAnyOrder("UP"),
                        "checks.name", containsInAnyOrder("deprecated-health"));

        get(HealthUrlProvider.liveEndpoint()).then()
                .contentType(ContentType.JSON)
                .header("Content-Type", containsString("application/json"))
                .body("status", is("UP"),
                        "checks.status", is(empty()),
                        "checks.name", is(empty()));

        get(HealthUrlProvider.readyEndpoint()).then()
                .contentType(ContentType.JSON)
                .header("Content-Type", containsString("application/json"))
                .body("status", is("UP"),
                        "checks.status", is(empty()),
                        "checks.name", is(empty()));
    }

}
