package org.jboss.eap.qe.micrometer.multiple;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;

import org.jboss.eap.qe.micrometer.container.PrometheusMetric;
import org.jboss.eap.qe.ts.common.docker.Docker;
import org.junit.Assert;
import org.junit.BeforeClass;

public abstract class BaseMultipleTestCase {
    protected static final String SERVICE_ONE = "service-one";
    protected static final String SERVICE_TWO = "service-two";
    protected static final int REQUEST_COUNT = 5;

    // The @ServerSetup(MicrometerSetupTask.class) requires Docker to be available.
    // Otherwise the org.wildfly.extension.micrometer.registry.NoOpRegistry is installed which will result in 0 counters,
    // and cause the test fail seemingly intermittently on machines with broken Docker setup.
    @BeforeClass
    public static void checkForDocker() {
        try {
            Docker.checkDockerPresent();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot verify Docker availability: " + e.getMessage());
        }
    }

    protected void makeRequests(URI service) throws URISyntaxException {
        try (Client client = ClientBuilder.newClient()) {
            WebTarget target = client.target(service);
            for (int i = 0; i < REQUEST_COUNT; i++) {
                Assert.assertEquals(200, target.request().get().getStatus());
            }
        }
    }

    protected List<PrometheusMetric> getMetricsByName(List<PrometheusMetric> metrics, String key) {
        return metrics.stream()
                .filter(m -> m.getKey().equals(key))
                .collect(Collectors.toList());
    }
}
