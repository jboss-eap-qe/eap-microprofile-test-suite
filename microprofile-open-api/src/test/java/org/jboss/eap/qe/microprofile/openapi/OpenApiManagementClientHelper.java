package org.jboss.eap.qe.microprofile.openapi;

import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientHelper;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientRelatedException;
import org.wildfly.extras.creaper.core.online.ModelNodeResult;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.operations.Address;
import org.wildfly.extras.creaper.core.online.operations.OperationException;
import org.wildfly.extras.creaper.core.online.operations.Operations;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Helper that provides operations to manage the OpenAPI extension configuration via
 * {@link OnlineManagementClient} API.
 */
public class OpenApiManagementClientHelper {

    /**
     * Executes CLI command to remove MP OpenAPI extension
     *
     * @param client {@link OnlineManagementClient} instance used to execute the command
     * @return {@link ModelNodeResult} instance to represent the command execution outcome
     * @throws ManagementClientRelatedException Wraps exceptions thrown by the internal operation executed by
     *                                          {@link OnlineManagementClient} API
     */
    public static ModelNodeResult removeOpenApiExtension(OnlineManagementClient client) throws ManagementClientRelatedException {
        return ManagementClientHelper.executeCliCommand(client,
                "/extension=org.wildfly.extension.microprofile.openapi-smallrye:remove");
    }

    /**
     * Executes CLI command to remove MP OpenAPI subsystem
     *
     * @param client {@link OnlineManagementClient} instance used to execute the command
     * @return {@link ModelNodeResult} instance to represent the command execution outcome
     * @throws ManagementClientRelatedException Wraps exceptions thrown by the internal operation executed by
     *                                          {@link OnlineManagementClient} API
     */
    public static ModelNodeResult removeOpenApiSubsystem(OnlineManagementClient client) throws ManagementClientRelatedException {
        return ManagementClientHelper.executeCliCommand(client,
                "/subsystem=microprofile-openapi-smallrye:remove");
    }

    /**
     * Executes CLI command to add MP OpenAPI subsystem
     *
     * @param client {@link OnlineManagementClient} instance used to execute the command
     * @return {@link ModelNodeResult} instance to represent the command execution outcome
     * @throws ManagementClientRelatedException Wraps exceptions thrown by the internal operation executed by
     *                                          {@link OnlineManagementClient} API
     */
    public static ModelNodeResult addOpenApiSubsystem(OnlineManagementClient client) throws ManagementClientRelatedException {
        return ManagementClientHelper.executeCliCommand(client,
                "/subsystem=microprofile-openapi-smallrye:add");
    }

    /**
     * Executes CLI command to add MP OpenAPI extension
     *
     * @param client {@link OnlineManagementClient} instance used to execute the command
     * @return {@link ModelNodeResult} instance to represent the command execution outcome
     * @throws ManagementClientRelatedException Wraps exceptions thrown by the internal operation executed by
     *                                          {@link OnlineManagementClient} API
     */
    public static ModelNodeResult addOpenApiExtension(OnlineManagementClient client) throws ManagementClientRelatedException {
        return ManagementClientHelper.executeCliCommand(client,
                "/extension=org.wildfly.extension.microprofile.openapi-smallrye:add");
    }

    /**
     * Executes a batch of CLI commands to enable MP OpenAPI feature
     *
     * @param client {@link OnlineManagementClient} instance used to execute the command
     * @throws ManagementClientRelatedException Wraps exceptions thrown by the internal operation executed by
     *                                          {@link OnlineManagementClient} API
     */
    public static void enableOpenApi(OnlineManagementClient client) throws ManagementClientRelatedException {
        try {
            ModelNodeResult result = addOpenApiExtension(client);
            result.assertSuccess();
            result = addOpenApiSubsystem(client);
            result.assertSuccess();
            new Administration(client).reload();
        } catch (TimeoutException | IOException | InterruptedException e) {
            throw new ManagementClientRelatedException(e);
        }
    }

    /**
     * Executes a batch of CLI commands to disable MP OpenAPI feature
     *
     * @param client {@link OnlineManagementClient} instance used to execute the command
     * @throws ManagementClientRelatedException Wraps exceptions thrown by the internal operation executed by
     *                                          {@link OnlineManagementClient} API
     */
    public static void disableOpenApi(OnlineManagementClient client) throws ManagementClientRelatedException {
        try {
            ModelNodeResult result = removeOpenApiSubsystem(client);
            result.assertSuccess();
            result = removeOpenApiExtension(client);
            result.assertSuccess();
            new Administration(client).reload();
        } catch (TimeoutException | IOException | InterruptedException e) {
            throw new ManagementClientRelatedException(e);
        }
    }

    /**
     * Checks whether <b>"org.wildfly.extension.microprofile.openapi-smallrye"</b> subsytem is present
     *
     * @param client {@link OnlineManagementClient} instance used to execute the command
     * @return True if subsystem is already present,false otherwise
     * @throws ManagementClientRelatedException Wraps exceptions thrown by the internal operation executed by
     *                                          {@link OnlineManagementClient} API
     */
    public static Boolean openapiSubsystemExists(OnlineManagementClient client) throws ManagementClientRelatedException {
        Operations ops = new Operations(client);
        try {
            return ops.exists(Address.extension("org.wildfly.extension.microprofile.openapi-smallrye"));
        } catch (IOException | OperationException e) {
            throw new ManagementClientRelatedException(e);
        }
    }
}
