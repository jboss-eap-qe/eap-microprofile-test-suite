package org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;

/**
 * Defines the contract needed to implement custom {@code setup} {@code tearDown} methods for server configuration using
 * Arquillian provided lifecycle but ignoring the injected {@code managementClient} and {@code containerId} parameters
 * which are injected in {@link ServerSetupTask}.
 */
public interface MicroProfileServerSetupTask extends ServerSetupTask {
    @Override
    default public void setup(ManagementClient managementClient, String containerId) throws Exception {
        setup();
    }

    void setup() throws Exception;

    @Override
    default public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        tearDown();
    }

    void tearDown() throws Exception;
}
