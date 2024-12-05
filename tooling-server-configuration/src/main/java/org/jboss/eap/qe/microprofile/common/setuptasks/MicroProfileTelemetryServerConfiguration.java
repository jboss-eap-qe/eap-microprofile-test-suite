package org.jboss.eap.qe.microprofile.common.setuptasks;

import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.operations.Address;
import org.wildfly.extras.creaper.core.online.operations.Operations;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;

/**
 * Operations required to set up and configure the {@code microprofile-telemetry} extension
 */
public class MicroProfileTelemetryServerConfiguration {
    private static final Address MICROPROFILE_TELEMETRY_EXTENSION_ADDRESS = Address
            .extension("org.wildfly.extension.microprofile.telemetry");
    private static final Address MICROPROFILE_TELEMETRY_SUBSYSTEM_ADDRESS = Address
            .subsystem("microprofile-telemetry");

    /**
     * Checks whether <b>"org.wildfly.extension.microprofile.telemetry"</b> extension is present
     *
     * @return True if extension is already present,false otherwise
     * @throws Exception exception thrown by the internal operation executed by {@link Operations} API
     */
    public static Boolean microProfileTelemetryExtensionExists(Operations operations) throws Exception {
        return operations.exists(MICROPROFILE_TELEMETRY_EXTENSION_ADDRESS);
    }

    /**
     * Checks whether <b>"microprofile-telemetry"</b> subsystem is present
     *
     * @return True if extension is already present,false otherwise
     * @throws Exception exception thrown by the internal operation executed by {@link Operations} API
     */
    public static Boolean microProfileTelemetrySubsystemExists(Operations operations) throws Exception {
        return operations.exists(MICROPROFILE_TELEMETRY_SUBSYSTEM_ADDRESS);
    }

    /**
     * Enable MicroProfile Telemetry extension and subsystem.
     *
     * @throws Exception exception thrown by the internal operation executed by {@link OnlineManagementClient} API
     */
    public static void enableMicroProfileTelemetry() throws Exception {
        try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
            enableMicroProfileTelemetry(client);
        }
    }

    /**
     * Enable MicroProfile Telemetry extension and subsystem.
     *
     * @param client {@link OnlineManagementClient} instance used to execute the command
     * @throws Exception exception thrown by the internal operation executed by {@link OnlineManagementClient} API
     */
    public static void enableMicroProfileTelemetry(OnlineManagementClient client) throws Exception {
        Operations operations = new Operations(client);
        if (!microProfileTelemetryExtensionExists(operations)) {
            operations.add(MICROPROFILE_TELEMETRY_EXTENSION_ADDRESS);
        }
        if (!microProfileTelemetrySubsystemExists(operations)) {
            operations.add(MICROPROFILE_TELEMETRY_SUBSYSTEM_ADDRESS);
        }
        new Administration(client).reloadIfRequired();
    }

    /**
     * Disable MicroProfile Telemetry subsystem and extension
     *
     * @throws Exception exception thrown by the internal operation executed by {@link OnlineManagementClient} API
     */
    public static void disableMicroProfileTelemetry() throws Exception {
        try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
            disableMicroProfileTelemetry(client);
        }
    }

    /**
     * Disable MicroProfile Telemetry subsystem and extension
     *
     * @param client {@link OnlineManagementClient} instance used to execute the command
     * @throws Exception exception thrown by the internal operation executed by {@link OnlineManagementClient} API
     */
    public static void disableMicroProfileTelemetry(OnlineManagementClient client) throws Exception {
        Operations operations = new Operations(client);
        if (microProfileTelemetrySubsystemExists(operations)) {
            operations.remove(MICROPROFILE_TELEMETRY_SUBSYSTEM_ADDRESS);
        }
        if (microProfileTelemetryExtensionExists(operations)) {
            operations.remove(MICROPROFILE_TELEMETRY_EXTENSION_ADDRESS);
        }
        new Administration(client).reloadIfRequired();
    }
}
