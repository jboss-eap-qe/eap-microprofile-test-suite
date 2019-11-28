package org.jboss.eap.qe.microprofile.tooling.server.configuration;

/**
 * Represents configuration related exceptions
 */
public class ConfigurationException extends Exception {

    private static final long serialVersionUID = -4950064556540049693L;

    public ConfigurationException() {
        this("");
    }

    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConfigurationException(Throwable cause) {
        super(cause);
    }
}