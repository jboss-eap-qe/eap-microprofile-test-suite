package org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian;

import org.jboss.arquillian.container.test.spi.RemoteLoadableExtension;
import org.jboss.arquillian.core.spi.LoadableExtension;

/**
 * Extension which sets property descriptor in {@link ArquillianContainerProperties} class which is used in MP specs
 * tests
 */
public class ArquillianConfigurationExtension implements RemoteLoadableExtension {

    /**
     * Registers the extension
     *
     * @param builder Instance of builder to be used by the registered extension
     */
    @Override
    public void register(LoadableExtension.ExtensionBuilder builder) {
        builder.observer(ArquillianDescriptorWrapper.class);
    }
}
