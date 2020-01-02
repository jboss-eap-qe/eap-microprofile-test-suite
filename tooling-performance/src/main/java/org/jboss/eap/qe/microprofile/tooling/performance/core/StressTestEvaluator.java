package org.jboss.eap.qe.microprofile.tooling.performance.core;

/**
 * Defines the contract to implement an evaluator for stress test results
 * 
 * @param <O> Type of outcome to be returned when {@link StressTestEvaluator#evaluate()} is called
 */
public interface StressTestEvaluator<O extends StressTestOutcome> {

    /**
     * Evaluates stress test results and returns an {@link StressTestOutcome} instance
     * 
     * @return {@link StressTestOutcome} instance storing the test session results
     */
    O evaluate();
}
