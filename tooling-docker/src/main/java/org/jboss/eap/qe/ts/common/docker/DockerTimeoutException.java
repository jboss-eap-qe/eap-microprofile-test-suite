package org.jboss.eap.qe.ts.common.docker;

public class DockerTimeoutException extends RuntimeException {
    public DockerTimeoutException(String message) {
        super(message);
    }

    public DockerTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
