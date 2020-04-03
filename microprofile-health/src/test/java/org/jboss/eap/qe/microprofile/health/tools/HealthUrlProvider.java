package org.jboss.eap.qe.microprofile.health.tools;

import org.jboss.eap.qe.microprofile.tooling.server.configuration.ConfigurationException;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.ArquillianContainerProperties;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.ArquillianDescriptorWrapper;

/**
 * Class provides static methods to get URLs tp {@code /health}, {@code /health/live} and {@code /health/ready} endpoints
 * based on Arquillian container properties.
 */
public class HealthUrlProvider {
    public static String healthEndpoint() throws ConfigurationException {
        ArquillianContainerProperties arqProps = new ArquillianContainerProperties(
                ArquillianDescriptorWrapper.getArquillianDescriptor());
        return "http://" + arqProps.getDefaultManagementAddress() + ":" + arqProps.getDefaultManagementPort() + "/health";
    }

    public static String liveEndpoint() throws ConfigurationException {
        ArquillianContainerProperties arqProps = new ArquillianContainerProperties(
                ArquillianDescriptorWrapper.getArquillianDescriptor());
        return "http://" + arqProps.getDefaultManagementAddress() + ":" + arqProps.getDefaultManagementPort()
                + "/health/live";
    }

    public static String readyEndpoint() throws ConfigurationException {
        ArquillianContainerProperties arqProps = new ArquillianContainerProperties(
                ArquillianDescriptorWrapper.getArquillianDescriptor());
        return readyEndpoint(arqProps);
    }

    public static String readyEndpoint(ArquillianContainerProperties arqProps) throws ConfigurationException {
        return "http://" + arqProps.getDefaultManagementAddress() + ":" + arqProps.getDefaultManagementPort()
                + "/health/ready";
    }
}
