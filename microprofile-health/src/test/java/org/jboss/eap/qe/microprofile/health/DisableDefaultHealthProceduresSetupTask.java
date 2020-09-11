package org.jboss.eap.qe.microprofile.health;

import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.MicroProfileServerSetupTask;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;

/**
 * Add system property {@code MP_HEALTH_DISABLE_DEFAULT_PROCEDURES} for MP Health to disable vendor specific checks
 */
public class DisableDefaultHealthProceduresSetupTask implements MicroProfileServerSetupTask {

    public static final String MP_HEALTH_DISABLE_DEFAULT_PROCEDURES = "mp.health.disable-default-procedures";

    @Override
    public void setup() throws Exception {
        try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
            client.execute(String.format("/system-property=%s:add(value=true)", MP_HEALTH_DISABLE_DEFAULT_PROCEDURES))
                    .assertSuccess();
            new Administration(client).reload();
        }
    }

    @Override
    public void tearDown() throws Exception {
        try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
            client.execute(String.format("/system-property=%s:remove", MP_HEALTH_DISABLE_DEFAULT_PROCEDURES))
                    .assertSuccess();
            new Administration(client).reload();
        }
    }
}
