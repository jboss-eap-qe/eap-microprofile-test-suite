package org.jboss.eap.qe.ts.common.docker.junit;

/**
 * An interface that marks a test class which requires a docker service running locally.
 * Used via JUnit {@code org.junit.experimental.categories.Category} to include or exclude groups of tests.
 */
public interface DockerRequiredTests {
}
