package org.jboss.eap.qe.micrometer.util;

import org.jboss.eap.qe.micrometer.container.OpenTelemetryCollectorContainer;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.MicroProfileServerSetupTask;
import org.jboss.eap.qe.ts.common.docker.Docker;

/**
 * Enables/Disables Micrometer extension/subsystem for Arquillian in-container tests
 */
public class MicrometerServerSetup implements MicroProfileServerSetupTask {
    private static boolean dockerAvailable = Docker.isDockerAvailable();
    private OpenTelemetryCollectorContainer otelCollector;

    @Override
    public void setup() throws Exception {
        // if a docker service is available, start the OTel collector container
        if (dockerAvailable) {
            otelCollector = OpenTelemetryCollectorContainer.getInstance();
        }
        // and pass Micrometer the OTel collector endopint URL
        MicrometerServerConfiguration.enableMicrometer(otelCollector.getOtlpHttpEndpoint());
    }

    @Override
    public void tearDown() throws Exception {
        MicrometerServerConfiguration.disableMicrometer();
        // if a docker service is available, stop the OTel collector container
        if (dockerAvailable) {
            otelCollector.stop();
        }
    }
}
