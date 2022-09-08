package org.jboss.eap.qe.microprofile.tooling.server.configuration.deployment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ConfigurationUtil {

    private static final String BEANS_XML_FILE_LOCATION = "cdi/beans.xml";
    public static final String BEANS_XML_FILE_CONTENT = ConfigurationUtil.readFromInputStream(
            ConfigurationUtil.class.getClassLoader().getResourceAsStream(BEANS_XML_FILE_LOCATION)
    );

    private static String readFromInputStream(InputStream inputStream) {
        StringBuilder resultStringBuilder = new StringBuilder();
        try (BufferedReader br
                     = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                resultStringBuilder.append(line).append("\n");
            }
        } catch (IOException err) {
            throw new RuntimeException("Could not read file " + BEANS_XML_FILE_LOCATION, err);
        }
        return resultStringBuilder.toString();
    }
}
