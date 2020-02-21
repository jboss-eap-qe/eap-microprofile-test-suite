package org.jboss.eap.qe.microprofile.tooling.performance.core;

import java.util.ArrayList;
import java.util.List;

/**
 * A stress tester that registers measurements as instances of {@link MemoryUsageRecord} and uses a
 * {@link Gauge} that accepts this data type
 * 
 * @param <R> The data type to be used to register measurements
 * @param <G> The gauge type to be used to perform measurements
 */
public class StressTester<R extends MeasurementRecord, G extends Gauge<R>> {

    private final G gauge;
    private final List<R> collectedValues = new ArrayList<>();

    public StressTester(G gauge) {
        this.gauge = gauge;
    }

    public void probe() throws MeasurementException {
        collectedValues.add(gauge.measure());
    }

    public void reset() {
        collectedValues.clear();
    }

    public List<R> getCollectedValues() {
        return collectedValues;
    }

    public void executeSession(StressTestProtocol protocol) throws StressTestException {
        protocol.run(this);
    }
}
