package org.jboss.eap.qe.microprofile.tooling.server;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.wildfly.extras.creaper.core.online.CliException;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;

import com.google.common.base.Joiner;

/**
 * Class provides support for add/remove module
 */
public class ModuleUtil {

    /**
     * @param name name of a module
     * @return handler for add operation
     */
    public static AddModuleHandler add(String name) {
        return new AddModuleHandler(name);
    }

    /**
     * @param name name of a module
     * @return handler for remove operation
     */
    public static RemoveModuleHandler remove(String name) {
        return new RemoveModuleHandler(name);
    }

    public static class AddModuleHandler {
        private final String name;
        private String moduleXMLPath;
        private List<File> resources;

        private AddModuleHandler(String name) {
            this.name = name;
            this.resources = new ArrayList<>();
        }

        public AddModuleHandler moduleXMLPath(String moduleXMLPath) {
            this.moduleXMLPath = moduleXMLPath;
            return this;
        }

        public AddModuleHandler resource(String jarName, Class... classes) {
            File testJar = new File(jarName + ".jar");
            ShrinkWrap.create(JavaArchive.class)
                    .addClasses(classes)
                    .as(ZipExporter.class)
                    .exportTo(testJar, true);
            resources.add(testJar);
            return this;
        }

        public void executeOn(OnlineManagementClient client) throws IOException, CliException {
            StringBuilder cmd = new StringBuilder("module add");
            cmd.append(" --name=").append(name);

            if (this.moduleXMLPath != null) {
                cmd.append(" --module-xml=").append(this.moduleXMLPath);
            }

            Joiner resourcesJoiner = Joiner.on(File.pathSeparatorChar);
            char pathSeparatorChar = File.pathSeparatorChar;
            cmd.append(" --resource-delimiter=").append(pathSeparatorChar);

            if (!this.resources.isEmpty()) {
                cmd.append(" --resources=").append(resourcesJoiner
                        .join(this.resources.stream().map(File::getAbsolutePath).toArray()).replaceAll(" ", "\\\\ "));
            }
            client.executeCli(cmd.toString());
            resources.forEach(File::delete);
        }
    }

    public static class RemoveModuleHandler {
        private final String name;

        private RemoveModuleHandler(String name) {
            this.name = name;
        }

        public void executeOn(OnlineManagementClient client) throws IOException, CliException {
            client.executeCli("module remove --name=" + name);
        }
    }
}
