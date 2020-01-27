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
     * Create and start new container
     * 
     * @throws ContainerStartException thrown when start of container fails
     */
    void run() throws ContainerStartException;

    /**
     * @return Returns true if docker container is running. It does NOT check whether container is ready.
     */
    boolean isRunning();

    /**
     * Stop this docker container using docker command.
     * 
     * @throws ContainerStopException thrown when the stop command fails. This generally means that the command wasn't
     *         successful and container might be still running.
     */
    void stop() throws ContainerStopException;

    /**
     * Kill this docker container using docker command. Be aware that there might occur a situation when the docker
     * command might fail. This might be caused for example by reaching file descriptors limit in system.
     * 
     * @throws ContainerKillException thrown when the kill command fails.
     */
    void kill() throws ContainerKillException;

    /**
     * Remove a container from system
     * 
     * @throws ContainerRemoveException thrown when removing fails
     */
    void remove() throws ContainerRemoveException;

}
