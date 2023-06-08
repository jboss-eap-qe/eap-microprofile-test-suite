package org.jboss.eap.qe.microprofile.opentracing;

import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.MicroProfileServerSetupTask;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;

/**
 * Configures a default jaeger tracer.
 * Otherwise MP tracing won't send anything after https://issues.redhat.com/browse/WFLY-16238
 */
public class DefaultJaegerTracerServerSetup implements MicroProfileServerSetupTask {

    @Override
    public void setup() throws Exception {
        try (final OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
            client.execute("/subsystem=microprofile-opentracing-smallrye/jaeger-tracer=default:add(sender-endpoint=http://localhost:14268/api/traces)");
            client.execute("/subsystem=microprofile-opentracing-smallrye:write-attribute(name=default-tracer,value=default)");
            client.execute(":reload");
        }
    }

    @Override
    public void tearDown() throws Exception {
        try (final OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
            client.execute("/subsystem=microprofile-opentracing-smallrye/jaeger-tracer=default:remove");
            client.execute("/subsystem=microprofile-opentracing-smallrye:undefine-attribute(name=default-tracer");
        }
    }
}
