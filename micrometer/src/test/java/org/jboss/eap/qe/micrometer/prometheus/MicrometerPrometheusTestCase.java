package org.jboss.eap.qe.micrometer.prometheus;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClients;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.eap.qe.microprofile.common.setuptasks.MicrometerServerConfiguration;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.ArquillianContainerProperties;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.ArquillianDescriptorWrapper;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClientEngine;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.operations.Address;
import org.wildfly.extras.creaper.core.online.operations.Operations;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;

/**
 * Test for prometheus micrometer registry.
 */
@RunWith(Arquillian.class)
public class MicrometerPrometheusTestCase {
    private static OnlineManagementClient client = null;
    ArquillianContainerProperties server = new ArquillianContainerProperties(
            ArquillianDescriptorWrapper.getArquillianDescriptor());

    private static boolean serverTypeCheck() {
        return System.getProperty("jboss.home").toLowerCase().contains("eap");
    }

    @Before
    public void testServerTypeCheck() {
        Assume.assumeTrue(serverTypeCheck());
    }

    /**
     * Enable micrometer and prometheus registry before first test execution
     */
    @BeforeClass
    public static void enableMicrometer() throws Exception {
        if (!serverTypeCheck()) {
            return;
        }
        client = ManagementClientProvider.onlineStandalone();
        MicrometerServerConfiguration.enableMicrometer(client, null, true);
        MicrometerPrometheusSetup.enable(client);
    }

    /**
     * Disable micrometer and prometheus registry after last test execution
     */
    @AfterClass
    public static void disableMicrometer() throws Exception {
        if (!serverTypeCheck()) {
            return;
        }
        try {
            MicrometerServerConfiguration.disableMicrometer(client, true);
            MicrometerPrometheusSetup.disable(client);
        } finally {
            client.close();
        }
    }

    /**
     * Get prometheus metrics from unsecured end-point, check that JVM/System metrics are in the list
     */
    @Test
    public void basicPrometheusTest() throws Exception {
        String response = fetchPrometheusMetricsRequireStatusCode(false, 200);
        MatcherAssert.assertThat(response, containsString("jvm_uptime_seconds "));
        MatcherAssert.assertThat(response, containsString("cpu_available_processors "));
    }

    /**
     * Map prometheus output to the end-point that WF Metrics is using. Startup-error is expected.
     */
    @Test
    public void wfMetricsEnabledTest() throws Exception {
        try {
            MicrometerPrometheusSetup.set(client, "/metrics", false);
            Assert.assertTrue(
                    "\"/metrics\" endpoint should be used by WF metrics, but MicroMeter allows to expose its own metrics there",
                    !new Operations(client).invoke("read-boot-errors", Address.of("core-service", "management")).get("result")
                            .asList().isEmpty());
        } finally {
            MicrometerPrometheusSetup.set(client, false);
        }
    }

    /**
     * Test disables WF Metrics, configure prometheus to use same end-point that is by default used by WF-metrics. Checks that
     * Micrometer metrics are available.
     */
    @Test
    public void wfMetricsDisabledTest() throws Exception {
        try {
            client.execute("/subsystem=metrics:remove");
            client.execute("/extension=org.wildfly.extension.metrics:remove");
            MicrometerPrometheusSetup.set(client, "/metrics", false);
            String response = fetchPrometheusMetricsRequireStatusCode(false, 200);
            MatcherAssert.assertThat(response, containsString("jvm_uptime_seconds "));
            MatcherAssert.assertThat(response, containsString("cpu_available_processors "));
        } finally {
            client.execute("/extension=org.wildfly.extension.metrics:add");
            client.execute(
                    "/subsystem=metrics:add(exposed-subsystems=[\"*\"], prefix=\"${wildfly.metrics.prefix:jboss}\", security-enabled=false)");
            MicrometerPrometheusSetup.set(client, false);
        }
    }

