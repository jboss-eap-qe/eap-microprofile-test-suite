package org.jboss.eap.qe.ts.common.docker;

import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface ContainerReadyCondition {

    /**
     * Waits until isReady() returns true otherwise exception is thrown.
     * <p>
     * Usually this is checked by checking exposed port or calling health check inside docker container.
     */
    default void waitUntilReady(long timeoutInMillis) throws Exception {
        long startTime = System.currentTimeMillis();
        while (!(new CompletableFuture().supplyAsync(() -> isReady())).get()) {
            if (System.currentTimeMillis() - startTime > timeoutInMillis) {
                throw new Exception("Container was not ready in timeout : " + timeoutInMillis + " ms");
            }
        }
    }

    /**
     * Returns true if container is ready. Usually this checked by calling health check on running container
     * or checking opened ports.
     * <p>
     * Note that this method is expected to be short running (<1 second)
     *
     * @return true if container is ready, otherwise false
     */
    boolean isReady();
}
