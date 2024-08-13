package org.jboss.eap.qe.ts.common.docker;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.not;
import static org.jboss.eap.qe.ts.common.docker.Docker.DOCKER_CMD;

import java.util.concurrent.TimeUnit;

import org.jboss.eap.qe.ts.common.docker.junit.DockerRequiredTests;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;

@Category(DockerRequiredTests.class)
public class MalformedDockerConfigTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testFailFastWithMalformedDockerCommand() throws Exception {
        Docker containerWithInvalidVersion = new Docker.Builder("wildfly",
                "quay.io/wildfly/wildfly:InvalidVersion")
                .setContainerReadyTimeout(2, TimeUnit.SECONDS) // shorten timeout as this should fail fast
                .setContainerReadyCondition(() -> false) // it's expected that server never starts and fails fast thus return false
                .withPortMapping("bad:mapping")
                .build();

        thrown.expect(DockerException.class);
        thrown.expectMessage(containsString("Starting of docker container using command: \"" + DOCKER_CMD + " run --name"));
        thrown.expectMessage(endsWith("failed. Check that provided command is correct."));

        containerWithInvalidVersion.start();
    }

    @Test
    public void testContainerWithHangingReadyCondition() throws Exception {
        Docker containerWithHangingReadyCondition = new Docker.Builder("wildfly",
                "quay.io/wildfly/wildfly")
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
        thrown.expectMessage(
                containsString("Provided ContainerReadyCondition.isReady() method took longer than containerReadyTimeout"));
        // throws expected Exception
        try {
            containerWithHangingReadyCondition.start();
        } finally {
            final boolean isRunning = containerWithHangingReadyCondition.isRunning();
            if (isRunning) {
                containerWithHangingReadyCondition.stop();
            }
            assertThat("ContainerReadyConditionException was thrown and starting container is expected to be stopped/killed. " +
                    "However this did not happen and there is still container running which is bug.",
                    not(isRunning));
        }

    }

    @Test
    public void testContainerReadyTimeout() throws Exception {
        Docker containerWithShortTimeout = new Docker.Builder("wildlfy", "quay.io/wildfly/wildfly")
                .setContainerReadyTimeout(2, TimeUnit.SECONDS)
                .setContainerReadyCondition(() -> false) // never ready
                .build();

        thrown.expect(DockerTimeoutException.class);
        thrown.expectMessage(containsString("Container was not ready in"));

        try {
            containerWithShortTimeout.start();
        } finally {
            final boolean isRunning = containerWithShortTimeout.isRunning();
            if (isRunning) {
                containerWithShortTimeout.stop();
            }
            assertThat("DockerTimeoutException was thrown and starting container is expected to be stopped/killed. " +
                    "However this did not happen and there is still container running which is bug.",
                    not(isRunning));
        }
    }
}
