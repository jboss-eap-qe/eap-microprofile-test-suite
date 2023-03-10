package org.jboss.eap.qe.microprofile.fault.tolerance.deployments.v10;

import jakarta.inject.Inject;

import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;

/**
 * Class providing @Fallback medhod for {@link HelloService} service
 */
public class HelloFallback implements FallbackHandler<String> {
    @Inject
    private MyContext context;

    @Override
    public String handle(ExecutionContext context) {
        // WFLY-12982 - with @Timeout application scoped CDI context is not propagated, thus do not use it
        if ("timeout".equals(context.getMethod().getName())) {
            return "Fallback Hello, context = foobar";
        }
        return "Fallback Hello, context = " + this.context.getValue();
    }
}
