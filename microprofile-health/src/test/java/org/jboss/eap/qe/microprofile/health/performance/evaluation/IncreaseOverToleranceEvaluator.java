package org.jboss.eap.qe.microprofile.health.performance.evaluation;

import org.jboss.eap.qe.microprofile.tooling.performance.core.StressTestEvaluator;
import org.jboss.eap.qe.microprofile.tooling.performance.core.StressTestOutcome;

/**
 * This evaluator is intended to assess whether final value is showing an increase bigger than the
 * accepted tolerance when compared to initial value
 */
public class IncreaseOverToleranceEvaluator implements StressTestEvaluator<IncreaseOverToleranceEvaluator.Outcome> {

    private final Long tolerance;
    private Long initialValue;
    private Long finalValue;

    public IncreaseOverToleranceEvaluator(final Long tolerance) {
        this.tolerance = tolerance;
    }

    public Long getInitialValue() {
        return initialValue;
    }

    public void setInitialValue(Long initialValue) {
        this.initialValue = initialValue;
    }

    public Long getFinalValue() {
        return finalValue;
    }

    public void setFinalValue(Long finalValue) {
        this.finalValue = finalValue;
    }

    /**
     * Outcome for this evaluator execution
     */
    public static class Outcome implements StressTestOutcome {

        private final Long initialValue;
        private final Long tolerance;
        private final Long finalValue;
        private final boolean passed;

        Outcome(final Long initialValue, final Long finalValue, final Long tolerance, final boolean passed) {
            this.initialValue = initialValue;
            this.tolerance = tolerance;
            this.finalValue = finalValue;
            this.passed = passed;
        }

        public Long getInitialValue() {
            return initialValue;
        }

        public Long getFinalValue() {
            return finalValue;
        }

        public boolean isPassed() {
            return passed;
        }

        static Outcome success(final Long initialValue, final Long finalValue, final Long tolerance) {
            return new Outcome(initialValue, finalValue, tolerance, true);
        }

        static Outcome fail(final Long initialValue, final Long finalValue, final Long tolerance) {
            return new Outcome(initialValue, finalValue, tolerance, false);
        }
    }

    public Outcome evaluate() {
        if ((finalValue - initialValue) > tolerance) {
            return Outcome.fail(initialValue, finalValue, tolerance);
        }
        return Outcome.success(initialValue, finalValue, tolerance);
    }
}
