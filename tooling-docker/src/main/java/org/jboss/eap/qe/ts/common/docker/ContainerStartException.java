package org.jboss.eap.qe.ts.common.docker;

/**
 * An exception thrown when container start fails
 */
public class ContainerStartException extends Exception {

    public ContainerStartException() {
        super();
    }

    public ContainerStartException(String message) {
        super(message);
    }

    public ContainerStartException(String message, Throwable cause) {
        super(message, cause);
    }

    public ContainerStartException(Throwable cause) {
        super(cause);
    }
}
