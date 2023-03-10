package org.jboss.eap.qe.microprofile.metrics.hello;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.Random;

import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.ConcurrentGauge;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Metered;
import org.eclipse.microprofile.metrics.annotation.Timed;

@ApplicationScoped
public class HelloService {
    @Counted(name = "hello-count", absolute = true, displayName = "Hello Count", description = "Number of hello invocations")
    @Timed(unit = MetricUnits.MILLISECONDS, name = "hello-time", absolute = true, displayName = "Hello Time", description = "Time of hello invocations")
    @Metered(name = "hello-freq", absolute = true, displayName = "Hello Freq", description = "Frequency of hello invocations")
    @ConcurrentGauge(name = "hello-invocations", absolute = true, displayName = "Hello Invocations", description = "Number of current hello invocations")
    public String hello() throws InterruptedException {
        Thread.sleep(new Random().nextInt(100) + 1);
        return "Hello from counted and timed and metered and concurrent-gauged method";
    }
}
