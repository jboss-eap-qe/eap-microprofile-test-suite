package org.jboss.eap.qe.microprofile.health.integration;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * Class provides MP Config properties values in dynamic way. Meaning values are taken from a file on FS. This is one
 * option to provide values for MP Config properties.
 */
public class CustomConfigSource implements ConfigSource {
    public static final String PROPERTIES_FILE_PATH = "config.source.properties.path";

    @Override
    public Map<String, String> getProperties() {
        String filename = System.getProperty(PROPERTIES_FILE_PATH);
        if (filename == null) {
            throw new RuntimeException(PROPERTIES_FILE_PATH + " property not defined");
        }
        Map<String, String> props = new HashMap<>();
        try (FileInputStream is = new FileInputStream(filename)) {
            Properties properties = new Properties();
            properties.load(is);
            for (String key : properties.stringPropertyNames()) {
                props.put(key, properties.getProperty(key));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
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
