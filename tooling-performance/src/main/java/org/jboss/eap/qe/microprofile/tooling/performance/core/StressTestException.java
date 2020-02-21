package org.jboss.eap.qe.microprofile.tooling.performance.core;

/**
 * Represents stress test related exceptions
 */
public class StressTestException extends Exception {

    public StressTestException() {
        this("");
    }

    public StressTestException(String message) {
        super(message);
    }

    public StressTestException(String message, Throwable cause) {
        super(message, cause);
    }

    public StressTestException(Throwable cause) {
        super(cause);
    }
}
