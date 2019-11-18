package org.jboss.eap.qe.ts.common.docker;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.not;


public class MalformedDockerConfigTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testFailFastWithMalformedDockerCommand() throws Exception {
        Docker containerWithInvalidVersion = new Docker.Builder("wildfly", "jboss/wildfly:InvalidVersion")
                .setContainerReadyTimeout(10, TimeUnit.SECONDS) // shorten timeout as this should fail fast
                .setContainerReadyCondition(() -> false) // it's expected that server never starts and fails fast thus return false
                .build();

        thrown.expect(DockerException.class);
        thrown.expectMessage(containsString("Starting of docker container using command: \"docker run --name"));
        thrown.expectMessage(endsWith("failed. Check that provided command is correct."));

        containerWithInvalidVersion.start();
    }

    @Test
    public void testContainerWithHangingReadyCondition() throws Exception {
        Docker containerWithHangingReadyCondition = new Docker.Builder("wildfly", "jboss/wildfly:18.0.0.Final")
                .setContainerReadyTimeout(1, TimeUnit.SECONDS) // shorten timeout as this should fail fast
                .setContainerReadyCondition(() -> { // simulate hanging isReady() condition
                    try {
                        Thread.sleep(10000); // wait 10 s
                    } catch (InterruptedException e) {
                        // ignore
                    }
                    return false;
                }) // it's expected that server never starts and fails fast thus return false
                .build();

        thrown.expect(ContainerReadyConditionException.class);
        thrown.expectMessage(containsString("Provided ContainerReadyCondition.isReady() method took longer than containerReadyTimeout"));
        // throws expected Exception
        try {
            containerWithHangingReadyCondition.start();
        } finally {
            assertThat("ContainerReadyConditionException was thrown and starting container is expected to be stopped/killed. " +
                    "However this did not happen and there is still container running which is bug.", not(containerWithHangingReadyCondition.isRunning()));
        }

    }

    @Test
    public void testContainerReadyTimeout() throws Exception {
        Docker containerWithShortTimeout = new Docker.Builder("wildlfy", "jboss/wildfly:18.0.0.Final")
                .setContainerReadyTimeout(2, TimeUnit.SECONDS)
                .setContainerReadyCondition(() -> {
                    try {
                        URL url = new URL("http://" + "127.0.0.1:" + 9990 + "/health");
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setRequestMethod("GET");

                        return connection.getResponseMessage().contains("OK");
                    } catch (Exception ex) {
                        return false;
                    }
                })
                .withPortMapping("8080:8080")
                .withPortMapping("9990:9990")
                .withCmdArg("/opt/jboss/wildfly/bin/standalone.sh")
                .withCmdArg("-b=0.0.0.0")
                .withCmdArg("-bmanagement=0.0.0.0")
                .build();

        thrown.expect(DockerTimeoutException.class);
        thrown.expectMessage(containsString("Container was not ready in timeout"));

        try {
            containerWithShortTimeout.start(); // will timeout after 1 sec
        } finally {
            assertThat("DockerTimeoutException was thrown and starting container is expected to be stopped/killed. " +
                    "However this did not happen and there is still container running which is bug.", not(containerWithShortTimeout.isRunning()));
        }
    }
}
