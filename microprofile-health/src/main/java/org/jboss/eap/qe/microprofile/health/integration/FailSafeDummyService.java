package org.jboss.eap.qe.microprofile.health.integration;

import java.io.IOException;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.metrics.annotation.Counted;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

@ApplicationScoped
public class FailSafeDummyService {

    public static final int MAX_RETRIES = 5;

    public static final String LIVE_CONFIG_PROPERTY = "dummy.live";
    public static final String READY_CONFIG_PROPERTY = "dummy.ready";
    public static final String IN_MAINTENANCE_CONFIG_PROPERTY = "dummy.in_maintenance";
    public static final String READY_IN_MAINTENANCE_CONFIG_PROPERTY = "dummy.in_maintenance.ready";

    @Inject
    @ConfigProperty(name = READY_CONFIG_PROPERTY)
    private Provider<Boolean> ready;

    @Inject
    @ConfigProperty(name = LIVE_CONFIG_PROPERTY)
    private Provider<Boolean> live;

    @Inject
    @ConfigProperty(name = READY_IN_MAINTENANCE_CONFIG_PROPERTY)
    private Provider<Boolean> readyInMaintenance;

    @Inject
    @ConfigProperty(name = IN_MAINTENANCE_CONFIG_PROPERTY)
    private Provider<Boolean> inMaintenance;

    @Inject
    FailSafeDummyService service;

    public boolean isLive() {
        return live.get();
    }

    @Fallback(fallbackMethod = "isReadyFallback")
    @Retry(maxRetries = MAX_RETRIES)
    public boolean isReady() throws IOException {
        service.simulateOpeningResources();
        return ready.get();
    }

    public boolean isReadyFallback() {
        return readyInMaintenance.get();
    }

    @Counted(name = "simulation-count", absolute = true, displayName = "Simulation Count", description = "Number of simulateOpeningResources invocations")
    public void simulateOpeningResources() throws IOException {
        if (inMaintenance.get()) {
            throw new IOException("In Maintenance");
        }
    }
}
