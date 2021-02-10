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
     * Validate MP FT metric {@code ft.retry.retries.total} for {@link FailSafeDummyService#isReady()}
     */
    public MetricsChecker validateRetryRetriesTotal(int retriesTotal) {
        response.body("base.'ft.retry.retries.total;method=" + FailSafeDummyService.class.getName() + ".isReady'",
                equalTo(retriesTotal));
        return this;
    }

    /**
     * Validate MP FT metric {@code ft.retry.calls.total} with {@code retryResult=maxRetriesReached}
     * for {@link FailSafeDummyService#isReady()}
     */
    public MetricsChecker validateRetryCallsTotalMaxRetriesReached(int maxRetriesReached) {
        response.body(
                "base.'ft.retry.calls.total;method=" + FailSafeDummyService.class.getName()
                        + ".isReady;retried=true;retryResult=maxRetriesReached'",
                equalTo(maxRetriesReached));
        return this;
    }

    /**
     * Validate MP FT metric {@code ft.retry.calls.total} for {@link FailSafeDummyService#isReady()}
     */
    public MetricsChecker validateRetryCallsSucceededNotTriedTotal(int callsSucceededNotTriedTotal) {
        response.body(
                "base.'ft.retry.calls.total;method=" + FailSafeDummyService.class.getName()
                        + ".isReady;retried=false;retryResult=valueReturned'",
                equalTo(callsSucceededNotTriedTotal));
        return this;
    }

    /**
     * Validate MP FT metric {@code ft.invocations.total} with {@code fallback=notApplied}
     * for {@link FailSafeDummyService#isReady()}
     */
    public MetricsChecker validateInvocationsTotal(int invocationsTotal) {
        validateInvocationsTotal(invocationsTotal, false);
        return this;
    }

    /**
     * Validate MP FT metric {@code ft.invocations.total} for {@link FailSafeDummyService#isReady()}
     */
    public MetricsChecker validateInvocationsTotal(int invocationsTotal, boolean fallbackApplied) {
        response.body("base.'ft.invocations.total;fallback=" + (fallbackApplied ? "applied" : "notApplied") +
                ";method=" + FailSafeDummyService.class.getName() + ".isReady;result=valueReturned'",
                equalTo(invocationsTotal));
        return this;
    }
}
