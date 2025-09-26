package org.jboss.eap.qe.micrometer.prometheus;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility class adding and removing management users. One user is with RBAC Monitor role, second without it.
 */
public class MgmtUsersSetup {
    private static final Path MGMT_USERS_FILE;
    private static final Path MGMT_GROUPS_FILE;
    private static String mgmtUsersBackupContent;
    private static String mgmtGroupsBackupContent;

    public final static String USER_NAME = "testSuite";
    public final static String RBAC_USER_NAME = "rbacUser";
    public final static String USERS_PASSWORD = "testSuitePassword";;

    private final static String PASSWORD_HASH = "29a64f8524f32269aa9b681efc347f96";
    private final static String RBAC_PASSWORD_HASH = "34d289513caf0d8967a6d293f128a5d8";
    private final static String MGMT_USERS_CONTENT = RBAC_USER_NAME + "=" + RBAC_PASSWORD_HASH + "\n" +
            USER_NAME + "=" + PASSWORD_HASH + "\n";

    /**
     * Evaluate path of mgmt-users.properties configuration path
     */
    static {
        String jbossHome;
        if (Boolean.getBoolean("ts.bootable")) {
            jbossHome = System.getProperty("install.dir");
        } else {
            jbossHome = System.getProperty("jboss.home");
        }
        MGMT_USERS_FILE = Path.of(jbossHome, "standalone", "configuration", "mgmt-users.properties");
        MGMT_GROUPS_FILE = Path.of(jbossHome, "standalone", "configuration", "mgmt-groups.properties");
    }

    /**
     * Backup original users and create new management users
     */
    public static void setup() throws Exception {
        mgmtUsersBackupContent = Files.readString(MGMT_USERS_FILE, StandardCharsets.UTF_8);
        mgmtGroupsBackupContent = Files.readString(MGMT_GROUPS_FILE, StandardCharsets.UTF_8);
        Files.writeString(MGMT_USERS_FILE, MGMT_USERS_CONTENT, StandardCharsets.UTF_8);
        Files.writeString(MGMT_GROUPS_FILE, RBAC_USER_NAME + "=Monitor\n", StandardCharsets.UTF_8);
    }

    /**
     * Recover original management users
     */
    public static void tearDown() throws Exception {
        if (mgmtUsersBackupContent != null) {
            Files.writeString(MGMT_USERS_FILE, mgmtUsersBackupContent, StandardCharsets.UTF_8);
        }
        if (mgmtGroupsBackupContent != null) {
            Files.writeString(MGMT_GROUPS_FILE, mgmtGroupsBackupContent, StandardCharsets.UTF_8);
        }
    }
}
