package org.jboss.eap.qe.microprofile.jwt;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.jboss.eap.qe.microprofile.tooling.server.configuration.ConfigurationException;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.MicroProfileServerSetupTask;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.wildfly.extras.creaper.core.online.CliException;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.operations.Address;
import org.wildfly.extras.creaper.core.online.operations.OperationException;
import org.wildfly.extras.creaper.core.online.operations.Operations;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;

/**
 * A way enable/disable MP-JWT subsystem when not enabled
 */
public class EnableJwtSubsystemSetupTask implements MicroProfileServerSetupTask {

    private static final Address MP_JWT_EXTENSION_ADDRESS = Address
            .extension("org.wildfly.extension.microprofile.jwt-smallrye");
    private static final Address MP_JWT_ADDRESS = Address.subsystem("microprofile-jwt-smallrye");

    private boolean wasExtensionAdded = false;
    private boolean wasSubsystemAdded = false;

    @Override
    public void setup() throws IOException, OperationException, TimeoutException, InterruptedException,
            ConfigurationException {
        try (final OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
            final Operations ops = new Operations(client);
            if (!ops.exists(MP_JWT_EXTENSION_ADDRESS)) {
                ops.add(MP_JWT_EXTENSION_ADDRESS).assertSuccess();
                wasExtensionAdded = true;
            }
            if (!ops.exists(MP_JWT_ADDRESS)) {
                ops.add(MP_JWT_ADDRESS).assertSuccess();
                wasSubsystemAdded = true;
            }
            // let's add DEBUG logging for "io.smallrye.jwt", too
            client.execute("/subsystem=logging/logger=io.smallrye.jwt:add(level=DEBUG)");
            if (wasExtensionAdded || wasSubsystemAdded) {
                new Administration(client).reload();
            }
        } catch (CliException e) {
            throw new IllegalStateException("Unexpected exception when adding MP-JWT subsystem", e);
        }
    }

    @Override
    public void tearDown() {
        try (final OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
            final Operations ops = new Operations(client);
            if (wasExtensionAdded) {
                ops.remove(MP_JWT_EXTENSION_ADDRESS).assertSuccess();
            }
            if (wasSubsystemAdded) {
                ops.remove(MP_JWT_ADDRESS).assertSuccess();
            }
            // let's remove DEBUG logging for "io.smallrye.jwt"
            client.execute("/subsystem=logging/logger=io.smallrye.jwt:remove()");
            if (wasExtensionAdded || wasSubsystemAdded) {
                new Administration(client).reload();
            }
        } catch (IOException | ConfigurationException | InterruptedException | TimeoutException | CliException e) {
            throw new IllegalStateException("Unexpected exception when removing MP-JWT subsystem", e);
        }
    }

}
