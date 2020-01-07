package org.jboss.eap.qe.microprofile.health.integration;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;

/**
 * Setup server to provide support for MP FT.
 */
public class MicroProfileFTSetupTask implements ServerSetupTask {
    private static final PathAddress FT_EXTENSION_ADDRESS = PathAddress.pathAddress().append(EXTENSION,
            "org.wildfly.extension.microprofile.fault-tolerance-smallrye");
    private static final PathAddress FT_SUBSYSTEM_ADDRESS = PathAddress.pathAddress().append(SUBSYSTEM,
            "microprofile-fault-tolerance-smallrye");

    @Override
    public void setup(ManagementClient managementClient, String s) throws Exception {
        OnlineManagementClient client = ManagementClientProvider.onlineStandalone(managementClient);
        client.execute(Util.createAddOperation(FT_EXTENSION_ADDRESS)).assertSuccess();
        client.execute(Util.createAddOperation(FT_SUBSYSTEM_ADDRESS)).assertSuccess();
    }

    @Override
    public void tearDown(ManagementClient managementClient, String s) throws Exception {
        OnlineManagementClient client = ManagementClientProvider.onlineStandalone(managementClient);
        client.execute(Util.createRemoveOperation(FT_SUBSYSTEM_ADDRESS)).assertSuccess();
        client.execute(Util.createRemoveOperation(FT_EXTENSION_ADDRESS)).assertSuccess();
    }
}
