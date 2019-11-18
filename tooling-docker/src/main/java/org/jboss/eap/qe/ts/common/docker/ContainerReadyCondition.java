package org.jboss.eap.qe.ts.common.docker;

@FunctionalInterface
public interface ContainerReadyCondition {

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
