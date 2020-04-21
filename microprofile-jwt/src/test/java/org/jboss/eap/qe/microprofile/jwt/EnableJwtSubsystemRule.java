package org.jboss.eap.qe.microprofile.jwt;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.jboss.eap.qe.microprofile.tooling.server.configuration.ConfigurationException;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.junit.rules.ExternalResource;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.operations.Address;
import org.wildfly.extras.creaper.core.online.operations.OperationException;
import org.wildfly.extras.creaper.core.online.operations.Operations;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;

/**
 * A JUnit rule to enable/disable MP-JWT subsystem
 */
public class EnableJwtSubsystemRule extends ExternalResource {

    private static final Address MP_JWT_EXTENSION_ADDRESS = Address.extension("org.wildfly.extension.microprofile.jwt");
    private static final Address MP_JWT_ADDRESS = Address.subsystem("microprofile-jwt-smallrye");

    private boolean wasExtensionAdded;
    private boolean wasSubsystemAdded;

    @Override
    protected void before() throws IOException, OperationException, TimeoutException, InterruptedException,
            ConfigurationException {
        try (final OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
            final Operations ops = new Operations(client);
            /*
             * if (!ops.exists(MP_JWT_EXTENSION_ADDRESS)) {
             * ops.add(MP_JWT_EXTENSION_ADDRESS).assertSuccess();
             * wasExtensionAdded = true;
             * }
             */
            if (!ops.exists(MP_JWT_ADDRESS)) {
                ops.add(MP_JWT_ADDRESS).assertSuccess();
                wasSubsystemAdded = true;
            }
            new Administration(client).reload();
        }
    }

    @Override
    protected void after() {
        try (final OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
            final Operations ops = new Operations(client);
            if (wasExtensionAdded) {
                ops.remove(MP_JWT_EXTENSION_ADDRESS).assertSuccess();
            }
            if (wasSubsystemAdded) {
                ops.remove(MP_JWT_ADDRESS).assertSuccess();
            }
            if (wasExtensionAdded || wasSubsystemAdded) {
                new Administration(client).reload();
            }
        } catch (IOException | ConfigurationException | InterruptedException | TimeoutException e) {
            throw new IllegalStateException("Unexpected exception when removing MP-JWT subsystem", e);
        }
    }
}
