package org.jboss.eap.qe.microprofile.metrics.integration.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Metric;

@ApplicationScoped
public class CustomCounterMetric implements Metric, Counter {

    private long counter = 0;

    @Inject
    CustomCounterIncrementProvider provider;

    @Override
    public void inc() {
        counter += provider.getIncrement();
    }

    @Override
    public void inc(long l) {
        counter += l + provider.getIncrement();
    }

    @Override
    public long getCount() {
        return counter;
    }
}
