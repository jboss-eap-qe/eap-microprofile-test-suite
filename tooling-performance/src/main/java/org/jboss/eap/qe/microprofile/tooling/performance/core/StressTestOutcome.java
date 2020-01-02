package org.jboss.eap.qe.microprofile.tooling.performance.core;

/**
 * Defines the contract to implement an object storing stress test results
 */
public interface StressTestOutcome {
    boolean isPassed();
}
