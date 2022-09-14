package org.jboss.eap.qe.microprofile.tooling.server.configuration.deployment;

import java.net.URL;

public class ConfigurationUtil {
    public static final URL BEANS_XML_FILE_LOCATION = ConfigurationUtil.class.getClassLoader().getResource("cdi/beans.xml");
}
