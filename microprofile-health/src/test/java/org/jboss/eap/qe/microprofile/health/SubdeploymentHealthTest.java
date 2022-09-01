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
import org.jboss.eap.qe.microprofile.health.tools.HealthUrlProvider;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.ConfigurationException;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.restassured.http.ContentType;

/**
 * Deployment with sub-deployments scenario.
 */
@Ignore("https://issues.redhat.com/browse/WFLY-12835")
@RunWith(Arquillian.class)
public class SubdeploymentHealthTest {

    @Deployment
    public static EnterpriseArchive createDeployment() {

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class,
                SubdeploymentHealthTest.class.getSimpleName() + ".ear");

        ear.addAsModule(ShrinkWrap.create(WebArchive.class, "dep1.war")
                .addClasses(BothHealthCheck.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml"));

        ear.addAsModule(ShrinkWrap.create(JavaArchive.class, "dep2.jar")
                .addClasses(LivenessHealthCheck.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

        ear.setApplicationXML(new StringAsset("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<application xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/application_9.xsd\" version=\"9\">\n"
                + "  <display-name>metrics</display-name>\n"
                + "  <module>\n"
                + "    <web>\n"
                + "      <web-uri>dep1.war</web-uri>\n"
                + "      <context-root>/dep</context-root>\n"
                + "    </web>\n"
                + "  </module>\n"
                + "  <module>\n"
                + "    <java>dep2.jar</java>\n"
                + "  </module>\n"
                + "</application>"));

        return ear;
    }

    /**
     * @tpTestDetails Customer deployment with sub-deployment scenario. HealthChecks from more sub-deployments are
     *                registered and exposed under {@code /health} endpoint.
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
     * @tpTestDetails Customer deployment with sub-deployment scenario. Liveness checks from more sub-deployments are
     *                registered and exposed under {@code /health/live} endpoint.
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
     * @tpTestDetails Customer deployment with sub-deployment scenario. Readiness checsk from more sub-deployments are
     *                registered and exposed under {@code /health/ready} endpoint.
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
