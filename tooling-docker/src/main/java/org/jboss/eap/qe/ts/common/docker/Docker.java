package org.jboss.eap.qe.ts.common.docker;

import org.fusesource.jansi.Ansi;
import org.junit.rules.ExternalResource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
    private List<String> commandArguments = new ArrayList<>();
    private ContainerReadyCondition containerReadyCondition;
    private long containerReadyTimeout;
    private final ExecutorService outputPrinter = Executors.newSingleThreadExecutor();

    private Docker() {
    } // avoid instantiation, use Builder

    protected void start() throws Exception {
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

        cmd.add(image);

        cmd.addAll(commandArguments);

        System.out.println(Ansi.ansi().reset().a("Starting container ").fgCyan().a(name).reset()
                .a(" with ID ").fgYellow().a(uuid).reset());

        Process dockerRunProcess = new ProcessBuilder()
                .redirectErrorStream(true)
                .command(cmd)
                .start();

        outputPrinter.execute(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(dockerRunProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(Ansi.ansi().fgCyan().a(name).reset().a("> ").a(line));
                }
            } catch (IOException ignored) {
                // ignore as any stop of docker container breaks the reader stream
                // note that shutdown of docker would be already logged
            }
        });

        containerReadyCondition.waitUntilReady(containerReadyTimeout);
    }

    protected void stop() throws Exception {
        System.out.println(Ansi.ansi().reset().a("Stopping container ").fgCyan().a(name).reset()
                .a(" with ID ").fgYellow().a(uuid).reset());

        new ProcessBuilder()
                .command("docker", "stop", uuid)
                .start()
                .waitFor(10, TimeUnit.SECONDS);

        outputPrinter.shutdown();
        outputPrinter.awaitTermination(10, TimeUnit.SECONDS);

        new ProcessBuilder()
                .command("docker", "rm", uuid)
                .start()
                .waitFor(10, TimeUnit.SECONDS);
    }

    @Override
    protected void before() throws Throwable {
        start();
    }

    @Override
    protected void after() {
        try {
            stop();
        } catch (Exception e) {
            System.out.println(Ansi.ansi().reset().a("Failed stopping container ").fgCyan().a(name).reset()
                    .a(" with ID ").fgYellow().a(uuid).reset());
        }
    }

    public static class Builder {
        private String uuid;
        private String name;
        private String image;
        private List<String> ports = new ArrayList<>();
        private Map<String, String> environmentVariables = new HashMap<>();
        private List<String> commandArguments = new ArrayList<>();
        private long containerReadyTimeoutInMillis = 60_000; // 1 minutes

        // by default - do not make any check
        private ContainerReadyCondition containerReadyCondition = () -> true;

        public Builder(String name, String image) {
            this.uuid = UUID.randomUUID().toString();
            this.name = name;
            this.image = image;
        }

        /**
         * Timeout in millis to wait until container is ready/starts
         *
         * @param timeoutInMillis timeout in millis         *
         */
        public Builder setContainerReadyTimeout(long timeoutInMillis) {
            this.containerReadyTimeoutInMillis = timeoutInMillis;
            return this;
        }

        /**
         * Adds port mapping exposed by docker container. For example "8080:80" maps
         * port 80 in the container to port 8080 on the Docker host.
         *
         * @param portMapping port mapping as defined by docker command, for exampple "8080:80" or "8080:80/tcp"
         */
        public Builder wihtPortMapping(String portMapping) {
            this.ports.add(portMapping);
            return this;
        }

        /**
         * Adds environment variable passed to docker container
         *
         * @param key   name of environment variable
         * @param value value of environment variable
         */
        public Builder withEnvVar(String key, String value) {
            environmentVariables.put(key, value);
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
            docker.environmentVariables = this.environmentVariables;
            docker.commandArguments = this.commandArguments;
            docker.containerReadyCondition = containerReadyCondition;
            docker.containerReadyTimeout = containerReadyTimeoutInMillis;
            return docker;
        }
    }
}
