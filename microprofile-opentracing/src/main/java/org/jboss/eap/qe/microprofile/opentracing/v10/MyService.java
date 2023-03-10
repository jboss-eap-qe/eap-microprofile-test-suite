package org.jboss.eap.qe.microprofile.opentracing.v10;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.opentracing.Traced;

import io.opentracing.Tracer;

/**
 * A simple service with traced method. Each call of this method is logged by Jaeger tracer and can be analyzed afterwards.
 */
@ApplicationScoped
public class MyService {
    @Inject
    private Tracer tracer;

    @Traced
    public String hello() {
        tracer.activeSpan().log("Hello tracer");
        return "Hello from traced service";
    }
}
