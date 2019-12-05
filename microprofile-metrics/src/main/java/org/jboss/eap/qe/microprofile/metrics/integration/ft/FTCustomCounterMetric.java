package org.jboss.eap.qe.microprofile.metrics.integration.ft;

import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Metric;

@ApplicationScoped
public class FTCustomCounterMetric implements Metric, Counter {

    private long counter = 0;

    @Inject
    FTCustomCounterIncrementProviderService provider;

    @Override
    public void inc() {
        try {
            counter += provider.getIncrement();
        } catch (IOException e) {
            throw new RuntimeException("Service should be fail safe!");
        }
    }

    @Override
    public void inc(long l) {
        try {
            counter += l + provider.getIncrement();
        } catch (IOException e) {
            throw new RuntimeException("Service should be fail safe!");
        }
    }

    @Override
    public long getCount() {
        return counter;
    }
}
