package org.jboss.eap.qe.microprofile.health;

import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
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
public class HealthNullTest {

    @Deployment(testable = false)
    public static Archive<?> deployment() {
        return ShrinkWrap.create(WebArchive.class, HealthNullTest.class.getSimpleName() + ".war")
                .addClasses(NullLivenessHealthCheck.class)
                .addAsManifestResource(new StringAsset("mp.health.disable-default-procedures=true"),
                        "microprofile-config.properties")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    /**
     * @tpTestDetails Negative scenario - health check returns null.
     * @tpPassCrit Overall and the health check status is down.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testNullHealthCheck() throws ConfigurationException {
        get(HealthUrlProvider.healthEndpoint()).then()
                .contentType(ContentType.JSON)
                .header("Content-Type", containsString("application/json"))
                .body("status", is("DOWN"),
                        "checks.status", contains("DOWN"),
                        "checks.name", contains(NullLivenessHealthCheck.class.getCanonicalName()));

        get(HealthUrlProvider.liveEndpoint()).then()
                .contentType(ContentType.JSON)
                .header("Content-Type", containsString("application/json"))
                .body("status", is("DOWN"));
    }
}
