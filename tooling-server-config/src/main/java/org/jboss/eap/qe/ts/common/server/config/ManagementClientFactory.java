package org.jboss.eap.qe.ts.common.server.config;

import org.wildfly.extras.creaper.core.offline.OfflineManagementClient;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;

import java.io.IOException;

/**
 * Interface to define the contract for implementing management client factory instances using Creaper
 */
public interface ManagementClientFactory {
    /**
     * Creates Creaper {@link OnlineManagementClient}, which can be used for server configuration.
     * Note that <b>it's the caller's responsibility</b> to {@code close} the {@code OnlineManagementClient}!
     * @return {@link }{@link OnlineManagementClient} instance
     * @throws IOException
     */
    OnlineManagementClient createOnline() throws IOException;

    /**
     * As {@link ManagementClientFactory#createOnline()}, but you can define a positive connection
     * timeout in milliseconds.
     * @param timeoutInMillis int for milliseconds that represent the timeout for connection to management client to
     *                        be created.
     * @return {@link }{@link OfflineManagementClient} instance
     * @throws IOException Thrown when no connection can be enabled between client and server
     */
    OnlineManagementClient createOnline(int timeoutInMillis) throws IOException;

    /**
     * Creates Creaper {@link OfflineManagementClient}, which can be used for server configuration.
     * Note that <b>it's the caller's responsibility</b> to {@code close} the {@code OnlineManagementClient}!
     *
     * @param rootDirectory Wildfly root directory
     * @param configurationFile Wildfly configuration file (e.g.: standalone.xml, standalone-full.xml etc.)
     * @return {@link }{@link OfflineManagementClient} instance
     * @throws IOException Thrown when no connection can be enabled between client and server
     */
    OfflineManagementClient createOffline(String rootDirectory, String configurationFile) throws IOException;
}
