package org.jboss.eap.qe.ts.common.server.config;

import org.wildfly.extras.creaper.core.ManagementClient;
import org.wildfly.extras.creaper.core.offline.OfflineManagementClient;
import org.wildfly.extras.creaper.core.offline.OfflineOptions;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.OnlineOptions;

import java.io.File;
import java.io.IOException;

/**
 * Factory for {@link ManagementClient} objects to work against a Wildfly instance running in <b>domain</b> mode.
 */
public class DomainManagementClientFactory extends BaseManagementClientFactory {

    private final String defaultProfile;
    private final String defaultHost;

    private DomainManagementClientFactory(String name, String host, int port, String defaultProfile,
                                          String defaultHost) {
        super(name, host, port);
        this.defaultProfile = defaultProfile;
        this.defaultHost = defaultHost;
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
        OnlineOptions.ConnectionOnlineOptions options =
                OnlineOptions.domain().forProfile(defaultProfile).forHost(defaultHost).build();

        OnlineOptions.OptionalOnlineOptions clientOptions = options
                .hostAndPort(host, port)
                .connectionTimeout(timeoutInMillis);

        return ManagementClient.online(clientOptions.build());
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
    public OfflineManagementClient createOffline(String rootDirectory, String configurationFile) throws IOException {
        return ManagementClient.offline(OfflineOptions
                .domain()
                .forProfile("default")
                .build()
                .rootDirectory(new File(rootDirectory))
                .configurationFile(configurationFile)
                .build()
        );
    }

    public static final class Builder {

        private String host;
        private int port;
        private String defaultProfile;
        private String defaultHost;

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder defaultProfile(String defaultProfile) {
            this.defaultProfile = defaultProfile;
            return this;
        }

        public Builder defaultHost(String defaultHost) {
            this.defaultHost = defaultHost;
            return this;
        }

        public DomainManagementClientFactory build(String withName) {
            return new DomainManagementClientFactory(withName, host, port, defaultProfile, defaultHost);
        }
    }
}
