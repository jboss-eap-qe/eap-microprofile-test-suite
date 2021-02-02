package org.jboss.eap.qe.ts.common.docker;

/**
 * An exception thrown when container removal fails
 */
public class ContainerRemoveException extends Exception {

    public ContainerRemoveException() {
        super();
    }

    public ContainerRemoveException(String message) {
        super(message);
    }

    public ContainerRemoveException(String message, Throwable cause) {
        super(message, cause);
    }

    public ContainerRemoveException(Throwable cause) {
        super(cause);
    }
}
