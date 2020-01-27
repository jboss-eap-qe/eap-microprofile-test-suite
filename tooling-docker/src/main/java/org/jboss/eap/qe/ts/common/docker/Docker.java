package org.jboss.eap.qe.ts.common.docker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.fusesource.jansi.Ansi;
import org.junit.rules.ExternalResource;

/**
 * Utility class for starting docker containers. This class allows to start any docker container.
 * <p>
 * Intended to be used as a JUnit @ClassRule. Example of usage - see org.jboss.eap.qe.ts.common.docker.DockerTest test
 */
public class Docker extends ExternalResource implements Container {

    private final String uuid;
    private final String name;
    private final String image;
    private final List<String> ports;
    private final Map<String, String> environmentVariables;
    private final List<String> options;
    private final List<String> commandArguments;
    private final ContainerReadyCondition containerReadyCondition;
    private final long containerReadyTimeout;
    private final PrintStream out;

    private ExecutorService outputPrintingThread;

    private Docker(Builder builder) {
        this.uuid = builder.uuid;
        this.name = builder.name;
        this.image = builder.image;
        this.ports = builder.ports;
        this.options = builder.options;
        this.environmentVariables = builder.environmentVariables;
        this.commandArguments = builder.commandArguments;
        this.containerReadyCondition = builder.containerReadyCondition;
        this.containerReadyTimeout = builder.containerReadyTimeoutInMillis;
        this.out = builder.out;
    }

    @Override
    public void start() throws ContainerStartException {
        if (isRunning()) {
            throw new ContainerStartException("Container '" + this.uuid + "' is already running!");
        }
        if (!containerExists()) {
            throw new ContainerStartException("Container '" + this.uuid + "' doesn't exist!");
        }
        try {
            final Process startProcess = runDockerCommand("start", this.uuid);
            this.outputPrintingThread = startPrinterThread(startProcess, this.out, this.name);
            final long startTime = System.currentTimeMillis();
            while (!isContainerReady(this.uuid, this.containerReadyCondition)) {
                if (System.currentTimeMillis() - startTime > containerReadyTimeout) {
                    stop();
                    remove();
                    throw new ContainerStartException(
                            "Container '" + this.uuid + "' was not ready in " + this.containerReadyTimeout + " ms");
                }
                // fail fast mechanism in case of malformed docker command, for example bad arguments, invalid format of port mapping, image version,...
                if (!startProcess.isAlive() && startProcess.exitValue() != 0) {
                    throw new ContainerStartException("Failed to start '" + this.uuid + "' container!");
                }
            }
        } catch (DockerCommandException | ContainerReadyConditionException | ContainerStopException
                | ContainerRemoveException e) {
            throw new ContainerStartException("Failed to start container '" + this.uuid + "'!", e);
        }
    }

    @Override
    public void run() throws ContainerStartException {
        if (!isDockerPresent()) {
            throw new ContainerStartException("'docker' command is not present on this machine!");
        }

        this.out.println(Ansi.ansi().reset().a("Starting container ").fgCyan().a(name).reset()
                .a(" with ID ").fgYellow().a(uuid).reset());

        final List<String> dockerStartCommand = composeRunCommand();
        Process dockerRunProcess;
        try {
            dockerRunProcess = new ProcessBuilder()
                    .redirectErrorStream(true)
                    .command(dockerStartCommand)
                    .start();
        } catch (IOException e) {
            throw new ContainerStartException("Failed to start the '" + String.join(" ", dockerStartCommand) + "' command");
        }

        this.outputPrintingThread = startPrinterThread(dockerRunProcess, this.out, this.name);

        long startTime = System.currentTimeMillis();
        try {
            while (!isContainerReady(this.uuid, this.containerReadyCondition)) {
                if (System.currentTimeMillis() - startTime > containerReadyTimeout) {
                    stop();
                    remove();
                    throw new ContainerStartException(uuid + " - Container was not ready in " + containerReadyTimeout + " ms");
                }
                // fail fast mechanism in case of malformed docker command, for example bad arguments, invalid format of port mapping, image version,...
                if (!dockerRunProcess.isAlive() && dockerRunProcess.exitValue() != 0) {
                    throw new ContainerStartException(
                            uuid + " - Starting of docker container using command: \"" + String.join(" ", dockerStartCommand)
                                    + "\" failed. Check that provided command is correct.");
                }
            }
        } catch (ContainerStopException e) {
            throw new ContainerStartException("Unable to stop container after failed start!", e);
        } catch (ContainerRemoveException e) {
            throw new ContainerStartException("Unable to remove container after failed start!", e);
        } catch (ContainerReadyConditionException e) {
            throw new ContainerStartException("There was a problem when checking container readiness!", e);
        }
    }

