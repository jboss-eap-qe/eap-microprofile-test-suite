package org.jboss.eap.qe.micrometer.util;

import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.MicroProfileServerSetupTask;
import org.jboss.eap.qe.observability.containers.OpenTelemetryCollectorContainer;
import org.jboss.eap.qe.ts.common.docker.Docker;

/**
 * Enables/Disables Micrometer extension/subsystem for Arquillian in-container tests
 */
public class MicrometerServerSetup implements MicroProfileServerSetupTask {
    private OpenTelemetryCollectorContainer otelCollector;

    @Override
    public void setup() throws Exception {
        // if a docker service is not available, throw an exception
        try {
            Docker.checkDockerPresent();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot verify Docker availability: " + e.getMessage());
        }
        // start the OTel collector container
        otelCollector = OpenTelemetryCollectorContainer.getInstance();
        // and pass Micrometer the OTel collector endopint URL
        MicrometerServerConfiguration.enableMicrometer(otelCollector.getOtlpHttpEndpoint());
    }

    @Override
    public void tearDown() throws Exception {
        MicrometerServerConfiguration.disableMicrometer();
        // stop the OTel collector container
        otelCollector.stop();
    }
}
