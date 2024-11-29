package org.jboss.eap.qe.microprofile.common.setuptasks;

import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.operations.Address;
import org.wildfly.extras.creaper.core.online.operations.Operations;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;

/**
 * Utility class enabling and disabling MP Fault Tolerance (which is disabled by default)
 */
public class MicroProfileFaultToleranceServerConfiguration {

    private static final Address MICROPROFILE_FAULT_TOLERANCE_EXTENSION_ADDRESS = Address
            .extension("org.wildfly.extension.microprofile.fault-tolerance-smallrye");
    private static final Address MICROPROFILE_FAULT_TOLERANCE_SUBSYSTEM_ADDRESS = Address
            .subsystem("microprofile-fault-tolerance-smallrye");

    /**
     * Enable fault tolerance extension and subsystem.
     *
     * @throws Exception exception thrown by the internal operation executed by {@link OnlineManagementClient} API
     */
    public static void enableFaultTolerance() throws Exception {
        try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
            enableFaultTolerance(client);
        }
    }

    /**
     * Enable fault tolerance extension and subsystem.
     *
     * @param client {@link OnlineManagementClient} instance used to execute the command
     * @throws Exception exception thrown by the internal operation executed by {@link OnlineManagementClient} API
     */
    public static void enableFaultTolerance(OnlineManagementClient client) throws Exception {
        Operations operations = new Operations(client);
        if (!faultToleranceExtensionExists(operations)) {
            operations.add(MICROPROFILE_FAULT_TOLERANCE_EXTENSION_ADDRESS);
        }
        if (!faultToleranceSubsystemExists(operations)) {
            operations.add(MICROPROFILE_FAULT_TOLERANCE_SUBSYSTEM_ADDRESS);
        }
        new Administration(client).reloadIfRequired();
    }

    /**
     * Disable Fault Tolerance subsystem and extension
     *
     * @throws Exception exception thrown by the internal operation executed by {@link OnlineManagementClient} API
     */
    public static void disableFaultTolerance() throws Exception {
        try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
            disableFaultTolerance(client);
        }
    }

    /**
     * Disable Fault Tolerance subsystem and extension
     *
     * @param client {@link OnlineManagementClient} instance used to execute the command
     * @throws Exception exception thrown by the internal operation executed by {@link OnlineManagementClient} API
     */
    public static void disableFaultTolerance(OnlineManagementClient client) throws Exception {
        Operations operations = new Operations(client);
        if (faultToleranceSubsystemExists(operations)) {
            operations.remove(MICROPROFILE_FAULT_TOLERANCE_SUBSYSTEM_ADDRESS);
        }
        if (faultToleranceExtensionExists(operations)) {
            operations.remove(MICROPROFILE_FAULT_TOLERANCE_EXTENSION_ADDRESS);
        }
        new Administration(client).reloadIfRequired();
    }

    /**
     * Checks whether <b>"org.wildfly.extension.microprofile.fault-tolerance-smallrye"</b> extension is present
     *
     * @return True if extension is already present,false otherwise
     * @throws Exception exception thrown by the internal operation executed by {@link Operations} API
     */
    public static Boolean faultToleranceExtensionExists(Operations operations) throws Exception {
        return operations.exists(MICROPROFILE_FAULT_TOLERANCE_EXTENSION_ADDRESS);

    }

    /**
     * Checks whether <b>"microprofile-fault-tolerance-smallrye"</b> subsystem is present
     *
     * @return True if subsystem is already present,false otherwise
     * @throws Exception exception thrown by the internal operation executed by {@link Operations} API
     */
    public static Boolean faultToleranceSubsystemExists(Operations operations) throws Exception {
        return operations.exists(MICROPROFILE_FAULT_TOLERANCE_SUBSYSTEM_ADDRESS);

    }
}
