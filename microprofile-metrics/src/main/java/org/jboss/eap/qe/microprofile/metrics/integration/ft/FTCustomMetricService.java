package org.jboss.eap.qe.microprofile.metrics.integration.ft;

import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.annotation.RegistryType;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class FTCustomMetricService {

    @Inject
    @RegistryType(type = MetricRegistry.Type.APPLICATION)
    MetricRegistry metricRegistry;

    public String hello() {
        metricRegistry.counter("ft-custom-metric").inc();
        return "Hello from custom metric fault-tolerant service!";
    }
}
