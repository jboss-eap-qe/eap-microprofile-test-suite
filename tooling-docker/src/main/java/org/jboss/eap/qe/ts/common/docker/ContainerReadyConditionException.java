package org.jboss.eap.qe.ts.common.docker;

/**
 * An exception thrown when checking for container readiness fails
 */
public class ContainerReadyConditionException extends Exception {

    public ContainerReadyConditionException(String message, Throwable cause) {
        super(message, cause);
    }

}
