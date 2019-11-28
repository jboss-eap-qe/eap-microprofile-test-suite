package org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper;

import org.wildfly.extras.creaper.core.online.ModelNodeResult;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;

/**
 * Helper that provides operations to manage the CLI operations via {@link OnlineManagementClient} API.
 */
public class ManagementClientHelper {

    /**
     * Executes a valid CLI command through the given {@link OnlineManagementClient} instance.
     *
     * @param client  {@link OnlineManagementClient} instance used to execute the command
     * @param command A valid CLI command
     * @return {@link ModelNodeResult} instance to represent the command execution outcome
     * @throws ManagementClientRelatedException Wraps exceptions thrown by the internal operation executed by
     *                                          {@link OnlineManagementClient} API
     */
    public static ModelNodeResult executeCliCommand(OnlineManagementClient client,
                                                    String command) throws ManagementClientRelatedException {
        try {
            return client.execute(command);
        } catch (Exception e) {
            throw new ManagementClientRelatedException(e);
        }
    }
}
