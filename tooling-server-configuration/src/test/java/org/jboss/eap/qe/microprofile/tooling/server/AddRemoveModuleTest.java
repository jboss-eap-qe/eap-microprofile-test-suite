package org.jboss.eap.qe.microprofile.tooling.server;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.ConfigurationException;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.core.CommandFailedException;
import org.wildfly.extras.creaper.core.online.CliException;
import org.wildfly.extras.creaper.core.online.ModelNodeResult;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.xml.sax.SAXException;

@RunWith(Arquillian.class)
@RunAsClient
public class AddRemoveModuleTest {
    private static final String TEST_MODULE_NAME = "org.jboss.testmodule";

    private OnlineManagementClient client;

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Before
    public void setUp() throws IOException, ConfigurationException {
        client = ManagementClientProvider.onlineStandalone();
    }

    @After
    public void tearDown() throws IOException {
        client.close();
    }

    @Test
    public void addRemoveModuleTest() throws IOException, CommandFailedException, CliException, SAXException {
        ModuleUtil.add(TEST_MODULE_NAME)
                .resource("testJar1", AddRemoveModuleTest.class)
                .resource("testJar2", AddRemoveModuleTest.class)
                .moduleXMLPath(AddRemoveModuleTest.class.getResource("module.xml").getPath())
                .executeOn(client);

        // verify that module was added
        File asRoot = new File(getPathToAs());

        File module = new File(asRoot, "modules" + File.separator + TEST_MODULE_NAME.replaceAll("\\.", File.separator));
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

    private String getPathToAs() throws IOException, CliException {
        ModelNodeResult result = client.execute(":resolve-expression(expression=${jboss.home.dir})");
        result.assertSuccess("Cannot resolve jboss.home.dir");
        return result.stringValue();
    }
}
