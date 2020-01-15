package org.jboss.eap.qe.microprofile.tooling.server;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.ConfigurationException;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.core.CommandFailedException;
import org.wildfly.extras.creaper.core.online.CliException;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.xml.sax.SAXException;

@RunWith(Arquillian.class)
@RunAsClient
public class AddRemoveModuleTest {
    private static final String TEST_MODULE_NAME = "org.jboss.testmodule";

    private static OnlineManagementClient client;

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @BeforeClass
    public static void setUp() throws IOException, ConfigurationException {
        client = ManagementClientProvider.onlineStandalone();
    }

    @AfterClass
    public static void tearDown() throws IOException {
        client.close();
    }

    @Test
    public void addRemoveModuleTest() throws IOException, CommandFailedException, CliException, SAXException {
        ModuleUtil.add(TEST_MODULE_NAME)
                .addResource("testJar1", AddRemoveModuleTest.class)
                .addResource("testJar2", AddRemoveModuleTest.class)
                .setModuleXMLPath(AddRemoveModuleTest.class.getResource("module.xml").getPath())
                .executeOn(client);

        // verify that module was added
        File modulesRoot = new File(System.getProperty("module.path"));

        File module = new File(modulesRoot, TEST_MODULE_NAME.replaceAll("\\.", File.separator));
        assertTrue("Module " + module.getAbsolutePath() + " should exist on path", module.exists());

        File moduleTestJar1 = new File(module, "main" + File.separator + "testJar1.jar");
        assertTrue("File " + moduleTestJar1.getAbsolutePath() + " should exist", moduleTestJar1.exists());
        File moduleTestJar2 = new File(module, "main" + File.separator + "testJar2.jar");
        assertTrue("File " + moduleTestJar2.getAbsolutePath() + " should exist", moduleTestJar2.exists());

        File moduleXml = new File(module, "main" + File.separator + "module.xml");
        assertTrue("File " + moduleXml.getName() + " should exist in " + module.getAbsolutePath(), moduleXml.exists());

        // remove test module
        ModuleUtil.remove(TEST_MODULE_NAME).executeOn(client);

        // verify that module was removed
        assertFalse("Module shouldn't exist on path " + module.getAbsolutePath(), module.exists());
    }
}
