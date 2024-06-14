package org.jboss.eap.qe.micrometer.base;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;

/**
 * @author <a href="mailto:jasondlee@redhat.com">Jason Lee</a>
 */
@RequestScoped
@Path("/")
public class MetricResource {
    @Inject
    private MeterRegistry meterRegistry;
    private Counter counter;

    @PostConstruct
    public void setupMeters() {
        counter = meterRegistry.counter("demo_counter");
    }

    @GET
    @Path("/")
    public double getCount() {
        Timer timer = meterRegistry.timer("demo_timer", Tags.of("ts", "" + System.currentTimeMillis()));

        timer.record(() -> {
            try {
                Thread.sleep((long) (Math.random() * 100L));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            counter.increment();
        });

        return counter.count();
    }
}
