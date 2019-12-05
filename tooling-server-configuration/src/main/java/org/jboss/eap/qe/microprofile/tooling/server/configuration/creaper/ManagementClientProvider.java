package org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper;

import org.jboss.eap.qe.microprofile.tooling.server.configuration.ConfigurationException;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.ArquillianContainerProperties;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.ArquillianDescriptorWrapper;
import org.wildfly.extras.creaper.core.ManagementClient;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.OnlineOptions;

/**
 * Provider for Creaper's OnlineManagementClient
 */
public class ManagementClientProvider {

    private ManagementClientProvider() {
    }

    /**
     * Creates {@link OnlineManagementClient} for <b>standalone</b> mode, based on {@link ArquillianContainerProperties}
     * obtained from {@link ArquillianDescriptorWrapper}
     *
     * @return Initialized {@link OnlineManagementClient} instance, don't forget to close it
     * @throws ConfigurationException Wraps exceptions thrown by the internal operation executed by
     *         {@link ArquillianContainerProperties} API
     */
    public static OnlineManagementClient onlineStandalone() throws ConfigurationException {
        return onlineStandalone(new ArquillianContainerProperties(ArquillianDescriptorWrapper.getArquillianDescriptor()));
    }

    /**
     * Creates {@link OnlineManagementClient} for <b>standalone</b> mode, based on {@link ArquillianContainerProperties}
     *
     * @param arquillianContainerProperties {@link ArquillianContainerProperties} instance to provide Arquillian
     *        configuration
     * @return Initialized {@link OnlineManagementClient} instance, don't forget to close it
     * @throws ConfigurationException Wraps exceptions thrown by the internal operation executed by
     *         {@link ArquillianContainerProperties} API
     */
    public static OnlineManagementClient onlineStandalone(ArquillianContainerProperties arquillianContainerProperties)
            throws ConfigurationException {
        return ManagementClient.onlineLazy(
                OnlineOptions.standalone().hostAndPort(
                        arquillianContainerProperties.getDefaultManagementAddress(),
                        arquillianContainerProperties.getDefaultManagementPort()).build());
    }
}
