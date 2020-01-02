package org.jboss.eap.qe.microprofile.tooling.performance.core;

/**
 * Represents memory footprint measurements related exceptions
 */
public class MeasurementException extends Exception {

    public MeasurementException() {
        this("");
    }

    public MeasurementException(String message) {
        super(message);
    }

    public MeasurementException(String message, Throwable cause) {
        super(message, cause);
    }

    public MeasurementException(Throwable cause) {
        super(cause);
    }
}
