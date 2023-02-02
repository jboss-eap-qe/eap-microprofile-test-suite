package org.jboss.eap.qe.ts.common.docker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
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
public class Docker extends ExternalResource {

    private String uuid;
    private String name;
    private String image;
    private List<String> ports = new ArrayList<>();
    private Map<String, String> environmentVariables = new HashMap<>();
    private List<String> options = new ArrayList<>();
    private List<String> commandArguments = new ArrayList<>();
    private ContainerReadyCondition containerReadyCondition;
    private long containerReadyTimeout;
    private ExecutorService outputPrinter;
    private Process dockerRunProcess;

    public static final String DOCKER_CMD = System.getProperty("docker.command", "docker");

    private Docker() {
    } // avoid instantiation, use Builder

    public void start() throws Exception {

        checkDockerPresent();

        List<String> cmd = new ArrayList<>();

        cmd.add(DOCKER_CMD);
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

        System.out.println(Ansi.ansi().reset().a("Starting container ").fgCyan().a(name).reset()
                .a(" with ID ").fgYellow().a(uuid).reset());

        dockerRunProcess = new ProcessBuilder()
                .redirectErrorStream(true)
                .command(cmd)
                .start();
        outputPrinter = Executors.newSingleThreadExecutor();
        outputPrinter.execute(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(dockerRunProcess.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(Ansi.ansi().fgCyan().a(name).reset().a("> ").a(line));
                }
            } catch (IOException ignored) {
                // ignore as any stop of docker container breaks the reader stream
                // note that shutdown of docker would be already logged
            }
        });

        long startTime = System.currentTimeMillis();
        while (!isContainerReady()) {
            if (System.currentTimeMillis() - startTime > containerReadyTimeout) {
                stop();
                throw new DockerTimeoutException(uuid + " - Container was not ready in " + containerReadyTimeout + " ms");
            }
            // fail fast mechanism in case of malformed docker command, for example bad arguments, invalid format of port mapping, image version,...
            if (!dockerRunProcess.isAlive() && dockerRunProcess.exitValue() != 0) {
                throw new DockerException(uuid + " - Starting of docker container using command: \"" + String.join(" ", cmd)
                        + "\" failed. Check that provided command is correct.");
            }
        }
    }

    private boolean isContainerReady() throws Exception {
        CompletableFuture<Boolean> condition = new CompletableFuture().supplyAsync(() -> containerReadyCondition.isReady());
        try {
            return condition.get(containerReadyTimeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            stop();
            // in case condition hangs interrupt it so there are no zombie threads
            condition.cancel(true);

            throw new ContainerReadyConditionException(uuid + " - Provided ContainerReadyCondition.isReady() method took " +
                    "longer than containerReadyTimeout: " + containerReadyTimeout + " ms. Check it does not hang and does " +
                    "not take longer then containerReadyTimeout. It's expected that ContainerReadyCondition.isReady() method " +
                    "is short lived (takes less than 1 second).", ex);
        }
    }

    private void checkDockerPresent() throws Exception {
        Process dockerInfoProcess = new ProcessBuilder()
                .redirectErrorStream(true)
                .command(new String[] { DOCKER_CMD, "info" })
                .start();
        dockerInfoProcess.waitFor();
        if (dockerInfoProcess.exitValue() != 0) {
            throw new DockerException("Docker is either not present or not installed on this machine. It must be installed " +
                    "and started up for executing tests with docker container.");
        }
    }

    /**
     * @return Returns true if docker container is running. It does NOT check whether container is ready.
     */
    public boolean isRunning() throws Exception {
        Process dockerRunProcess = new ProcessBuilder()
                .redirectErrorStream(true)
                .command(new String[] { DOCKER_CMD, "ps" })
                .start();

        dockerRunProcess.waitFor();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(dockerRunProcess.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(uuid)) {
                    return true;
                }
            }
        } catch (IOException ignored) {
            // ignore as any stop of docker container breaks the reader stream
            // note that shutdown of docker would be already logged
        }
        return false;
    }

    public void stop() throws Exception {
        System.out.println(Ansi.ansi().reset().a("Stopping container ").fgCyan().a(name).reset()
                .a(" with ID ").fgYellow().a(uuid).reset());

        new ProcessBuilder()
                .command(DOCKER_CMD, "stop", uuid)
                .start()
                .waitFor(10, TimeUnit.SECONDS);
        terminateThreadPools();
        removeDockerContainer();
    }

    public void kill() throws Exception {
        System.out.println(Ansi.ansi().reset().a("Killing container ").fgCyan().a(name).reset()
                .a(" with ID ").fgYellow().a(uuid).reset());

        new ProcessBuilder()
                .command(DOCKER_CMD, "kill", uuid)
                .start()
                .waitFor(10, TimeUnit.SECONDS);
        terminateThreadPools();
        removeDockerContainer();
    }

    private void terminateThreadPools() throws Exception {
        outputPrinter.shutdown();
        outputPrinter.awaitTermination(10, TimeUnit.SECONDS);
    }

    private void removeDockerContainer() throws Exception {
        new ProcessBuilder()
                .command(DOCKER_CMD, "rm", uuid)
                .start()
                .waitFor(10, TimeUnit.SECONDS);
    }

    @Override
    protected void before() throws Throwable {
        System.out.println(
                Ansi.ansi().reset().a("About to run Docker initialization: ").fgBrightGreen().a(name).reset());
        start();
        System.out.println(
                Ansi.ansi().reset().a("Docker initialization was successfully run: ").fgBrightGreen().a(name).reset());
    }

    @Override
    protected void after() {
        System.out.println(
                Ansi.ansi().reset().a("About to run Docker finalization: ").fgBrightGreen().a(name).reset());
        try {
            stop();
        } catch (Exception e) {
            System.out.println(Ansi.ansi().reset().a("Failed stopping container ").fgCyan().a(name).reset()
                    .a(" with ID ").fgYellow().a(uuid).reset());
        }
        System.out.println(
                Ansi.ansi().reset().a("Docker finalization was successfully run: ").fgBrightGreen().a(name).reset());
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
         * See "<docker_command> run --help" for full list of options
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
         * Builds instance of Docker class.
         *
         * @return build Docker instance
         */
        public Docker build() {
            Docker docker = new Docker();
            docker.uuid = this.uuid;
            docker.name = this.name;
            docker.image = this.image;
            docker.ports = this.ports;
            docker.options = this.options;
            docker.environmentVariables = this.environmentVariables;
            docker.commandArguments = this.commandArguments;
            docker.containerReadyCondition = containerReadyCondition;
            docker.containerReadyTimeout = containerReadyTimeoutInMillis;
            return docker;
        }
    }
}
