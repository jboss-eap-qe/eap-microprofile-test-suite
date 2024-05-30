package org.jboss.eap.qe.micrometer.multiple;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@RequestScoped
@Path("/" + DuplicateMetricResource1.TAG)
public class DuplicateMetricResource1 {
    public static final String TAG = "app1";
    public static final String METER_NAME = "ping_count";
    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private MeterRegistry meterRegistry;
    private Counter counter;

    @PostConstruct
    public void setupMeters() {
        counter = meterRegistry.counter(METER_NAME, "app", TAG);
    }

    @GET
    @Path("/")
    public String ping() {
        counter.increment();
        return "ping";
    }
}
