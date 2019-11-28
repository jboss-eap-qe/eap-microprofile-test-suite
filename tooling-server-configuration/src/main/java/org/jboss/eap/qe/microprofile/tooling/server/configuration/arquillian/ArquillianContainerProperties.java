package org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian;

import org.jboss.arquillian.config.descriptor.api.ArquillianDescriptor;
import org.jboss.arquillian.config.descriptor.api.ContainerDef;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.ConfigurationException;

import java.util.Optional;

/**
 * Provides access to Arquillian container configuration properties
 */
public class ArquillianContainerProperties {

    private final ArquillianDescriptor arquillianDescriptor;

    public static final String DEFAULT_CONTAINER_NAME = "jboss";
    public static final String DEFAULT_MANAGEMENT_ADDRESS_VALUE = "127.0.0.1";
    public static final String DEFAULT_MANAGEMENT_PORT_VALUE = "9990";
    public static final String ARQ_MANAGEMENT_ADDRESS_PROPERTY_NAME = "managementAddress";
    public static final String ARQ_MANAGEMENT_PORT_PROPERTY_NAME = "managementPort";

    public ArquillianContainerProperties(ArquillianDescriptor descriptor) {
        this.arquillianDescriptor = descriptor;
    }

    /**
     * Retrieves definition for the given Arquillian container name
     *
     * @param container Name of requested container definition
     * @return Instance of {@link ContainerDef} that describes the container
     */
    private Optional<ContainerDef> getNamedContainerDefinition(String container) {
        return arquillianDescriptor.getContainers().stream()
                .filter(c -> c.getContainerName().equals(container))
                .findFirst();
    }

    /**
     * Gets the value for a property for a named Arquillian container
     *
     * @param container The name of the container
     * @param key       The name of the property
     * @return The value for the requested property
     * @throws {@link ConfigurationException} instance if no container for given name is found
     */
    public String getContainerProperty(String container, String key) throws ConfigurationException {
        Optional<ContainerDef> containerDefinition = getNamedContainerDefinition(container);
        if (!containerDefinition.isPresent()) {
            throw new ConfigurationException(
                    String.format("Definition for container with name [%s] was not found in arquillian.xml descriptor", container));
        }
        return containerDefinition.get().getContainerProperty(key);
    }

    /**
     * Gets the value for a property of a given Arquillian container and returns the default value passed when the
     * given property value is null or empty
     *
     * @param container    The name of the container
     * @param key          The name of the property
     * @param defaultValue The default value to be returned when the given property name is not found
     * @return The value for the requested property or the given default value when the given property value is null or
     * empty
     * @throws {@link ConfigurationException} instance if no container for given name is found
     */
    public String getContainerProperty(String container, String key, String defaultValue) throws ConfigurationException {
        Optional<ContainerDef> containerDefinition = getNamedContainerDefinition(container);
        if (!containerDefinition.isPresent()) {
            throw new ConfigurationException(
                    String.format("Definition for container with name [%s] was not found in arquillian.xml descriptor", container));
        }
        String result = containerDefinition.get().getContainerProperty(key);
        return (result == null) || result.isEmpty() ? defaultValue : result;
    }

    /**
     * Gets the default value for the management address, which is taken from arquillian.xml configuration file
     * <b>"127.0.0.1"</b> is returned if no value is found for "managementAddress" property.
     *
     * @return The default Wildfly management interface address
     */
    public String getDefaultManagementAddress() throws ConfigurationException {
        return getContainerProperty(
                DEFAULT_CONTAINER_NAME,
                ARQ_MANAGEMENT_ADDRESS_PROPERTY_NAME,
                DEFAULT_MANAGEMENT_ADDRESS_VALUE);
    }

    /**
     * Gets the default value for the management port, which is taken from arquillian.xml configuration file
     * <b>9990</b> is returned if no value is found for "managementPort" property.
     *
     * @return The default Wildfly management interface port
     */
    public int getDefaultManagementPort() throws ConfigurationException {
        return Integer.parseInt(
                getContainerProperty(
                        DEFAULT_CONTAINER_NAME,
                        ARQ_MANAGEMENT_PORT_PROPERTY_NAME,
                        DEFAULT_MANAGEMENT_PORT_VALUE));
    }
}