    private List<String> composeRunCommand() {
        List<String> cmd = new ArrayList<>();

        cmd.add("docker");
        cmd.add("run");
        cmd.add("--name");
        cmd.add(uuid);

        for (String port : ports) {
            cmd.add("-p");
            cmd.add(port);
        }

        for (Map.Entry<String, String> envVar : environmentVariables.entrySet()) {
            cmd.add("-e");
            cmd.add(envVar.getKey() + "=" + envVar.getValue());
        }

        cmd.addAll(options);

        cmd.add(image);

        cmd.addAll(commandArguments);

        return cmd;
    }

    private ExecutorService startPrinterThread(final Process dockerRunProcess, final PrintStream out,
            final String containerName) {
        final ExecutorService outputPrinter = Executors.newSingleThreadExecutor();
        outputPrinter.execute(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(dockerRunProcess.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    out.println(Ansi.ansi().fgCyan().a(containerName).reset().a("> ").a(line));
                }
            } catch (IOException ignored) {
                // ignore as any stop of docker container breaks the reader stream
                // note that shutdown of docker would be already logged
            }
        });
        return outputPrinter;
    }

    private boolean isContainerReady(final String uuid, final ContainerReadyCondition condition)
            throws ContainerReadyConditionException {
        final CompletableFuture<Boolean> conditionFuture = CompletableFuture.supplyAsync(condition::isReady);
        try {
            return conditionFuture.get(containerReadyTimeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            throw new ContainerReadyConditionException(uuid + " - Provided ContainerReadyCondition.isReady() method took " +
                    "longer than containerReadyTimeout: " + containerReadyTimeout + " ms. Check it does not hang and does " +
                    "not take longer then containerReadyTimeout. It's expected that ContainerReadyCondition.isReady() method " +
                    "is short lived (takes less than 1 second).", ex);
        } catch (ExecutionException e) {
            throw new ContainerReadyConditionException("There was an exception in thread which was checking the " +
                    "container readiness.", e);
        } catch (InterruptedException e) {
            throw new ContainerReadyConditionException("The thread waiting for container readiness was interrupted!", e);
        } finally {
            // in case condition hangs interrupt it so there are no zombie threads
            conditionFuture.cancel(true);
        }
    }

    private boolean isDockerPresent() {
        try {
            final int processExitValue = runDockerCommand("info").exitValue();
            return processExitValue == 0;
        } catch (DockerCommandException e) {
            throw new IllegalStateException("There was an exception when checking for docker command presence!", e);
        }
    }

    /**
     * @return Returns true if docker container is running. It does NOT check whether container is ready.
     */
    public boolean isRunning() {
        Process dockerRunProcess;
        try {
            dockerRunProcess = runDockerCommand("ps");
        } catch (DockerCommandException ignored) {
            //when we cannot start the process for making the check container is not running
            return false;
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(dockerRunProcess.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(this.uuid)) {
                    return true;
                }
            }
        } catch (IOException ignored) {
            // ignore as any stop of docker container breaks the reader stream
            // note that shutdown of docker would be already logged
        }
        return false;
    }

    /**
     * @return Returns true if container exists.
     */
    private boolean containerExists() {
        Process dockerRunProcess;
        try {
            dockerRunProcess = runDockerCommand("ps", "-a");
        } catch (DockerCommandException ignored) {
            //when we cannot start the process for making the check container is not running
            return false;
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(dockerRunProcess.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(this.uuid)) {
                    return true;
                }
            }
        } catch (IOException ignored) {
            // ignore as any stop of docker container breaks the reader stream
            // note that shutdown of docker would be already logged
        }
        return false;
    }

    /**
     * Stop this docker container using docker command.
     * 
     * @throws ContainerStopException thrown when the stop command fails. This generally means that the command wasn't
     *         successful and container might be still running.
     */
    public void stop() throws ContainerStopException {
        this.out.println(Ansi.ansi().reset().a("Stopping container ").fgCyan().a(this.name).reset()
                .a(" with ID ").fgYellow().a(this.uuid).reset());

        try {
            runDockerCommand("stop", this.uuid);
        } catch (DockerCommandException e) {
            throw new ContainerStopException("Failed to stop container '" + this.uuid + "'!", e);
        }

        try {
            terminatePrintingThread();
        } catch (InterruptedException e) {
            throw new ContainerStopException("A thread was interrupted while waiting for its termination!", e);
        }
    }

    /**
     * Kill this docker container using docker command. Be aware that there might occur a situation when the docker
     * command might fail. This might be caused for example by reaching file descriptors limit in system.
     * 
     * @throws ContainerKillException thrown when the kill command fails.
     */
    public void kill() throws ContainerKillException {
        this.out.println(Ansi.ansi().reset().a("Killing container ").fgCyan().a(this.name).reset()
                .a(" with ID ").fgYellow().a(uuid).reset());

        try {
            runDockerCommand("kill", this.uuid);
        } catch (DockerCommandException e) {
            throw new ContainerKillException("Failed to kill the container '" + this.uuid + "'!", e);
        }

        try {
            terminatePrintingThread();
        } catch (InterruptedException e) {
            throw new ContainerKillException("Interrupted when waiting for printer thread termination!", e);
        }
    }

    private void terminatePrintingThread() throws InterruptedException {
        if (outputPrintingThread != null) {
            outputPrintingThread.shutdown();
            outputPrintingThread.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    @Override
    public void remove() throws ContainerRemoveException {
        try {
            runDockerCommand("rm", this.uuid);
        } catch (DockerCommandException e) {
            throw new ContainerRemoveException("Failed to remove the container '" + this.uuid + "'!", e);
        }
    }

    private Process runDockerCommand(final String... commandArguments) throws DockerCommandException {
        final String dockerCommand = "docker";
        final List<String> cmd = new ArrayList<>();
        cmd.add(dockerCommand);
        Collections.addAll(cmd, commandArguments);
        try {
            final Process process;

            process = new ProcessBuilder()
                    .command(cmd)
                    .start();

            process.waitFor(10, TimeUnit.SECONDS);

            return process;
        } catch (InterruptedException e) {
            throw new DockerCommandException("Interrupted while waiting for '" + String.join(" ", cmd) + "' to return!", e);
        } catch (IOException e) {
            throw new DockerCommandException("Failed to start command '" + String.join(" ", cmd) + "'!", e);
        }
    }

    @Override
    protected void before() throws ContainerStartException {
        run();
    }

    @Override
    protected void after() {
        try {
            stop();
            remove();
        } catch (ContainerStopException e) {
            throw new IllegalStateException("Failed to stop container '" + this.uuid + "'! ", e);
        } catch (ContainerRemoveException e) {
            throw new IllegalStateException("Failed to remove container '" + this.uuid + "'! ", e);
        }
    }

    public static class Builder {
        private String uuid;
        private String name;
        private String image;
        private List<String> ports = new ArrayList<>();
        private Map<String, String> environmentVariables = new HashMap<>();
        private List<String> options = new ArrayList<>();
        private List<String> commandArguments = new ArrayList<>();
        private long containerReadyTimeoutInMillis = 120_000; // 2 minutes
        private PrintStream out = System.out;

        // by default - do not make any check
        private ContainerReadyCondition containerReadyCondition = () -> true;

        public Builder(String name, String image) {
            this.uuid = name + "-" + UUID.randomUUID().toString();
            this.name = name;
            this.image = image;
        }

        /**
         * Timeout to wait until container is ready/starts
         *
         * @param timeout the maximum time to wait
         * @param unit the time unit of the {@code timeout} argument
         */
        public Builder setContainerReadyTimeout(long timeout, TimeUnit unit) {
            this.containerReadyTimeoutInMillis = unit.toMillis(timeout);
            return this;
        }

        /**
         * Adds port mapping exposed by docker container. For example "8080:80" maps
         * port 80 in the container to port 8080 on the Docker host.
         *
         * @param portMapping port mapping as defined by docker command, for exampple "8080:80" or "8080:80/tcp"
         */
        public Builder withPortMapping(String portMapping) {
            this.ports.add(portMapping);
            return this;
        }

        /**
         * Adds environment variable passed to docker container
         *
         * @param key name of environment variable
         * @param value value of environment variable
         */
        public Builder withEnvVar(String key, String value) {
            environmentVariables.put(key, value);
            return this;
        }

        /**
         * Adds options into starting docker command.
         * <p>
         * See "docker run --help" for full list of options
         *
         * @param option additional docker parameters
         */
        public Builder withCmdOption(String option) {
            options.add(option);
            return this;
        }

        /**
         * Adds parameters into starting docker command
         *
         * @param commandArgument additional docker parameters
         */
        public Builder withCmdArg(String commandArgument) {
            commandArguments.add(commandArgument);
            return this;
        }

        /**
         * Sets condition/check returning true if container is ready.
         *
         * @param containerReadyCondition condition returning true when container is ready, false otherwise
         */
        public Builder setContainerReadyCondition(ContainerReadyCondition containerReadyCondition) {
            this.containerReadyCondition = containerReadyCondition;
            return this;
        }

        /**
         * Set default output stream to which the container stdout will be written. Default is System.out.
         * 
         * @param out an output stream
         * @return instance of this builder
         */
        public Builder standardOutputStream(final PrintStream out) {
            this.out = out;
            return this;
        }

        /**
         * Builds instance of Docker class.
         *
         * @return build Docker instance
         */
        public Docker build() {
            return new Docker(this);
        }
    }
}
