package org.jboss.eap.qe.micrometer.prometheus;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.operations.Address;
import org.wildfly.extras.creaper.core.online.operations.OperationException;
import org.wildfly.extras.creaper.core.online.operations.Operations;
import org.wildfly.extras.creaper.core.online.operations.Values;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;

public class MicrometerPrometheusSetup {
    public static final String DEFAULT_PROMETHEUS_CONTEXT = "/prometheus";
    private static String prometheusContext = null;
    public static final Address SYSPROP_PROMETHEUS_CONTEXT = Address.of("system-property",
            "org.jboss.eap.xp.micrometer.prometheus.context");
    public static final Address SYSPROP_SECURITY_ENABLED = Address.of("system-property",
            "org.jboss.eap.xp.micrometer.prometheus.security-enabled");

    public static void enable(OnlineManagementClient client) throws IOException, InterruptedException, TimeoutException {
        Operations ops = new Operations(client);
        ops.add(SYSPROP_PROMETHEUS_CONTEXT, Values.of("value", DEFAULT_PROMETHEUS_CONTEXT));
        prometheusContext = DEFAULT_PROMETHEUS_CONTEXT;
        ops.add(SYSPROP_SECURITY_ENABLED, Values.of("value", "false"));
        new Administration(client).reload();
    }

    public static void set(OnlineManagementClient client, String context, boolean security)
            throws IOException, InterruptedException, TimeoutException {
        Operations ops = new Operations(client);
        ops.writeAttribute(SYSPROP_PROMETHEUS_CONTEXT, "value", context);
        prometheusContext = context;
        ops.writeAttribute(SYSPROP_SECURITY_ENABLED, "value", security);
        new Administration(client).reload();
    }

    public static void set(OnlineManagementClient client, boolean security)
            throws IOException, InterruptedException, TimeoutException {
        set(client, DEFAULT_PROMETHEUS_CONTEXT, security);
    }

    public static void disable(OnlineManagementClient client)
            throws IOException, OperationException, InterruptedException, TimeoutException {
        Operations ops = new Operations(client);
        if (ops.exists(SYSPROP_PROMETHEUS_CONTEXT)) {
            ops.remove(SYSPROP_PROMETHEUS_CONTEXT);
        }
        if (ops.exists(SYSPROP_SECURITY_ENABLED)) {
            ops.remove(SYSPROP_SECURITY_ENABLED);
        }
        new Administration(client).reload();
    }

    public static String getPrometheusContext() {
        return prometheusContext;
    }
}
