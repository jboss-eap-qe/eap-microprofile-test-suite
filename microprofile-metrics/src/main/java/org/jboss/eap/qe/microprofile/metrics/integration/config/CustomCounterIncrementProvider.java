package org.jboss.eap.qe.microprofile.metrics.integration.config;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Provider;

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
