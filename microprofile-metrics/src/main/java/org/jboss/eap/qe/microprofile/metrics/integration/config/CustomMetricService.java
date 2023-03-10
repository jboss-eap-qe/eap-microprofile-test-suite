package org.jboss.eap.qe.microprofile.metrics.integration.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.annotation.RegistryType;

@ApplicationScoped
public class CustomMetricService {

    @Inject
    @RegistryType(type = MetricRegistry.Type.APPLICATION)
    MetricRegistry metricRegistry;

    public String hello() {
        metricRegistry.counter("custom-metric").inc();
        return "Hello from custom metric service!";
    }
}
