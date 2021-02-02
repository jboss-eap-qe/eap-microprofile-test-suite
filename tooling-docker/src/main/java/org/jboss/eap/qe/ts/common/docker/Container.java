package org.jboss.eap.qe.ts.common.docker;

/**
 * Describes public interface available for a container (not an Arquillian one but the operating system level
 * virtualization unit).
 */
public interface Container {

    /**
     * Start a previously stopped or killed container
     */
    void start() throws ContainerStartException;

    /**
     * Check if container is running. Container in running state doesn't mean that the initialization is done and
     * container is ready for work.
     * 
     * @return Returns true if docker container is running.
     */
    boolean isRunning();

    /**
     * Stop this docker container.
     * 
     * @throws ContainerStopException thrown when the stop fails. This might mean that the container is still running.
     */
    void stop() throws ContainerStopException;

    /**
     * Kill this container.
     * 
     * @throws ContainerKillException thrown when the kill fails. This might mean that the container is still running.
     */
    void kill() throws ContainerKillException;

}
