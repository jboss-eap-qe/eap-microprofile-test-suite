package org.jboss.eap.qe.microprofile.metrics.integration.ft;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Provider;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class FTCustomCounterIncrementFailSafeProvider {

    @Inject
    @ConfigProperty(name = "dummy.failsafe.increment")
    private Provider<Integer> increment;

    public int getIncrement() {
        return increment.get();
    }
}
