/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.eap.qe.observability.containers;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;

import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.jboss.eap.qe.observability.jaeger.model.JaegerResponse;
import org.jboss.eap.qe.observability.jaeger.model.JaegerTrace;
import org.jboss.eap.qe.ts.common.docker.Docker;

/**
 * Copied from org.wildfly.test.integration.observability.container
 */
public class JaegerContainer {
    private static JaegerContainer INSTANCE = null;
    public static final int DOCKER_CONTAINER_PORT_JAEGER_QUERY = 16686;
    public static final int DOCKER_HOST_PORT_JAEGER_QUERY = DOCKER_CONTAINER_PORT_JAEGER_QUERY;
    public static final int DOCKER_CONTAINER_PORT_JAEGER_OTLP = OpenTelemetryCollectorContainer.DEFAULT_OTLP_GRPC_PORT;
    public static final int DOCKER_HOST_PORT_JAEGER_OTLP = DOCKER_CONTAINER_PORT_JAEGER_OTLP - 100;
    private String jaegerEndpoint;

    private final Docker jaeger;

    private JaegerContainer() {
        jaeger = new Docker.Builder("jaeger", "quay.io/jaegertracing/all-in-one:1.58")
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
                .withPortMapping("14250:14250")
                .withPortMapping("14267:14267")
                .withPortMapping("14268:14268")
                .withPortMapping("9411:9411")
                .withPortMapping(String.format("%s:%s", DOCKER_HOST_PORT_JAEGER_QUERY, DOCKER_CONTAINER_PORT_JAEGER_QUERY))
                .withPortMapping(String.format("%s:%s", DOCKER_HOST_PORT_JAEGER_OTLP, DOCKER_CONTAINER_PORT_JAEGER_OTLP))
                .withEnvVar("JAEGER_DISABLED", "true")
                .withEnvVar("COLLECTOR_OTLP_ENABLED", "false")
                .build();
    }

    public static synchronized JaegerContainer getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new JaegerContainer();
            INSTANCE.start();
        }

        return INSTANCE;
    }

    public void start() {
        try {
            jaeger.start();
        } catch (Exception e) {
            throw new IllegalStateException("Starting the Jaeger container failed: " + e);
        }
        jaegerEndpoint = "http://localhost:" + DOCKER_HOST_PORT_JAEGER_QUERY;
    }

    public synchronized void stop() {
        INSTANCE = null;
        try {
            jaeger.stop();
        } catch (Exception e) {
            throw new IllegalStateException("Stopping the Jaeger container failed: " + e);
        }
    }

    List<JaegerTrace> getTraces(String serviceName) throws InterruptedException {
        try (Client client = ClientBuilder.newClient()) {
            waitForDataToAppear(serviceName);
            String jaegerUrl = jaegerEndpoint + "/api/traces?service=" + serviceName;
            JaegerResponse jaegerResponse = client.target(jaegerUrl).request().get().readEntity(JaegerResponse.class);
            return jaegerResponse.getData();
        }
    }

    private void waitForDataToAppear(String serviceName) {
        try (Client client = ClientBuilder.newClient()) {
            String uri = jaegerEndpoint + "/api/services";
            WebTarget target = client.target(uri);
            boolean found = false;
            int count = 0;
            while (count < 10) {
                String response = target.request().get().readEntity(String.class);
                if (response.contains(serviceName)) {
                    found = true;
                    break;
                }
                count++;
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    //
                }
            }
            if (!found) {
                throw new IllegalStateException("Expected service name not found");
            }
        }
    }
}
