package org.jboss.eap.qe.microprofile.health.integration;

import static org.hamcrest.Matchers.equalTo;

import org.jboss.eap.qe.microprofile.tooling.server.configuration.ConfigurationException;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;

/**
 * Checker for expected MP Metrics values.
 */
public class MetricsChecker {

    /**
     * Create an instance of the class and execute request.
     */
    public static MetricsChecker get(RequestSpecification request) throws ConfigurationException {
        return new MetricsChecker(request);
    }

    private final ValidatableResponse response;

    private MetricsChecker(RequestSpecification request) {
        response = request.get().then();
    }

    /**
     * Validate simulation counter {@code simulation-count} {@link FailSafeDummyService#simulateOpeningResources()}
     */
    public MetricsChecker validateSimulationCounter(int simulationCount) {
        response.body("application.simulation-count", equalTo(simulationCount));
        return this;
    }

    /**
     * Validate MP FT metric {@code isReady.retry.retries.total} for {@link FailSafeDummyService#isReady()}
     */
    public MetricsChecker validateRetryRetriesTotal(int retriesTotal) {
        response.body("application.'ft." + FailSafeDummyService.class.getName() + ".isReady.retry.retries.total'",
                equalTo(retriesTotal));
        return this;
    }

    /**
     * Validate MP FT metric {@code isReady.retry.callsFailed.total} for {@link FailSafeDummyService#isReady()}
     */
    public MetricsChecker validateRetryCallsFailedTotal(int callsFailedTotal) {
        response.body("application.'ft." + FailSafeDummyService.class.getName() + ".isReady.retry.callsFailed.total'",
                equalTo(callsFailedTotal));
        return this;
    }

    /**
     * Validate MP FT metric {@code isReady.retry.callsSucceededRetried.total} for {@link FailSafeDummyService#isReady()}
     */
    public MetricsChecker validateRetryCallsSucceededTotal(int callsSucceededTotal) {
        response.body(
                "application.'ft." + FailSafeDummyService.class.getName() + ".isReady.retry.callsSucceededRetried.total'",
                equalTo(callsSucceededTotal));
        return this;
    }

    /**
     * Validate MP FT metric {@code isReady.retry.callsSucceededNotRetried.total} for {@link FailSafeDummyService#isReady()}
     */
    public MetricsChecker validateRetryCallsSucceededNotTriedTotal(int callsSucceededNotTriedTotal) {
        response.body(
                "application.'ft." + FailSafeDummyService.class.getName()
                        + ".isReady.retry.callsSucceededNotRetried.total'",
                equalTo(callsSucceededNotTriedTotal));
        return this;
    }

    /**
     * Validate MP FT metric {@code isReady.invocations.total} for {@link FailSafeDummyService#isReady()}
     */
    public MetricsChecker validateInvocationsTotal(int invocationsTotal) {
        response.body("application.'ft." + FailSafeDummyService.class.getName() + ".isReady.invocations.total'",
                equalTo(invocationsTotal));
        return this;
    }

    /**
     * Validate MP FT metric {@code isReady.invocations.failed.total} for {@link FailSafeDummyService#isReady()}
     */
    public MetricsChecker validateInvocationsFailedTotal(int invocationsFailedTotal) {
        response.body("application.'ft." + FailSafeDummyService.class.getName() + ".isReady.invocations.failed.total'",
                equalTo(invocationsFailedTotal));
        return this;
    }

    /**
     * Validate MP FT metric {@code isReady.fallback.calls.total} for {@link FailSafeDummyService#isReadyFallback()}
     */
    public MetricsChecker validateFallbackCallsTotal(int fallbackCallsTotal) {
        response.body("application.'ft." + FailSafeDummyService.class.getName() + ".isReady.fallback.calls.total'",
                equalTo(fallbackCallsTotal));
        return this;
    }

}
