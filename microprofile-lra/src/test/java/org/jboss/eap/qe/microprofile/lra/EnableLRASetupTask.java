package org.jboss.eap.qe.microprofile.lra;

import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.MicroProfileServerSetupTask;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.operations.Address;
import org.wildfly.extras.creaper.core.online.operations.Operations;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;

/**
 * Enable LRA coordinator and participant extensions and subsystems.
 */
public class EnableLRASetupTask implements MicroProfileServerSetupTask {

    private static final String EXTENSION_LRA_PARTICIPANT = "org.wildfly.extension.microprofile.lra-participant";
    private static final String EXTENSION_LRA_COORDINATOR = "org.wildfly.extension.microprofile.lra-coordinator";
    private static final String SUBSYSTEM_LRA_PARTICIPANT = "microprofile-lra-participant";
    private static final String SUBSYSTEM_LRA_COORDINATOR = "microprofile-lra-coordinator";

    @Override
    public void setup() throws Exception {
        try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
            final Operations ops = new Operations(client);
            if (!ops.exists(Address.extension(EXTENSION_LRA_COORDINATOR))) {
                client.execute(String.format("/extension=%s:add", EXTENSION_LRA_COORDINATOR))
                        .assertSuccess();
            }
            if (!ops.exists(Address.subsystem(SUBSYSTEM_LRA_COORDINATOR))) {
                client.execute(String.format("/subsystem=%s:add", SUBSYSTEM_LRA_COORDINATOR))
                        .assertSuccess();
            }
            if (!ops.exists(Address.extension(EXTENSION_LRA_PARTICIPANT))) {
                client.execute(String.format("/extension=%s:add", EXTENSION_LRA_PARTICIPANT))
                        .assertSuccess();
            }
            if (!ops.exists(Address.subsystem(SUBSYSTEM_LRA_PARTICIPANT))) {
                client.execute(String.format("/subsystem=%s:add", SUBSYSTEM_LRA_PARTICIPANT))
                        .assertSuccess();
            }
            new Administration(client).reload();
        }
    }

    @Override
    public void tearDown() throws Exception {
        try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
            final Operations ops = new Operations(client);
            if (ops.exists(Address.subsystem(SUBSYSTEM_LRA_PARTICIPANT))) {
                client.execute(String.format("/subsystem=%s:remove", SUBSYSTEM_LRA_PARTICIPANT))
                        .assertSuccess();
            }
            if (ops.exists(Address.extension(EXTENSION_LRA_PARTICIPANT))) {
                client.execute(String.format("/extension=%s:remove", EXTENSION_LRA_PARTICIPANT))
                        .assertSuccess();
            }
            if (ops.exists(Address.subsystem(SUBSYSTEM_LRA_COORDINATOR))) {
                client.execute(String.format("/subsystem=%s:remove", SUBSYSTEM_LRA_COORDINATOR))
                        .assertSuccess();
            }
            if (ops.exists(Address.extension(EXTENSION_LRA_COORDINATOR))) {
                client.execute(String.format("/extension=%s:remove", EXTENSION_LRA_COORDINATOR))
                        .assertSuccess();
            }
            new Administration(client).reload();
        }
    }
}
