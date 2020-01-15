package org.jboss.eap.qe.microprofile.fault.tolerance.deployments.v10;

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.concurrent.Future;

import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;

/**
 * Class providing @Fallback medhod for {@link AsyncHelloService} service
 */
public class AsyncHelloFallback implements FallbackHandler<Future<String>> {
    @Override
    public Future<String> handle(ExecutionContext context) {
        return completedFuture("Fallback Hello");
    }
}
