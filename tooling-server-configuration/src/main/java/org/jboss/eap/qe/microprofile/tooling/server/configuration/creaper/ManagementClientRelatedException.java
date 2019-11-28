package org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper;

/**
 * Represents management client related exceptions
 */
public class ManagementClientRelatedException extends Exception {

    private static final long serialVersionUID = -4044759360593130610L;

    public ManagementClientRelatedException() {
        this("");
    }

    public ManagementClientRelatedException(String message) {
        super(message);
    }

    public ManagementClientRelatedException(String message, Throwable cause) {
        super(message, cause);
    }

    public ManagementClientRelatedException(Throwable cause) {
        super(cause);
    }
}