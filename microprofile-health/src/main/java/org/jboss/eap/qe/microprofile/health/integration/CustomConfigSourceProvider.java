package org.jboss.eap.qe.microprofile.health.integration;

import java.util.Arrays;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

/**
 * Class provide list of {@link ConfigSource} implementations. Namely {@link CustomConfigSource}
 */
public class CustomConfigSourceProvider implements ConfigSourceProvider {
    @Override
    public Iterable<ConfigSource> getConfigSources(ClassLoader classLoader) {
        return Arrays.asList(new CustomConfigSource());
    }
}
