package org.jboss.eap.qe.micrometer;

import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.eap.qe.micrometer.base.MetricResource;
import org.jboss.eap.qe.micrometer.container.OpenTelemetryCollectorContainer;
import org.jboss.eap.qe.micrometer.container.PrometheusMetric;
import org.jboss.eap.qe.micrometer.util.MicrometerServerSetup;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.deployment.ConfigurationUtil;
import org.jboss.eap.qe.ts.common.docker.Docker;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@RunWith(Arquillian.class)
@ServerSetup(MicrometerServerSetup.class) // Enables/Disables Micrometer extension/subsystem for Arquillian in-container tests
public class MicrometerOtelIntegrationTestCase {
    private static boolean dockerAvailable = Docker.isDockerAvailable();

    public static final int REQUEST_COUNT = 5;
    @ArquillianResource
    private URL url;
    @Inject
    private MeterRegistry meterRegistry;

    static final String WEB_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<web-app xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n"
            + "           xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"\n"
            + "         xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-app_4_0.xsd\" \n"
            + "              version=\"4.0\">\n"
            + "    <servlet-mapping>\n"
            + "        <servlet-name>jakarta.ws.rs.core.Application</servlet-name>\n"
            + "        <url-pattern>/*</url-pattern>\n"
            + "    </servlet-mapping>"
            + "</web-app>\n";

    @Deployment
    public static Archive<?> deploy() {
        return dockerAvailable ? ShrinkWrap.create(WebArchive.class, "micrometer-test.war")
                .addClasses(
                        ServerSetupTask.class, MetricResource.class, PrometheusMetric.class)
                .addPackages(false, Docker.class.getPackage())
                .addClasses(MicrometerOtelIntegrationTestCase.class)
                .addAsWebInfResource(new StringAsset(WEB_XML), "web.xml")
                .addAsManifestResource(ConfigurationUtil.BEANS_XML_FILE_LOCATION, "beans.xml")
                : ShrinkWrap.create(WebArchive.class, "empty.war").addManifest();
    }

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

    @Test
    @InSequence(1)
    public void testInjection() {
        Assert.assertNotNull(meterRegistry);
    }

    @Test
    @RunAsClient
    @InSequence(2)
    public void makeRequests() throws URISyntaxException {
        try (Client client = ClientBuilder.newClient()) {
            WebTarget target = client.target(url.toURI());
            for (int i = 0; i < REQUEST_COUNT; i++) {
                target.request().get();
            }
        }
    }

    @Test
    @InSequence(3)
    public void checkCounter() {
        Counter counter = meterRegistry.get("demo_counter").counter();
        Assert.assertEquals(counter.count(), REQUEST_COUNT, 0.0);
    }

    // Request the published metrics from the OpenTelemetry Collector via the configured Prometheus exporter and check
    // a few metrics to verify there existence
    @Test
    @RunAsClient
    @InSequence(4)
    public void getMetrics() throws InterruptedException {
        List<String> metricsToTest = Arrays.asList(
                "demo_counter",
                "demo_timer",
                "memory_used_heap",
                "cpu_available_processors",
                "classloader_loaded_classes_count",
                "cpu_system_load_average",
                "gc_time",
                "thread_count",
                "undertow_bytes_received");

        final List<PrometheusMetric> metrics = OpenTelemetryCollectorContainer.getInstance().fetchMetrics(metricsToTest.get(0));
        metricsToTest.forEach(n -> Assert.assertTrue("Missing metric: " + n,
                metrics.stream().anyMatch(m -> m.getKey().equals(n))));
    }

    @Test
    @RunAsClient
    @InSequence(5)
    public void testJmxMetrics() throws InterruptedException {
        List<String> metricsToTest = Arrays.asList(
                "thread_max_count",
                "classloader_loaded_classes",
                "cpu_system_load_average",
                "cpu_process_cpu_time",
                "classloader_unloaded_classes",
                "classloader_loaded_classes_count",
                "thread_count",
                "thread_daemon_count",
                "cpu_available_processors");
        final List<PrometheusMetric> metrics = OpenTelemetryCollectorContainer.getInstance().fetchMetrics(metricsToTest.get(0));

        metricsToTest.forEach(m -> {
            Assert.assertNotEquals("Metric value should be non-zero: " + m,
                    "0", metrics.stream().filter(e -> e.getKey().equals(m))
                            .findFirst()
                            .orElseThrow()
                            .getValue()); // Add the metrics tags to complete the key
        });
    }
}
