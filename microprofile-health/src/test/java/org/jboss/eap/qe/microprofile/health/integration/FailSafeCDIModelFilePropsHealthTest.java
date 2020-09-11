package org.jboss.eap.qe.microprofile.health.integration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.eap.qe.microprofile.health.DisableDefaultHealthProceduresSetupTask;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.MicroProfileServerSetupTask;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;

/**
 * MP FT integration will be tested relying on FS based MP Config
 * MP Config properties are provided by file-props model option - values are stored in files on FS.
 * Files are backed-up before every test and restored after.
 * Purpose of the class is to configure MP Config. Superclass is responsible for test execution.
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({ DisableDefaultHealthProceduresSetupTask.class, MicroProfileFTSetupTask.class,
        FailSafeCDIModelFilePropsHealthTest.SetupTask.class })
public class FailSafeCDIModelFilePropsHealthTest extends FailSafeCDIHealthBaseTest {

    private byte[] liveFileBytes;
    private byte[] readyFileBytes;
    private byte[] inMaintenanceFileBytes;
    private byte[] readyInMaintenanceFileBytes;

    Path liveFile = Paths.get(
            FailSafeCDIModelFilePropsHealthTest.class.getResource("file-props/" + FailSafeDummyService.LIVE_CONFIG_PROPERTY)
                    .getPath());

    Path readyFile = Paths.get(
            FailSafeCDIModelFilePropsHealthTest.class.getResource("file-props/" + FailSafeDummyService.READY_CONFIG_PROPERTY)
                    .getFile());

    Path inMaintenanceFile = Paths.get(
            FailSafeCDIModelFilePropsHealthTest.class
                    .getResource("file-props/" + FailSafeDummyService.IN_MAINTENANCE_CONFIG_PROPERTY).getPath());

    Path readyInMaintenanceFile = Paths.get(
            FailSafeCDIModelFilePropsHealthTest.class
                    .getResource("file-props/" + FailSafeDummyService.READY_IN_MAINTENANCE_CONFIG_PROPERTY).getPath());

    @Before
    public void backup() throws IOException {
        liveFileBytes = Files.readAllBytes(liveFile);
        readyFileBytes = Files.readAllBytes(readyFile);
        inMaintenanceFileBytes = Files.readAllBytes(inMaintenanceFile);
        readyInMaintenanceFileBytes = Files.readAllBytes(readyInMaintenanceFile);
    }

    @After
    public void restore() throws IOException {
        Files.write(liveFile, liveFileBytes);
        Files.write(readyFile, readyFileBytes);
        Files.write(inMaintenanceFile, inMaintenanceFileBytes);
        Files.write(readyInMaintenanceFile, readyInMaintenanceFileBytes);
    }

    @Override
    protected void setConfigProperties(boolean live, boolean ready, boolean inMaintenance, boolean readyInMaintenance)
            throws Exception {
        Files.write(liveFile, Boolean.toString(live).getBytes(StandardCharsets.UTF_8));
        Files.write(readyFile, Boolean.toString(ready).getBytes(StandardCharsets.UTF_8));
        Files.write(inMaintenanceFile, Boolean.toString(inMaintenance).getBytes(StandardCharsets.UTF_8));
        Files.write(readyInMaintenanceFile, Boolean.toString(readyInMaintenance).getBytes(StandardCharsets.UTF_8));
        try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
            new Administration(client).reload();
        }
    }

    /**
     * Setup a microprofile-config-smallrye subsystem to obtain values from a directory defined in the subsystem.
     * The directory contains files (filenames are mapped to MP Config properties name) which contains config values.
     */
    public static class SetupTask implements MicroProfileServerSetupTask {

        @Override
        public void setup() throws Exception {
            String dir = SetupTask.class.getResource("file-props").getFile();
            try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
                client.execute(
                        String.format("/subsystem=microprofile-config-smallrye/config-source=file-props:add(dir={path=%s})",
                                dir))
                        .assertSuccess();
            }
        }

        @Override
        public void tearDown() throws Exception {
            try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
                client.execute("/subsystem=microprofile-config-smallrye/config-source=file-props:remove").assertSuccess();
            }
        }
    }
}
