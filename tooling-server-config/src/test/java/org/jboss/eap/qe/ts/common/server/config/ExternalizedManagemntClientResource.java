package org.jboss.eap.qe.ts.common.server.config;

import org.junit.rules.ExternalResource;
import org.wildfly.extras.creaper.core.offline.OfflineManagementClient;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;

import java.io.IOException;

/**
 * Provides a convenient wrapper for @{@link BaseManagementClientFactory} instances to be used as {@link ExternalResource}
 * @param <F> type of {@link BaseManagementClientFactory}
 */
public class ExternalizedManagemntClientResource<F extends BaseManagementClientFactory>
        extends ExternalResource implements ManagementClientFactory {

    private final F factory;

    public ExternalizedManagemntClientResource(F factory) {
        this.factory = factory;
    }

    /**
     * Creates Creaper {@link OnlineManagementClient}, which can be used for server configuration.
     * Note that <b>it's the caller's responsibility</b> to {@code close} the {@code OnlineManagementClient}!
     * @return {@link }{@link OnlineManagementClient} instance
     * @throws IOException
     */
    @Override
    public OnlineManagementClient createOnline() throws IOException {
        return factory.createOnline();
    }

    /**
     * As {@link ManagementClientFactory#createOnline()}, but you can define a positive connection
     * timeout in milliseconds.
     * @param timeoutInMillis int for milliseconds that represent the timeout for connection to management client to
     *                        be created.
     * @return {@link }{@link OfflineManagementClient} instance
     * @throws IOException Thrown when no connection can be enabled between client and server
     */
    @Override
    public OnlineManagementClient createOnline(int timeoutInMillis) throws IOException {
        return factory.createOnline(timeoutInMillis);
    }

    /**
     * Creates Creaper {@link OfflineManagementClient}, which can be used for server configuration.
     * Note that <b>it's the caller's responsibility</b> to {@code close} the {@code OnlineManagementClient}!
     *
     * @param rootDirectory Wildfly root directory
     * @param configurationFile Wildfly configuration file (e.g.: standalone.xml, standalone-full.xml etc.)
     * @return {@link }{@link OfflineManagementClient} instance
     * @throws IOException Thrown when no connection can be enabled between client and server
     */
    @Override
    public OfflineManagementClient createOffline(String rootDirectory, String configurationFile) {
        return createOffline(rootDirectory, configurationFile);
    }

    public F unwrap() {
        return factory;
    }
}
