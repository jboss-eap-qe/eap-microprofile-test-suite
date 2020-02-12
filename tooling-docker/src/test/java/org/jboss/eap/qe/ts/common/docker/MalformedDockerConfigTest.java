package org.jboss.eap.qe.ts.common.docker;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;

import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class MalformedDockerConfigTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testFailFastWithMalformedDockerCommand() throws Exception {
        Docker containerWithInvalidVersion = new Docker.Builder("wildfly",
                "registry.hub.docker.com/jboss/wildfly:InvalidVersion")
                        .setContainerReadyTimeout(2, TimeUnit.SECONDS) // shorten timeout as this should fail fast
                        .setContainerReadyCondition(() -> false) // it's expected that server never starts and fails fast thus return false
                        .withPortMapping("bad:mapping")
                        .build();

        thrown.expect(ContainerStartException.class);
        thrown.expectMessage(containsString("Starting of docker container using command: \"docker run --name"));
        thrown.expectMessage(endsWith("failed. Check that provided command is correct."));

        containerWithInvalidVersion.start();
    }

    @Test
    public void testContainerWithHangingReadyCondition() throws Exception {
        Docker containerWithHangingReadyCondition = new Docker.Builder("wildfly",
                "registry.hub.docker.com/jboss/wildfly:18.0.0.Final")
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

        thrown.expect(ContainerStartException.class);
        thrown.expectCause(allOf(instanceOf(ContainerReadyConditionException.class),
                hasProperty("message", containsString(
                        "Provided ContainerReadyCondition.isReady() method took longer than containerReadyTimeout"))));
        // throws expected Exception
        try {
            containerWithHangingReadyCondition.start();
        } finally {
            assertThat("ContainerReadyConditionException was thrown and starting container is expected to be stopped/killed. " +
                    "However this did not happen and there is still container running which is bug.",
                    not(containerWithHangingReadyCondition.isRunning()));
        }

    }

    @Test
    public void testContainerReadyTimeout() throws Exception {
        Docker containerWithShortTimeout = new Docker.Builder("wildlfy", "registry.hub.docker.com/jboss/wildfly:18.0.0.Final")
                .setContainerReadyTimeout(2, TimeUnit.SECONDS)
                .setContainerReadyCondition(() -> false) // never ready
                .build();

        thrown.expect(ContainerStartException.class);
        thrown.expectMessage(containsString("Container was not ready in"));

        try {
            containerWithShortTimeout.start();
        } finally {
            assertThat("DockerTimeoutException was thrown and starting container is expected to be stopped/killed. " +
                    "However this did not happen and there is still container running which is bug.",
                    not(containerWithShortTimeout.isRunning()));
        }
    }
}
