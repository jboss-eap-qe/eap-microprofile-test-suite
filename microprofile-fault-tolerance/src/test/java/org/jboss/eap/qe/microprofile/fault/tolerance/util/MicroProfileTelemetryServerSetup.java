package org.jboss.eap.qe.microprofile.fault.tolerance.util;

import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.operations.Address;
import org.wildfly.extras.creaper.core.online.operations.Operations;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;

/**
 * Operations required to set up the server for MicroProfile Telemetry
 */
public class MicroProfileTelemetryServerSetup {
    private static final Address OPENTELEMETRY_EXTENSION_ADDRESS = Address
            .extension("org.wildfly.extension.opentelemetry");
    private static final Address OPENTELEMETRY_SUBSYSTEM_ADDRESS = Address
            .subsystem("opentelemetry");

    private static final Address MICROPROFILE_TELEMETRY_EXTENSION_ADDRESS = Address
            .extension("org.wildfly.extension.microprofile.telemetry");
    private static final Address MICROPROFILE_TELEMETRY_SUBSYSTEM_ADDRESS = Address
            .subsystem("microprofile-telemetry");

    /**
     * Checks whether <b>"org.wildfly.extension.opentelemetry"</b> extension is present
     *
     * @return True if extension is already present,false otherwise
     * @throws Exception exception thrown by the internal operation executed by {@link Operations} API
     */
    public static Boolean openTelemetryExtensionExists(Operations operations) throws Exception {
        return operations.exists(OPENTELEMETRY_EXTENSION_ADDRESS);
    }

    /**
     * Checks whether <b>"opentelemetry"</b> subsystem is present
     *
     * @return True if extension is already present,false otherwise
     * @throws Exception exception thrown by the internal operation executed by {@link Operations} API
     */
    public static Boolean openTelemetrySubsystemExists(Operations operations) throws Exception {
        return operations.exists(OPENTELEMETRY_SUBSYSTEM_ADDRESS);
    }

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
     * Enable OpenTelemetry extension and subsystem.
     *
     * @throws Exception exception thrown by the internal operation executed by {@link OnlineManagementClient} API
     */
    public static void enableOpenTelemetry() throws Exception {
        try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
            enableOpenTelemetry(client);
        }
    }

    /**
     * Set a default, working OpenTelemetry subsystem configuration, e.g.: to set the OTLP receiver URL.
     */
    public static void addOpenTelemetryCollectorConfiguration(final String otlpCollectorEndpointUrl) throws Exception {
        try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
            addOpenTelemetryCollectorConfiguration(otlpCollectorEndpointUrl, client);
        }
    }

    /**
     * Set a default, working OpenTelemetry subsystem configuration, e.g.: to set the OTLP receiver URL.
     *
     * @param client {@link OnlineManagementClient} instance used to execute the command
     */
    public static void addOpenTelemetryCollectorConfiguration(final String otlpCollectorEndpointUrl,
            OnlineManagementClient client) throws Exception {
        Operations operations = new Operations(client);
        if (!openTelemetrySubsystemExists(operations)) {
            throw new IllegalStateException("OpenTelemetry subsystem not found");
        }
        operations.writeAttribute(OPENTELEMETRY_SUBSYSTEM_ADDRESS, "exporter-type", "otlp");
        operations.writeAttribute(OPENTELEMETRY_SUBSYSTEM_ADDRESS, "sampler-type", "on");
        operations.writeAttribute(OPENTELEMETRY_SUBSYSTEM_ADDRESS, "max-export-batch-size", "512");
        operations.writeAttribute(OPENTELEMETRY_SUBSYSTEM_ADDRESS, "max-queue-size", "1");
        operations.writeAttribute(OPENTELEMETRY_SUBSYSTEM_ADDRESS, "endpoint", otlpCollectorEndpointUrl);
        new Administration(client).reloadIfRequired();
    }

    /**
     * Enable OpenTelemetry extension and subsystem.
     *
     * @param client {@link OnlineManagementClient} instance used to execute the command
     * @throws Exception exception thrown by the internal operation executed by {@link OnlineManagementClient} API
     */
    public static void enableOpenTelemetry(OnlineManagementClient client) throws Exception {
        Operations operations = new Operations(client);
        if (!openTelemetryExtensionExists(operations)) {
            operations.add(OPENTELEMETRY_EXTENSION_ADDRESS);
        }
        if (!openTelemetrySubsystemExists(operations)) {
            operations.add(OPENTELEMETRY_SUBSYSTEM_ADDRESS);
        }
        new Administration(client).reloadIfRequired();
    }

    /**
     * Disable OpenTelemetry subsystem and extension
     *
     * @throws Exception exception thrown by the internal operation executed by {@link OnlineManagementClient} API
     */
    public static void disableOpenTelemetry() throws Exception {
        try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
            disableOpenTelemetry(client);
        }
    }

    /**
     * Disable OpenTelemetry subsystem and extension
     *
     * @param client {@link OnlineManagementClient} instance used to execute the command
     * @throws Exception exception thrown by the internal operation executed by {@link OnlineManagementClient} API
     */
    public static void disableOpenTelemetry(OnlineManagementClient client) throws Exception {
        Operations operations = new Operations(client);
        if (openTelemetrySubsystemExists(operations)) {
            operations.remove(OPENTELEMETRY_SUBSYSTEM_ADDRESS);
        }
        if (openTelemetryExtensionExists(operations)) {
            operations.remove(OPENTELEMETRY_EXTENSION_ADDRESS);
        }
        new Administration(client).reloadIfRequired();
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
        if (!openTelemetryExtensionExists(operations)) {
            operations.add(MICROPROFILE_TELEMETRY_EXTENSION_ADDRESS);
        }
        if (!openTelemetrySubsystemExists(operations)) {
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
