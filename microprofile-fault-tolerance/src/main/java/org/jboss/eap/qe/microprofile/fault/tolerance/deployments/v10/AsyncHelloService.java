package org.jboss.eap.qe.microprofile.fault.tolerance.deployments.v10;

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.io.IOException;
import java.util.concurrent.Future;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Class providing service with MP FT @Asynchronous calls.
 */
@ApplicationScoped
public class AsyncHelloService {
    @Asynchronous
    @Timeout
    @Fallback(AsyncHelloFallback.class)
    public Future<String> timeout(boolean fail) throws InterruptedException {
        if (fail) {
            // default @Timeout is 1000ms thus this will throw TimeoutException and @Fallback method will be called
            Thread.sleep(2000);
        }

        return completedFuture("Hello from @Timeout method");
    }

    @Asynchronous
    @Retry
    @Fallback(AsyncHelloFallback.class)
    public Future<String> retry(boolean fail) throws IOException {
        if (fail) {
            throw new IOException("Simulated IO error");
        }

        return completedFuture("Hello from @Retry method");
    }

    @Asynchronous
    @CircuitBreaker
    @Fallback(AsyncHelloFallback.class)
    public Future<String> circuitBreaker(boolean fail) throws IOException {
        if (fail) {
            throw new IOException("Simulated IO error");
        }

        return completedFuture("Hello from @CircuitBreaker method");
    }

    @Asynchronous
    @Bulkhead
    @Fallback(AsyncHelloFallback.class)
    public Future<String> bulkhead(boolean fail) throws InterruptedException {
        if (fail) {
            // delay the call to simulate real execution logic
            Thread.sleep(2000);
        }

        return completedFuture("Hello from @Bulkhead method");
    }
}
