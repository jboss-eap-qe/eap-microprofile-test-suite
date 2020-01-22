package org.jboss.eap.qe.ts.common.docker;

/**
 * An exception thrown when killing a container fails
 */
public class ContainerKillException extends Exception {

    public ContainerKillException() {
        super();
    }

    public ContainerKillException(String message) {
        super(message);
    }

    public ContainerKillException(String message, Throwable cause) {
        super(message, cause);
    }

    public ContainerKillException(Throwable cause) {
        super(cause);
    }
}
