package org.jboss.eap.qe.micrometer.prometheus;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class MgmtUsersSetup {
    private static final Path MGMT_USERS_FILE;
    private static String backupContent;

    public final static String USER_NAME = "testSuite";
    public final static String PASSWORD = "testSuitePassword";

    static {
        String jbossHome = System.getProperty("jboss.home");
        MGMT_USERS_FILE = Path.of(jbossHome, "standalone", "configuration", "mgmt-users.properties");
    }

    public static void setup() throws Exception {
        backupContent = Files.readString(MGMT_USERS_FILE, StandardCharsets.UTF_8);
        Files.writeString(MGMT_USERS_FILE, USER_NAME + "=29a64f8524f32269aa9b681efc347f96\n", StandardCharsets.UTF_8); // testSuite=testSuitePassword
    }

    public static void tearDown() throws Exception {
        if (backupContent != null) {
            Files.writeString(MGMT_USERS_FILE, backupContent, StandardCharsets.UTF_8);
        }
    }
}
