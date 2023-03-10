package org.jboss.eap.qe.microprofile.metrics.integration.ft;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.io.IOException;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class FTCustomCounterIncrementProvider {

    @Inject
    @ConfigProperty(name = "dummy.increment")
    private Provider<Integer> dependantIncrement;

    @Inject
    @ConfigProperty(name = "dummy.corrupted")
    private Provider<Boolean> corrupted;

    public int getIncrement() throws IOException {
        if (corrupted.get()) {
            throw new IOException("I am bad bad provider.");
        }
        return dependantIncrement.get();
    }
}
