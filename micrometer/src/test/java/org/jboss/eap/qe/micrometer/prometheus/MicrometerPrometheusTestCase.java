package org.jboss.eap.qe.micrometer.prometheus;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClients;
import org.hamcrest.MatcherAssert;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.eap.qe.microprofile.common.setuptasks.MicrometerServerConfiguration;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.ArquillianContainerProperties;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.ArquillianDescriptorWrapper;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClientEngine;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.operations.Address;
import org.wildfly.extras.creaper.core.online.operations.Operations;

@RunWith(Arquillian.class)
public class MicrometerPrometheusTestCase {
    private static OnlineManagementClient client = null;
    ArquillianContainerProperties server = new ArquillianContainerProperties(
            ArquillianDescriptorWrapper.getArquillianDescriptor());

    @BeforeClass
    public static void enableMicrometer() throws Exception {
        client = ManagementClientProvider.onlineStandalone();
        MicrometerServerConfiguration.enableMicrometer(client, null, true);
        MicrometerPrometheusSetup.enable(client);
        Thread.sleep(1000);
    }

    @AfterClass
    public static void disableMicrometer() throws Exception {
        try {
            MicrometerServerConfiguration.disableMicrometer(client, true);
            MicrometerPrometheusSetup.disable(client);
        } finally {
            client.close();
        }
    }

    @Test
    public void basicPrometheusTest() throws Exception {
        String response = fetchPrometheusMetricsRequireStatusCode(false, 200);
        MatcherAssert.assertThat(response, containsString("jvm_uptime_seconds "));
        MatcherAssert.assertThat(response, containsString("cpu_available_processors "));
    }

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

    private String fetchPrometheusMetricsRequireStatusCode(boolean authenticate, int requiredStatusCode) throws Exception {
        String url = "http://" + server.getDefaultManagementAddress() + ":" + server.getDefaultManagementPort()
                + MicrometerPrometheusSetup.getPrometheusContext();
        try {
            Client client;
            if (authenticate) {
                CredentialsProvider credentials = new BasicCredentialsProvider();
                credentials.setCredentials(AuthScope.ANY,
                        new UsernamePasswordCredentials(MgmtUsersSetup.USER_NAME, MgmtUsersSetup.PASSWORD));
                client = ((ResteasyClientBuilder) ClientBuilder.newBuilder())
                        .httpEngine(ApacheHttpClientEngine
                                .create(HttpClients.custom().setDefaultCredentialsProvider(credentials).build()))
                        .build();
            } else {
                client = ClientBuilder.newClient();
            }
            try (Response response = client.target(url).request().get()) {
                MatcherAssert.assertThat("Unexpected status of HTTP response", response.getStatus(), equalTo(requiredStatusCode));
                return response.readEntity(String.class);
            }
        } finally {
            client.close();
        }
    }
}
