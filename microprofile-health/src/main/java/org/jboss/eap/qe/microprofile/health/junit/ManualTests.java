package org.jboss.eap.qe.microprofile.health.junit;

/**
 * An interface that marks a test class which handles the Arquillian life cycle manually.
 * Used via JUnit {@code org.junit.experimental.categories.Category} to include or exclude groups of tests.
 */
public interface ManualTests {
    String ARQUILLIAN_CONTAINER = "jboss-manual";
}
