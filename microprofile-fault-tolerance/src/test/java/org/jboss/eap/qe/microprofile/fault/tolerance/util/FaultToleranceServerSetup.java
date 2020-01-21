package org.jboss.eap.qe.microprofile.fault.tolerance.util;

import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.MicroProfileServerSetupTask;

/**
 * Enables/Disables fault tolerance extension/subsystem for Arquillian in-container tests
 */
public class FaultToleranceServerSetup implements MicroProfileServerSetupTask {
    @Override
    public void setup() throws Exception {
        MicroProfileFaultToleranceServerConfiguration.enableFaultTolerance();
    }

    @Override
    public void tearDown() throws Exception {
        MicroProfileFaultToleranceServerConfiguration.enableFaultTolerance();
    }
}