    /**
     * More security checks are in one test method in order to avoid unnecessary reloads.
     *
     * Keep prometheus end-point unsecure, try to get metrics with authorization. Metrics should be available.
     *
     * Secure end-point, try to get metrics without authorization. Metrics should not be available.
     *
     * Keep end-point secure, try to get metrics with authorization. Metrics should be available.
     */
    @Test
    public void securityTest() throws Exception {
        try {
            // use authnetication, although prometheus is not secured yet
            MgmtUsersSetup.setup();
            String response = fetchPrometheusMetricsRequireStatusCode(true, 200);
            MatcherAssert.assertThat(response, containsString("jvm_uptime_seconds "));
            MatcherAssert.assertThat(response, containsString("cpu_available_processors "));

            // secure prometheus, but do not use authentication
            MicrometerPrometheusSetup.set(client, true);
            response = fetchPrometheusMetricsRequireStatusCode(false, 401);
            MatcherAssert.assertThat(response, containsString("401 - Unauthorized"));

            // keep prometheus secure, use authentication
            response = fetchPrometheusMetricsRequireStatusCode(true, 200);
            MatcherAssert.assertThat(response, containsString("jvm_uptime_seconds "));
            MatcherAssert.assertThat(response, containsString("cpu_available_processors "));
        } finally {
            MgmtUsersSetup.tearDown();
            MicrometerPrometheusSetup.set(client, false);
        }
    }

    /**
     * Check that micrometer shows real values of restricted metrics if proper user with proper RBAC role is used
     */
    @Test
    public void rbacTest() throws Exception {
        try {
            MgmtUsersSetup.setup();
            MicrometerPrometheusSetup.set(client, true);
            // Create the Monitor role mapping and associate the group Monitor with it
            client.execute("/core-service=management/access=authorization/role-mapping=Monitor:add");
            client.execute(
                    "/core-service=management/access=authorization/role-mapping=Monitor/include=group-monitors:add(name=Monitor, type=GROUP)");
            // enable RBAC
            client.execute("/core-service=management/access=authorization:write-attribute(name=provider,value=rbac)");
            new Administration(client).reload();

            // get metrics with user without Monitor RBAC role
            List<String> response = fetchPrometheusMetricsRequireStatusCode(true, 200).lines().collect(Collectors.toList());
            MatcherAssert.assertThat(response, Matchers.hasItem(Matchers.allOf(
                    Matchers.startsWith("io_max_pool_size"),
                    Matchers.endsWith("0.0"))));

            // get metrics with user with Monitor RBAC role
            response = fetchPrometheusMetricsWithRbacRequireStatusCode(200).lines().collect(Collectors.toList());
            MatcherAssert.assertThat(response, Matchers.hasItem(Matchers.allOf(
                    Matchers.startsWith("io_max_pool_size"),
                    Matchers.endsWith("256.0"))));
        } finally {
            client.execute("/core-service=management/access=authorization/role-mapping=Monitor:remove");
            client.execute("/core-service=management/access=authorization:write-attribute(name=provider,value=simple)");
            MgmtUsersSetup.tearDown();
            MicrometerPrometheusSetup.set(client, false);
        }
    }

    private String fetchPrometheusMetricsRequireStatusCode(boolean authenticate, int requiredStatusCode) throws Exception {
        return fetchPrometheusMetricsRequireStatusCode(authenticate, requiredStatusCode, MgmtUsersSetup.USER_NAME,
                MgmtUsersSetup.USERS_PASSWORD);
    }

    private String fetchPrometheusMetricsWithRbacRequireStatusCode(int requiredStatusCode) throws Exception {
        return fetchPrometheusMetricsRequireStatusCode(true, requiredStatusCode, MgmtUsersSetup.RBAC_USER_NAME,
                MgmtUsersSetup.USERS_PASSWORD);
    }

    private String fetchPrometheusMetricsRequireStatusCode(boolean authenticate, int requiredStatusCode, String userName,
            String password) throws Exception {
        String url = "http://" + server.getDefaultManagementAddress() + ":" + server.getDefaultManagementPort()
                + MicrometerPrometheusSetup.getPrometheusContext();
        try {
            Client client;
            if (authenticate) {
                CredentialsProvider credentials = new BasicCredentialsProvider();
                credentials.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(userName, password));
                client = ((ResteasyClientBuilder) ClientBuilder.newBuilder())
                        .httpEngine(ApacheHttpClientEngine
                                .create(HttpClients.custom().setDefaultCredentialsProvider(credentials).build()))
                        .build();
            } else {
                client = ClientBuilder.newClient();
            }
            try (Response response = client.target(url).request().get()) {
                MatcherAssert.assertThat("Unexpected status of HTTP response", response.getStatus(),
                        equalTo(requiredStatusCode));
                return response.readEntity(String.class);
            }
        } finally {
            client.close();
        }
    }
}
