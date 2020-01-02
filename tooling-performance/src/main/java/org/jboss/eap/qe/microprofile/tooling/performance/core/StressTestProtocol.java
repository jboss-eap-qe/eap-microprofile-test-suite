package org.jboss.eap.qe.microprofile.tooling.performance.core;

/**
 * Defines the contract to implement a stress test protocol that can communicate with an owning {@link StressTester}
 * instance
 */
public interface StressTestProtocol {

    void run(StressTester owner) throws StressTestException;
}
