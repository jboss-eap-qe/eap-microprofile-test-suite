package org.jboss.eap.qe.microprofile.metrics.integration;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.wildfly.extras.creaper.commands.modules.AddModule;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;

public class ModuleUtil {
    public static void setupModule(OnlineManagementClient client, File moduleXML, String moduleName, String jarName,
            Class... classes)
            throws Exception {
        File jar = createJar(jarName, classes);
        client.apply(
                new AddModule.Builder(moduleName)
                        .resource(jar)
                        .moduleXml(moduleXML)
                        .build());
        FileUtils.forceDelete(jar);
    }

    public static File createJar(String name, Class... classes) {
        File testJar = new File(name + ".jar");
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class)
                .addClasses(classes);
        jar.as(ZipExporter.class).exportTo(testJar, true);
        return testJar;
    }
}
