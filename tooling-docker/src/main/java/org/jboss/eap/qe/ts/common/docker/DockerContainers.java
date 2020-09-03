package org.jboss.eap.qe.ts.common.docker;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

/**
 * Purpose of this class is to have single place for configuration of docker containers which are used and started in tests.
 */
public class DockerContainers {

    private DockerContainers() {
    } // avoid instantiation

    public static Docker jaeger() {
        return new Docker.Builder("jaeger", "jaegertracing/all-in-one:1.18")
                .setContainerReadyCondition(() -> {
                    try {
                        new Socket("127.0.0.1", 16686).close();
                        return true;
                    } catch (IOException e) {
                        return false;
                    }
                })
                .setContainerReadyTimeout(3, TimeUnit.MINUTES)
                .withPortMapping("5775:5775/udp")
                .withPortMapping("6831:6831/udp")
                .withPortMapping("6832:6832/udp")
                .withPortMapping("5778:5778")
                .withPortMapping("16686:16686")
                .withPortMapping("14250:14250")
                .withPortMapping("14267:14267")
                .withPortMapping("14268:14268")
                .withPortMapping("9411:9411")
                .withEnvVar("COLLECTOR_ZIPKIN_HTTP_PORT", "9411")
                .withCmdArg("--reporter.grpc.host-port=localhost:14250")
                .build();
    }
}
