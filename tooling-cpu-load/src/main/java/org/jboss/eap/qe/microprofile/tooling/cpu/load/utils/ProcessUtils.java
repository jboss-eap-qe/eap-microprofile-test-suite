package org.jboss.eap.qe.microprofile.tooling.cpu.load.utils;

import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.wildfly.extras.creaper.core.online.ModelNodeResult;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;

/**
 * Utility class for working with OS process. Use {@link ProcessUtilsProvider} to get instance of this class for your
 * platform.
 */
public interface ProcessUtils {

    /**
     * CPU cores to which process can be bound on.
     */
    enum CPUCoreMask {
        ALL_CORES, // all CPU cores
        FIRST_CORE // first core
    }

    /**
     * Retrieves process id of EAP/Wildfly server using managament client
     *
     * @return pid of the server
     */
    default int getServerProcessId() throws Exception {
        try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
            ModelNode model = new ModelNode();
            model.get(ClientConstants.OP).set("read-resource");
            model.get(ClientConstants.OP_ADDR).add("core-service", "platform-mbean");
            model.get(ClientConstants.OP_ADDR).add("type", "runtime");

            ModelNodeResult result = client.execute(model);
            String nodeName = result.get("result").get("name").asString();
            return Integer.valueOf(nodeName.substring(0, nodeName.indexOf("@")));
        }
    }

    /**
     * Returns pid of given process.
     *
     * @param process process
     * @return pid of the process
     */
    int getProcessId(Process process);

    /**
     * Binds process to given CPU cores.
     *
     * @param pid process id
     * @param cpuCoreMask cpu cores to which process will be bound on
     *        see {@link org.jboss.eap.qe.microprofile.tooling.cpu.load.utils.ProcessUtils.CPUCoreMask}
     * @throws Exception throws exception if this operation fails
     */
    void bindProcessToCPU(int pid, CPUCoreMask cpuCoreMask) throws Exception;

    /**
     * Kills process (kill -9 ...)
     *
     * @param process process to kill
     */
    void killProcess(Process process) throws Exception;
}
