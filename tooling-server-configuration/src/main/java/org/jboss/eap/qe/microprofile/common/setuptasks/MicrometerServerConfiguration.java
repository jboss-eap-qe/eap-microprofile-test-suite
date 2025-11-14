package org.jboss.eap.qe.microprofile.common.setuptasks;

import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.operations.Address;
import org.wildfly.extras.creaper.core.online.operations.Operations;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;

import com.google.common.base.Strings;

/**
 * Utility class enabling and disabling Micrometer (which is disabled by default)
 */
public class MicrometerServerConfiguration {
    private static final Address MICROMETER_EXTENSION_ADDRESS = Address
            .extension("org.wildfly.extension.micrometer");
    private static final Address MICROMETER_SUBSYSTEM_ADDRESS = Address
            .subsystem("micrometer");
    private static final Address UNDERTOW_SUBSYSTEM_ADDRESS = Address.subsystem("undertow");
    private static final Address LOGGING_SUBSYSTEM_ADDRESS = Address.subsystem("logging");

    /**
     * Enable Micrometer extension and subsystem.
     *
     * @param otlpHttpEndpoint OTel collector endpoint
     *
     * @throws Exception exception thrown by the internal operation executed by {@link OnlineManagementClient} API
     */
    public static void enableMicrometer(final String otlpHttpEndpoint) throws Exception {
        try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
            enableMicrometer(client, otlpHttpEndpoint);
        }
    }

    /**
     * Enable Micrometer extension and subsystem.
     *
     * @param client {@link OnlineManagementClient} instance used to execute the command
     * @param otlpHttpEndpoint OTel collector endpoint
     * @throws Exception exception thrown by the internal operation executed by {@link OnlineManagementClient} API
     */
    public static void enableMicrometer(OnlineManagementClient client, final String otlpHttpEndpoint) throws Exception {
        enableMicrometer(client, otlpHttpEndpoint, false);
    }

    /**
     * Enable Micrometer extension and subsystem.
     *
     * @param client {@link OnlineManagementClient} instance used to execute the command
     * @param otlpHttpEndpoint OTel collector endpoint
     * @param skipReload Allow to skip reload at the end of the configuration
     * @throws Exception exception thrown by the internal operation executed by {@link OnlineManagementClient} API
     */
    public static void enableMicrometer(OnlineManagementClient client, final String otlpHttpEndpoint, boolean skipReload)
            throws Exception {
        Operations operations = new Operations(client);
        operations.writeAttribute(UNDERTOW_SUBSYSTEM_ADDRESS, "statistics-enabled", "true");
        if (!micrometerExtensionExists(operations)) {
            operations.add(MICROMETER_EXTENSION_ADDRESS);
        }
        if (!micrometerSubsystemExists(operations)) {
            operations.add(MICROMETER_SUBSYSTEM_ADDRESS);
        }
        if (!Strings.isNullOrEmpty(otlpHttpEndpoint)) {
            operations.writeAttribute(MICROMETER_SUBSYSTEM_ADDRESS, "endpoint", otlpHttpEndpoint + "/v1/metrics");
            operations.writeAttribute(MICROMETER_SUBSYSTEM_ADDRESS, "step", "1");
        }
        client.execute("/subsystem=logging/logger=io.micrometer:add(level=TRACE)");
        if (!skipReload) {
            new Administration(client).reloadIfRequired();
        }
    }

    /**
     * Disable Micrometer subsystem and extension
     *
     * @throws Exception exception thrown by the internal operation executed by {@link OnlineManagementClient} API
     */
    public static void disableMicrometer() throws Exception {
        try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
            disableMicrometer(client);
        }
    }

    /**
     * Disable Micrometer subsystem and extension
     *
     * @param client {@link OnlineManagementClient} instance used to execute the command
     * @throws Exception exception thrown by the internal operation executed by {@link OnlineManagementClient} API
     */
    public static void disableMicrometer(OnlineManagementClient client) throws Exception {
        disableMicrometer(client, false);
    }

    /**
     * Disable Micrometer subsystem and extension
     *
     * @param client {@link OnlineManagementClient} instance used to execute the command
     * @param skipReload Allow to skip reload at the end of the configuration
     * @throws Exception exception thrown by the internal operation executed by {@link OnlineManagementClient} API
     */
    public static void disableMicrometer(OnlineManagementClient client, boolean skipReload) throws Exception {
        Operations operations = new Operations(client);
        if (micrometerSubsystemExists(operations)) {
            operations.remove(MICROMETER_SUBSYSTEM_ADDRESS);
        }
        if (micrometerExtensionExists(operations)) {
            operations.remove(MICROMETER_EXTENSION_ADDRESS);
        }
        operations.undefineAttribute(UNDERTOW_SUBSYSTEM_ADDRESS, "statistics-enabled");
        if (!skipReload) {
            new Administration(client).reloadIfRequired();
        }
    }

    /**
     * Checks whether <b>"org.wildfly.extension.microprofile.fault-tolerance-smallrye"</b> extension is present
     *
     * @return True if extension is already present,false otherwise
     * @throws Exception exception thrown by the internal operation executed by {@link Operations} API
     */
    public static Boolean micrometerExtensionExists(Operations operations) throws Exception {
        return operations.exists(MICROMETER_EXTENSION_ADDRESS);

    }

    /**
     * Checks whether <b>"microprofile-fault-tolerance-smallrye"</b> subsystem is present
     *
     * @return True if subsystem is already present,false otherwise
     * @throws Exception exception thrown by the internal operation executed by {@link Operations} API
     */
    public static Boolean micrometerSubsystemExists(Operations operations) throws Exception {
        return operations.exists(MICROMETER_SUBSYSTEM_ADDRESS);
    }
}
