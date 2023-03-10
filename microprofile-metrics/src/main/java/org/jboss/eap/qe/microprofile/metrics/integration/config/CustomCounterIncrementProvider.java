package org.jboss.eap.qe.microprofile.metrics.integration.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class CustomCounterIncrementProvider {

    @Inject
    @ConfigProperty(name = "dummy.increment")
    private Provider<Integer> increment;

    public int getIncrement() {
        return increment.get();
    }
}
