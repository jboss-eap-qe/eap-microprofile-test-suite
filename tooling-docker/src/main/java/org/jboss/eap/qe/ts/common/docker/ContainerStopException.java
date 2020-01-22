package org.jboss.eap.qe.ts.common.docker;

/**
 * An exception thrown when container stop fails
 */
public class ContainerStopException extends Exception {

    public ContainerStopException() {
        super();
    }

    public ContainerStopException(String message) {
        super(message);
    }

    public ContainerStopException(String message, Throwable cause) {
        super(message, cause);
    }

    public ContainerStopException(Throwable cause) {
        super(cause);
    }
}
