package org.jboss.eap.qe.ts.common.docker;

/**
 * An exception thrown when docker command fails to return/execute
 */
public class DockerCommandException extends Exception {

    public DockerCommandException() {
        super();
    }

    public DockerCommandException(String message) {
        super(message);
    }

    public DockerCommandException(String message, Throwable cause) {
        super(message, cause);
    }

    public DockerCommandException(Throwable cause) {
        super(cause);
    }
}
