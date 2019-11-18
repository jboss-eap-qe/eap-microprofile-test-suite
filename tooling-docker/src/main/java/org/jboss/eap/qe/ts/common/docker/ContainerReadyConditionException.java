package org.jboss.eap.qe.ts.common.docker;

public class ContainerReadyConditionException extends RuntimeException {
    public ContainerReadyConditionException(String message, Throwable cause) {
        super(message, cause);
    }
}
