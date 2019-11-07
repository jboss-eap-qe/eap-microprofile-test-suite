package org.jboss.eap.qe.ts.common.docker;

/**
 * Purpose of this class is to have single place for configuration of docker containers which are used and started in tests.
 */
public class DockerContainers {
    private DockerContainers() {} // avoid instantiation

    public static Docker jaeger() {
        return new Docker("jaeger", "jaegertracing/all-in-one:1.11")
                .waitForLogLine("\"Health Check state change\",\"status\":\"ready\"")
                // https://www.jaegertracing.io/docs/1.11/getting-started/
                .addPort("5775:5775/udp")
                .addPort("6831:6831/udp")
                .addPort("6832:6832/udp")
                .addPort("5778:5778")
                .addPort("16686:16686")
                .addPort("14250:14250")
                .addPort("14267:14267")
                .addPort("14268:14268")
                .addPort("9411:9411")
                .addEnvVar("COLLECTOR_ZIPKIN_HTTP_PORT", "9411")
                .addCmdArg("--reporter.grpc.host-port=localhost:14250");
    }
}
