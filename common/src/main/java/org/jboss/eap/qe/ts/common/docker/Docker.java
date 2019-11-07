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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 * Utility class for starting docker containers. This class allow to start any docker container before the test.
 *
 * Intended to be used as a JUnit {@link org.junit.ClassRule ClassRule}.
 *
 * Example of usage:
 *
 * import org.jboss.arquillian.container.test.api.RunAsClient;
 * import org.jboss.arquillian.junit.Arquillian;
 * import org.jboss.arquillian.junit.InSequence;
 * import org.jboss.eap.qe.ts.common.docker.Docker;
 * import org.jboss.eap.qe.ts.common.docker.DockerContainers;
 * import org.junit.ClassRule;
 * import org.junit.Test;
 * import org.junit.runner.RunWith;
 *
 * @RunAsClient
 * @RunWith(Arquillian.class)
 * public class TestDocker {
 *
 * // Docker container will be started in @BeforeClass phase - see list and current configuration of current {@link DockerContainers}
 * @ClassRule
 * public static Docker jaegerContainer=DockerContainers.jaeger();
 *
 *
 * @Test
 * public void test()  {
 *  // test something with docker container...
 * }
 * }
 */
public class Docker extends ExternalResource {
    private final String uuid;
    private final String name;
    private final String image;
    private final List<String> ports = new ArrayList<>();
    private final Map<String, String> environmentVariables = new HashMap<>();
    private final List<String> commandArguments = new ArrayList<>();
    private String awaitedLogLine;
    private long waitTimeoutInMillis = 600_000; // 10 minutes

    private final ExecutorService outputPrinter = Executors.newSingleThreadExecutor();

    public Docker(String name, String image) {
        this.uuid = UUID.randomUUID().toString();
        this.name = name;
        this.image = image;
    }

    public Docker waitForLogLine(String logWait) {
        this.awaitedLogLine = logWait;
        return this;
    }

    public Docker setWaitForLogLineTimeout(long timeoutInMillis) {
        this.waitTimeoutInMillis = timeoutInMillis;
        return this;
    }

    public Docker addPort(String port) {
        this.ports.add(port);
        return this;
    }

    public Docker addEnvVar(String key, String value) {
        environmentVariables.put(key, value);
        return this;
    }

    public Docker addCmdArg(String commandArgument) {
        commandArguments.add(commandArgument);
        return this;
    }

    private void start() throws IOException, TimeoutException, InterruptedException {
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

        CountDownLatch latch = new CountDownLatch(1);

        outputPrinter.execute(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(dockerRunProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(Ansi.ansi().fgCyan().a(name).reset().a("> ").a(line));

                    if (awaitedLogLine != null && line.contains(awaitedLogLine)) {
                        latch.countDown();
                    }
                }
            } catch (IOException ignored) {
                // ignore as any stop of docker container breaks the reader stream
                // note that shutdown of docker would be already logged
            }
        });

        if (awaitedLogLine != null) {
            if (latch.await(waitTimeoutInMillis, TimeUnit.MILLISECONDS)) {
                return;
            }

            try {
                stop();
            } catch (Exception ignored) {
                // stop() is calling "docker stop/rm ..." command, any exception thrown here does not provide any useful information thus ignored
            }

            throw new TimeoutException("Container '" + name + " (" + uuid + ")' didn't print '"
                    + awaitedLogLine + "' in " + waitTimeoutInMillis + " ms");
        }
    }

    private void stop() throws IOException, InterruptedException {
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

    // ---

    @Override
    protected void before() throws Throwable {
        start();
    }

    @Override
    protected void after() {
        try {
            stop();
        } catch (IOException | InterruptedException e) {
            System.out.println(Ansi.ansi().reset().a("Failed stopping container ").fgCyan().a(name).reset()
                    .a(" with ID ").fgYellow().a(uuid).reset());
        }
    }
}
