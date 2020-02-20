package org.jboss.eap.qe.microprofile.health;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.eap.qe.microprofile.health.tools.HealthUrlProvider;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.ConfigurationException;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;

/**
 * Tests case to assess the behavior when no readiness health check procedures are registered explicitly by any
 * deployment, so default procedures would provide one.
 * This is since Wildfly 19 beta 2, see: <br>
 * - Default procedures in MicroProfile Health 2.2:
 * https://download.eclipse.org/microprofile/microprofile-health-2.2/microprofile-health-spec.html#_disabling_default_vendor_procedures
 * <br>
 * - Wildfly issue: https://issues.redhat.com/browse/WFLY-12952 <br>
 * - Wildfly PR: https://github.com/wildfly/wildfly/pull/12940
 */
@RunAsClient
@RunWith(Arquillian.class)
public class DefaultReadinessProcedureHealthTest {

    public static final String ARCHIVE_NAME = DefaultReadinessProcedureHealthTest.class.getSimpleName() + ".war";

    @Deployment(testable = false)
    public static Archive<?> deployment() {
        return ShrinkWrap.create(WebArchive.class, ARCHIVE_NAME)
                .addClasses(LivenessHealthCheck.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    /**
     * @tpTestDetails Calls the deprecated {@code health} endpoint to get response from all health check procedures
     * @tpPassCrit Overall health check status is up and two checks are expected to be returned: one {@code liveness}
     *             check - which is defined by the {@link LivenessHealthCheck} annotated class and has "UP" status and data -
     *             and
     *             one {@code readiness} check - which is provided by Wildfly because none was defined in the deployment and the
     *             {@code mp.health.disable-default-procedures} being set to {@code false} by default. This last check must be
     *             conventionally named after the deplyment name - i.e namely: {@code "ready-deployment." + deployment name}
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testHealthEndpoint() throws ConfigurationException {
        RestAssured.get(HealthUrlProvider.healthEndpoint()).then()
                .contentType(ContentType.JSON)
                .body("status", is("UP"),
                        "checks", hasSize(2),
                        "checks.status", hasItems("UP", "UP"),
                        "checks.name", containsInAnyOrder(String.format("ready-deployment.%s", ARCHIVE_NAME), "live"),
                        "checks.data", hasSize(2),
                        "checks.data.key", contains("value"));
    }

    /**
     * @tpTestDetails Calls the {@code live} endpoint to get response from all {@code liveness} procedures
     * @tpPassCrit Overall health check status is up and one check is expected to be returned, i.e the {@code liveness}
     *             check which is defined by the {@link LivenessHealthCheck} annotated class and has "UP" status and data.
     *             So this is to assess that the value of {@code mp.health.disable-default-procedures} is not affecting
     *             {@code liveness} probes at all.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testLivenessEndpoint() throws ConfigurationException {
        RestAssured.get(HealthUrlProvider.liveEndpoint()).then()
                .contentType(ContentType.JSON)
                .body("status", is("UP"),
                        "checks", hasSize(1),
                        "checks.status", hasItems("UP"),
                        "checks.name", contains("live"),
                        "checks.data", hasSize(1),
                        "checks.data[0].key", is("value"));
    }

    /**
     * @tpTestDetails Calls the {@code ready} endpoint to get response from all {@code readiness} procedures
     * @tpPassCrit Overall health check status is up and one check is expected to be returned, i.e the {@code readiness}
     *             check which is provided by Wildfly because none was defined in the deployment and the
     *             {@code mp.health.disable-default-procedures} being set to {@code false} by default. It must be
     *             conventionally named after the deplyment name - i.e namely: {@code "ready-deployment." + deployment name}
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testReadinessEndpoint() throws ConfigurationException {
        RestAssured.get(HealthUrlProvider.readyEndpoint()).then()
                .contentType(ContentType.JSON)
                .body("status", is("UP"),
                        "checks", hasSize(1),
                        "checks.status", hasItems("UP"),
                        "checks.name", contains(String.format("ready-deployment.%s", ARCHIVE_NAME)),
                        "checks.data", contains(nullValue()));
    }
}
