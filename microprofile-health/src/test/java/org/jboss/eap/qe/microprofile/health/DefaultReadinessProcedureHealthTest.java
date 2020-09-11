package org.jboss.eap.qe.microprofile.health;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientHelper.executeCliCommand;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.apache.http.HttpStatus;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.eap.qe.microprofile.health.tools.HealthUrlProvider;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.ConfigurationException;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientRelatedException;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;

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
    private final String CHANGE_H2_DRIVER_NAME_CLI = "/subsystem=datasources/data-source=ExampleDS:write-attribute(name=driver-name, value=%s)";

    @Deployment(testable = false)
    public static Archive<?> deployment() {
        return ShrinkWrap.create(WebArchive.class, ARCHIVE_NAME)
                .addClasses(LivenessHealthCheck.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    /**
     * @tpTestDetails Calls the deprecated {@code health} endpoint to get response from all health check procedures
     * @tpPassCrit Overall health check status is up and five checks are expected to be returned: one {@code liveness}
     *             check - which is defined by the {@link LivenessHealthCheck} annotated class and has "UP" status and data -
     *             and
     *             one {@code readiness} check - which is provided by Wildfly because none was defined in the deployment and the
     *             {@code mp.health.disable-default-procedures} being set to {@code false} by default. This last check must be
     *             conventionally named after the deplyment name - i.e namely: {@code "ready-deployment." + deployment name}
     *             With WFLY-12342 There was also added {@code deployments-status}, {@code boot-errors}, {@code server-state}
     *             checks
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testHealthEndpoint() throws ConfigurationException {
        RestAssured.get(HealthUrlProvider.healthEndpoint()).then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .contentType(ContentType.JSON)
                .body("status", is("UP"),
                        "checks", hasSize(5),
                        "checks.status", hasItems("UP", "UP"),
                        "checks.name",
                        containsInAnyOrder("deployments-status", "boot-errors", "server-state",
                                String.format("ready-deployment.%s", ARCHIVE_NAME), "live"),
                        "checks.data", hasSize(5),
                        "checks.find{it.name == 'live'}.data.key", is("value"));
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
     * @tpPassCrit Overall health check status is up and four check are expected to be returned, i.e the {@code readiness}
     *             check which is provided by Wildfly because none was defined in the deployment and the
     *             {@code mp.health.disable-default-procedures} being set to {@code false} by default. It must be
     *             conventionally named after the deplyment name - i.e namely: {@code "ready-deployment." + deployment name}
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testReadinessEndpoint() throws ConfigurationException {
        String readyDeploymentCheckName = String.format("ready-deployment.%s", ARCHIVE_NAME);
        RestAssured.get(HealthUrlProvider.readyEndpoint()).then()
                .contentType(ContentType.JSON)
                .body("status", is("UP"),
                        "checks", hasSize(4),
                        "checks.status", hasItems("UP"),
                        "checks.name",
                        containsInAnyOrder("boot-errors", "server-state", "deployments-status",
                                readyDeploymentCheckName),
                        "checks.find{it.name == '" + readyDeploymentCheckName + "'}.data", is(nullValue()),
                        "checks.find{it.name == 'boot-errors'}.data", is(nullValue()),
                        "checks.find{it.name == 'deployments-status'}.data", is(notNullValue()),
                        "checks.find{it.name == 'server-state'}.data.value", is("running"));
    }

    /**
     * @tpTestDetails Calls the {@code ready} endpoint to get response from all {@code readiness} procedures. We have
     *                introduced change which switch server to {@code reload-required} change.
     * @tpPassCrit Overall health check status is down and {@code server-state} readiness check is DOWN
     * @tpSince EAP 7.4.0.CD21
     */
    @Test
    public void testServerStateDown() throws ConfigurationException, IOException, ManagementClientRelatedException {
        OnlineManagementClient client = ManagementClientProvider.onlineStandalone();
        try {
            executeCliCommand(client, String.format(CHANGE_H2_DRIVER_NAME_CLI, "wrong_driver_name"));

            RestAssured.get(HealthUrlProvider.readyEndpoint()).then()
                    .contentType(ContentType.JSON)
                    .body("status", is("DOWN"),
                            "checks.find{it.name == 'server-state'}.status", is("DOWN"),
                            "checks.find{it.name == 'deployments-status'}.status", is("UP"),
                            "checks.find{it.name == 'boot-errors'}.status", is("UP"),
                            "checks.find{it.name == 'server-state'}.data.value", is("reload-required"));

        } finally {
            // put back configuration to correct form
            executeCliCommand(client, String.format(CHANGE_H2_DRIVER_NAME_CLI, "h2"));
            client.close();
        }
    }

    /**
     * @tpTestDetails Calls the {@code ready} endpoint to get response from all {@code readiness} procedures. Wrong
     *                configuration change is introduced which emits exception in server start
     * @tpPassCrit Overall health check status is down and {@code boot-errors} readiness check is DOWN
     * @tpSince EAP 7.4.0.CD21
     */
    @Test
    public void testBootErrorsDown() throws ConfigurationException, IOException, ManagementClientRelatedException,
            TimeoutException, InterruptedException {
        OnlineManagementClient client = ManagementClientProvider.onlineStandalone();
        try {
            executeCliCommand(client, String.format(CHANGE_H2_DRIVER_NAME_CLI, "wrong_driver_name"));
            new Administration(client).reloadIfRequired();

            RestAssured.get(HealthUrlProvider.readyEndpoint()).then()
                    .contentType(ContentType.JSON)
                    .body("status", is("DOWN"),
                            "checks.find{it.name == 'boot-errors'}.status", is("DOWN"),
                            "checks.find{it.name == 'server-state'}.status", is("UP"),
                            "checks.find{it.name == 'deployments-status'}.status", is("DOWN"),
                            "checks.find{it.name == 'boot-errors'}.data", is(notNullValue()));
        } finally {
            // put back configuration to correct form
            executeCliCommand(client, String.format(CHANGE_H2_DRIVER_NAME_CLI, "h2"));
            new Administration(client).reloadIfRequired();
            client.close();
        }
    }

    /**
     * @tpTestDetails Calls the {@code ready} endpoint to get response from all {@code readiness} procedures. There is
     *                one STOPPED deployment which cause deployments-status to be DOWN.
     * @tpPassCrit Overall health check status is down and {@code deployments-status} readiness check is DOWN
     * @tpSince EAP 7.4.0.CD21
     */
    @Test
    public void testDeploymentsStatusDown() throws ConfigurationException, IOException, ManagementClientRelatedException {
        OnlineManagementClient client = ManagementClientProvider.onlineStandalone();
        try {
            executeCliCommand(client, "deployment disable " + ARCHIVE_NAME);

            RestAssured.get(HealthUrlProvider.readyEndpoint()).then()
                    .contentType(ContentType.JSON)
                    .body("status", is("DOWN"),
                            "checks.find{it.name == 'deployments-status'}.status", is("DOWN"),
                            "checks.find{it.name == 'server-state'}.status", is("UP"),
                            "checks.find{it.name == 'boot-errors'}.status", is("UP"),
                            "checks.find{it.name == 'deployments-status'}.data", is(notNullValue()));
        } finally {
            executeCliCommand(client, "deployment enable " + ARCHIVE_NAME);
            client.close();
        }
    }
}
