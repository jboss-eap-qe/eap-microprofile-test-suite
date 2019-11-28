package org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian;

import org.jboss.arquillian.config.descriptor.api.ArquillianDescriptor;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.event.suite.BeforeSuite;

/**
 * Wraps Arquillian descriptor retrieval/exposure logic
 */
public class ArquillianDescriptorWrapper {

    /**
     * This property is initialized during Before* phase by ArquillianConfiguration extension
     */
    private static ArquillianDescriptor arquillianDescriptor;

    public ArquillianDescriptorWrapper() {
    }

    /**
     * Sets the {@link ArquillianDescriptorWrapper#arquillianDescriptor} field during Before* phase through
     * ArquillianConfiguration
     * extension
     *
     * @param event      {@link BeforeSuite} instance of Arquillian event the method listens to
     * @param descriptor {@link ArquillianDescriptor} instance that will be assigned to the internal static field
     */
    public static void setArquillianDescriptor(@Observes BeforeSuite event, ArquillianDescriptor descriptor) {
        arquillianDescriptor = descriptor;
    }

    public static ArquillianDescriptor getArquillianDescriptor() {
        return arquillianDescriptor;
    }
}