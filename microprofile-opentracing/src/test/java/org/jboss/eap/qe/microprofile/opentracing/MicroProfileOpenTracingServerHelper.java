package org.jboss.eap.qe.microprofile.opentracing;

import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;

/**
 * Utility class
 */
public class MicroProfileOpenTracingServerHelper {

    public static void reload() throws Exception {
        try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
            new Administration(client).reload();
        }
    }
}
