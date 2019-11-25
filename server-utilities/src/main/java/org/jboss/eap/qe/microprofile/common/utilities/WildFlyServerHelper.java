package org.jboss.eap.qe.microprofile.common.utilities;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Class aggregating utilities for server
 */
public final class WildFlyServerHelper {

    private WildFlyServerHelper() {
        //intentionally left empty
    }

    /**
     * Return path to server's log file
     * @return path to log file
     */
    public static File getPathToLogFile() {
        final Path path = Paths.get(System.getProperty("jboss.home"), "standalone", "log", "server.log");
        return path.toFile();
    }

}
