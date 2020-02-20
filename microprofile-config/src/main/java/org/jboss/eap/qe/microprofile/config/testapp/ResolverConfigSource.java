package org.jboss.eap.qe.microprofile.config.testapp;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * End point use default Config object, release it, create and use new one and release it again.
 * New default Config should be created again.
 *
 * This is just an example that demonstrate, that releaseConfig call is possible.
 * It is not full use-case (where some synchronization would be necessary, etc.)
 */
public class ResolverConfigSource implements ConfigSource {

    public final static String PROPERTY_NAME = "resolver.test.property";
    public final static String UPDATED_PROPERTY_VALUE = "custom";

    private final Map<String, String> properties;

    public ResolverConfigSource() {
        properties = new HashMap<>();
        properties.put(PROPERTY_NAME, UPDATED_PROPERTY_VALUE);
    }

    @Override
    public Map<String, String> getProperties() {
        return properties;
    }

    @Override
    public String getValue(String propertyName) {
        return properties.get(propertyName);
    }

    @Override
    public String getName() {
        return "Resolver config source";
    }

    @Override
    public int getOrdinal() {
        return 500;
    }
}
