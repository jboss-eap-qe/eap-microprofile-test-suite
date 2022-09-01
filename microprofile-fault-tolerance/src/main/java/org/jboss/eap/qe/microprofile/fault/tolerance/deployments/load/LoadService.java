package org.jboss.eap.qe.microprofile.fault.tolerance.deployments.load;

import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Service with MP FT annotations to verify correct work under high CPU load.
 */
@ApplicationScoped
public class LoadService {

    @Timeout(5000)
    public String timeout() throws InterruptedException {
        Thread.sleep(10000);
        return "Hello from @Timeout method";
    }

    /**
     * Retry 10 times, takes ~2000 ms of retrying (as there is 200ms delay) in total before throwing Exception
     *
     */
    @Retry(maxRetries = 10, delay = 200, retryOn = Exception.class)
    public String retry() throws Exception {
        throw new Exception("Exception from @Retry method.");
    }

    @Timeout(5000)
    @Fallback(fallbackMethod = "fallback")
    public String timeoutWithFallback() throws Exception {
        Thread.sleep(10000);
        return "Hello from @Timeout method";
    }

    public String fallback() {
        return "Hello from @Fallback method";
    }
}
