package org.jboss.eap.qe.microprofile.metrics.hello;

import java.util.Random;

import org.eclipse.microprofile.metrics.annotation.Counted;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AnotherHelloService {
    @Counted(name = "hello-count", absolute = true, displayName = "Hello Count", description = "Number of hello invocations")
    public String hello() throws InterruptedException {
        Thread.sleep(new Random().nextInt(100) + 1);
        return "Hello from another counted method";
    }
}
