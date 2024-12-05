package org.jboss.eap.qe.microprofile.telemetry.metrics;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.eap.qe.microprofile.common.setuptasks.MicroProfileTelemetryServerConfiguration;
import org.jboss.eap.qe.microprofile.common.setuptasks.MicrometerServerConfiguration;
import org.jboss.eap.qe.microprofile.common.setuptasks.OpenTelemetryServerConfiguration;
import org.jboss.eap.qe.observability.containers.OpenTelemetryCollectorContainer;
import org.jboss.eap.qe.ts.common.docker.Docker;

/**
 * Server setup task for configuration of MicroProfile Telemetry and otel collector
 */
public class MPTelemetryServerSetupTask implements ServerSetupTask {

    private static OpenTelemetryCollectorContainer otelCollector;

    /**
     * Start otel collector in container and configure OpenTelemetry and MP Telemetry in application server
     */
    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        // we need a Docker container for The OTel collector here, so throw an exception if a docker service is not available
        try {
            Docker.checkDockerPresent();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot verify Docker availability: " + e.getMessage());
        }
        // disable micrometer
        MicrometerServerConfiguration.disableMicrometer();
        // start the OTel collector container
        otelCollector = OpenTelemetryCollectorContainer.getInstance();
        otelCollector.start();
        // Enable MP Telemetry based metrics, which rely on OpenTelemetry subsystem
        OpenTelemetryServerConfiguration.enableOpenTelemetry();
        OpenTelemetryServerConfiguration.addOpenTelemetryCollectorConfiguration(otelCollector.getOtlpGrpcEndpoint());
        MicroProfileTelemetryServerConfiguration.enableMicroProfileTelemetry();
    }

    /**
     * Stop otel collector in container and disable OpenTelemetry and MP Telemetry in application server
     */
    @Override
    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        // disable MP Telemetry based metrics
        MicroProfileTelemetryServerConfiguration.disableMicroProfileTelemetry();
        OpenTelemetryServerConfiguration.disableOpenTelemetry();
        // stop the OTel collector container
        otelCollector.stop();
    }
}
