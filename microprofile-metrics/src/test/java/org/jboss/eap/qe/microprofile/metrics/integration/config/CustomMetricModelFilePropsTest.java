package org.jboss.eap.qe.microprofile.metrics.integration.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.MicroProfileServerSetupTask;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;

/**
 * MP Config property is provided by file-props model option - values are stored in files on FS.
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(CustomMetricModelFilePropsTest.SetupTask.class)
public class CustomMetricModelFilePropsTest extends CustomMetricBaseTest {
    private byte[] bytes;

    private Path incrementFilePath = Paths.get(
            CustomMetricModelFilePropsTest.class.getResource("file-props/" + INCREMENT_CONFIG_PROPERTY).getPath());

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        WebArchive webArchive = ShrinkWrap
                .create(WebArchive.class, CustomMetricModelFilePropsTest.class.getSimpleName() + ".war")
                .addClasses(CustomCounterIncrementProvider.class, CustomCounterMetric.class, CustomMetricService.class,
                        CustomMetricApplication.class, CustomMetricAppInitializer.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return webArchive;
    }

    @Before
    public void backup() throws IOException {
        bytes = Files.readAllBytes(incrementFilePath);
    }

    @After
    public void restore() throws IOException {
        Files.write(incrementFilePath, bytes);
    }

    void setConfigProperties(int increment) throws Exception {
        //      TODO Java 11 API way - Files.writeString(incrementFilePath, Integer.toString(increment));
        Files.write(incrementFilePath, Integer.toString(increment).getBytes(StandardCharsets.UTF_8));
        try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
            new Administration(client).reload();
        }
    }

    /**
     * Setup a microprofile-config-smallrye subsystem to obtain values from a directory defined in the subsystem.
     * The directory contains files (file name is mapped to MP Config property name) which contain config values.
     */
    static class SetupTask implements MicroProfileServerSetupTask {

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
