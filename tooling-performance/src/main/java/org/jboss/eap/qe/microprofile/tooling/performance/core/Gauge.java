package org.jboss.eap.qe.microprofile.tooling.performance.core;

/**
 * Defines the contract to implement a Gauge which is able to perform a measurement and return the results in
 * a given data type *
 * 
 * @param <R> The data type to be used to register measurements
 */
public interface Gauge<R extends MeasurementRecord> {

    R measure() throws MeasurementException;
}
