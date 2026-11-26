package org.jboss.eap.qe.microprofile.common.setuptasks;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.operations.Address;
import org.wildfly.extras.creaper.core.online.operations.OperationException;
import org.wildfly.extras.creaper.core.online.operations.Operations;
import org.wildfly.extras.creaper.core.online.operations.Values;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;

/**
 * Utility class enabling and disabling Micrometer Prometheus registry. The registry is configured as a
 * <code>/subsystem=micrometer/registry=prometheus</code> resource, so the Micrometer subsystem must exist before
 * {@link #enable(OnlineManagementClient)} is called.
 */
public class MicrometerPrometheusSetup {
    public static final String DEFAULT_PROMETHEUS_CONTEXT = "/prometheus";
    private static String prometheusContext = null;
    public static final Address PROMETHEUS_REGISTRY_ADDRESS = Address.subsystem("micrometer")
            .and("registry", "prometheus");
    /** Name of the registry attribute holding the end-point path */
    public static final String ATTR_CONTEXT = "context";
    /** Name of the registry attribute enabling/disabling security of the end-point */
    public static final String ATTR_SECURITY_ENABLED = "security-enabled";

    /**
     * Enable prometheus with most used configuration in this testsuite ("/prometheus" end-point with disabled security)
     *
     * @param client Creaper client
     */
    public static void enable(OnlineManagementClient client) throws IOException, InterruptedException, TimeoutException {
        Operations ops = new Operations(client);
        ops.add(PROMETHEUS_REGISTRY_ADDRESS,
                Values.of(ATTR_CONTEXT, DEFAULT_PROMETHEUS_CONTEXT).and(ATTR_SECURITY_ENABLED, false));
        prometheusContext = DEFAULT_PROMETHEUS_CONTEXT;
        new Administration(client).reload();
    }

    /**
     * Enable prometheus registry with custom parameters
     *
     * @param client Creaper client
     * @param context Prometheus end-point
     * @param security Security enabled/disabled
     */
    public static void set(OnlineManagementClient client, String context, boolean security)
            throws IOException, InterruptedException, TimeoutException {
        Operations ops = new Operations(client);
        ops.writeAttribute(PROMETHEUS_REGISTRY_ADDRESS, ATTR_CONTEXT, context);
        prometheusContext = context;
        ops.writeAttribute(PROMETHEUS_REGISTRY_ADDRESS, ATTR_SECURITY_ENABLED, security);
        new Administration(client).reload();
    }

    /**
     * Enable prometheus registry with default end-point (/prometheus) and custom security
     *
     * @param client Creaper client
     * @param security Security enabled/disabled
     */
    public static void set(OnlineManagementClient client, boolean security)
            throws IOException, InterruptedException, TimeoutException {
        set(client, DEFAULT_PROMETHEUS_CONTEXT, security);
    }

    /**
     * Disable prometheus
     *
     * @param client Creaper client
     */
    public static void disable(OnlineManagementClient client)
            throws IOException, OperationException, InterruptedException, TimeoutException {
        Operations ops = new Operations(client);
        // the registry is already gone if the whole Micrometer subsystem was removed before
        if (ops.exists(PROMETHEUS_REGISTRY_ADDRESS)) {
            ops.remove(PROMETHEUS_REGISTRY_ADDRESS);
        }
        new Administration(client).reload();
    }

    /**
     * Get prometheus end-point
     */
    public static String getPrometheusContext() {
        return prometheusContext;
    }
}
