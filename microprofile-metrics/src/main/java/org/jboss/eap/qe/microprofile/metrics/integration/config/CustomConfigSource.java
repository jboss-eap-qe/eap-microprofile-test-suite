package org.jboss.eap.qe.microprofile.metrics.integration.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * Always loads properties from {@link CustomConfigSource#FILEPATH_PROPERTY}.
 */
public class CustomConfigSource implements ConfigSource {
    public static final String FILEPATH_PROPERTY = "config.source.properties.path";

    @Override
    public Map<String, String> getProperties() {
        String filename = System.getProperty(FILEPATH_PROPERTY);
        if (filename == null) {
            throw new RuntimeException(FILEPATH_PROPERTY + " property not defined");
        }
        Map<String, String> props = new HashMap<>();
        try (FileInputStream is = new FileInputStream(filename)) {
            Properties properties = new Properties();
            properties.load(is);
            for (String key : properties.stringPropertyNames()) {
                props.put(key, properties.getProperty(key));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Collections.unmodifiableMap(props);
    }

    @Override
    public String getValue(String s) {
        return getProperties().get(s);
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }
}
