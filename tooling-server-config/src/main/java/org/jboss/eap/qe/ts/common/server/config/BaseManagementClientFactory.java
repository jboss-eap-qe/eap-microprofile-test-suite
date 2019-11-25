package org.jboss.eap.qe.ts.common.server.config;

import org.wildfly.extras.creaper.core.ManagementClient;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;

import java.io.IOException;

/**
 * Provides common basic implementation of a factory for {@link ManagementClient} objects.
 */
public abstract class BaseManagementClientFactory implements ManagementClientFactory {

    /**
     * Default value for connection to managment client timeout, used when calling
     * {@link ManagementClientFactory#createOnline(int)} method.
     *
     */
    public static final int MANAGEMENT_CLIENT_CONNECTION_TIMEOUT_IN_SEC = 10;

    protected final String name;
    protected final String host;
    protected final int port;

    BaseManagementClientFactory(String name, String host, int port) {
        this.name = name;
        this.host = host;
        this.port = port;
    }

    /**
     * Gets the value - in seconds - for managment client connection timout.
     * @return returns {@link BaseManagementClientFactory#MANAGEMENT_CLIENT_CONNECTION_TIMEOUT_IN_SEC} value
     */
    public int getManagementClientConnectionTimeoutInSec() {
        return (int)MANAGEMENT_CLIENT_CONNECTION_TIMEOUT_IN_SEC;
    }

    /**
     * Creates Creaper {@link OnlineManagementClient}, which can be used for server configuration.
     * Note that <b>it's the caller's responsibility</b> to {@code close} the {@code OnlineManagementClient}!
     * @return {@link }{@link OnlineManagementClient} instance
     * @throws IOException
     */
    public OnlineManagementClient createOnline() throws IOException {
        return createOnline(1000 * getManagementClientConnectionTimeoutInSec());
    }
}
