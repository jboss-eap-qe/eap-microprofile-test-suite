package org.jboss.eap.qe.micrometer.container;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.jboss.eap.qe.micrometer.model.jaeger.JaegerTrace;
import org.jboss.eap.qe.ts.common.docker.Docker;

/**
 * Inspired by the similar class in Wildfly, this is an implementation of an OTel collector which uses the TS Docker
 * APIs, instead ogf the Testcontainers based tooling which is available in WildFly
 */
public class OpenTelemetryCollectorContainer {
    private static OpenTelemetryCollectorContainer INSTANCE = null;
    private static JaegerContainer jaegerContainer;

    public static final int DEFAULT_OTLP_GRPC_PORT = 4317;
    public static final int DOCKER_CONTAINER_OTLP_GRPC_PORT = DEFAULT_OTLP_GRPC_PORT;
    public static final int DOCKER_HOST_OTLP_GRPC_PORT = DEFAULT_OTLP_GRPC_PORT + 100;
    public static final int DEFAULT_OTLP_HTTP_PORT = 4318;
    public static final int DOCKER_CONTAINER_OTLP_HTTP_PORT = DEFAULT_OTLP_HTTP_PORT;
    public static final int DOCKER_HOST_OTLP_HTTP_PORT = DEFAULT_OTLP_HTTP_PORT + 100;
    public static final int PROMETHEUS_PORT = 49152;
    public static final int HEALTH_CHECK_PORT = 13133;
    private static final String OTEL_CONFIG_DIR_NAME = "otel-collector";
    private static final String OTEL_CONFIG_FILE_NAME = "config.yaml";
    public static final String OTEL_COLLECTOR_CONFIG_LOCAL_PATH = Paths.get(System.getProperty("user.home"),
            OTEL_CONFIG_DIR_NAME, OTEL_CONFIG_FILE_NAME).toAbsolutePath().toString();
    public static final String OTEL_COLLECTOR_CONFIG_CONTAINER_BASE_PATH = Paths.get("/etc",
            OTEL_CONFIG_DIR_NAME).toString();
    public static final String OTEL_COLLECTOR_CONFIG_CONTAINER_FILE_PATH = OTEL_COLLECTOR_CONFIG_CONTAINER_BASE_PATH + "/"
            + OTEL_CONFIG_FILE_NAME;

    private String otlpGrpcEndpoint;
    private String otlpHttpEndpoint;
    private String prometheusUrl;
    private final Docker otelCollector;

    private String getLocalOtelCollectorConfigYamlAbsolutePath() {
        File tempFile = null;
        Path localPath = null;
        final InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream("otel-collector-config.yaml");
        if (resourceAsStream == null) {
            throw new IllegalStateException(
                    "Test error, can't find the \"otel-collector-config.yaml\" resource in the classpath");
        }
        try {
            tempFile = new File(OTEL_COLLECTOR_CONFIG_LOCAL_PATH);
            localPath = tempFile.toPath();
            Files.deleteIfExists(localPath);
            Files.createDirectories(tempFile.getParentFile().toPath());
            Files.copy(
                    resourceAsStream,
                    tempFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Test error, cannot create a copy of the OTel config file: " + e);
        }
        return tempFile.getAbsolutePath();
    }

    private OpenTelemetryCollectorContainer() {
        otelCollector = new Docker.Builder("otel-collector",
                "ghcr.io/open-telemetry/opentelemetry-collector-releases/opentelemetry-collector-contrib:latest")
                .setContainerReadyCondition(() -> {
                    try {
                        new Socket("127.0.0.1", HEALTH_CHECK_PORT).close();
                        return true;
                    } catch (IOException e) {
                        return false;
                    }
                })
                .setContainerReadyTimeout(3, TimeUnit.MINUTES)
                .withVolumeMount(String.format("%s:%s:Z",
                        getLocalOtelCollectorConfigYamlAbsolutePath(),
                        OTEL_COLLECTOR_CONFIG_CONTAINER_FILE_PATH))
                .withPortMapping(String.format("%s:%s", DOCKER_HOST_OTLP_GRPC_PORT, DOCKER_CONTAINER_OTLP_GRPC_PORT))
                .withPortMapping(String.format("%s:%s", DOCKER_HOST_OTLP_HTTP_PORT, DOCKER_CONTAINER_OTLP_HTTP_PORT))
                .withPortMapping(String.format("%1$s:%1$s", HEALTH_CHECK_PORT))
                .withPortMapping(String.format("%1$s:%1$s", PROMETHEUS_PORT))
                .withCmdArg("--config=" + OTEL_COLLECTOR_CONFIG_CONTAINER_FILE_PATH)
                .build();
    }

    public static synchronized OpenTelemetryCollectorContainer getInstance() {
        if (INSTANCE == null) {
            jaegerContainer = JaegerContainer.getInstance();
            INSTANCE = new OpenTelemetryCollectorContainer();
            INSTANCE.start();
        }
        return INSTANCE;
    }

    public void start() {
        try {
            otelCollector.start();
        } catch (Exception e) {
            throw new IllegalStateException("Starting the OTel container failed: " + e);
        }
        otlpGrpcEndpoint = "http://localhost:" + DOCKER_HOST_OTLP_GRPC_PORT;
        otlpHttpEndpoint = "http://localhost:" + DOCKER_HOST_OTLP_HTTP_PORT;
        prometheusUrl = "http://localhost:" + PROMETHEUS_PORT + "/metrics";
    }

    public synchronized void stop() {
        if (jaegerContainer != null) {
            jaegerContainer.stop();
            jaegerContainer = null;
        }
        INSTANCE = null;
        try {
            otelCollector.stop();
        } catch (Exception e) {
            throw new IllegalStateException("Stopping the OTel container failed: " + e);
        }
    }

    public String getOtlpGrpcEndpoint() {
        return otlpGrpcEndpoint;
    }

    public String getOtlpHttpEndpoint() {
        return otlpHttpEndpoint;
    }

    public String getPrometheusUrl() {
        return prometheusUrl;
    }

    public List<PrometheusMetric> fetchMetrics(String nameToMonitor) throws InterruptedException {
        String body = "";
        try (Client client = ClientBuilder.newClient()) {
            WebTarget target = client.target(OpenTelemetryCollectorContainer.getInstance().getPrometheusUrl());

            int attemptCount = 0;
            boolean found = false;

            // Request counts can vary. Setting high to help ensure test stability
            while (!found && attemptCount < 30) {
                // Wait to give Micrometer time to export
                Thread.sleep(1000);

                body = target.request().get().readEntity(String.class);
                found = body.contains(nameToMonitor);
                attemptCount++;
            }
        }

        return buildPrometheusMetrics(body);
    }

    public List<JaegerTrace> getTraces(String serviceName) throws InterruptedException {
        return (jaegerContainer != null ? jaegerContainer.getTraces(serviceName) : Collections.emptyList());
    }

    private List<PrometheusMetric> buildPrometheusMetrics(String body) {
        String[] entries = body.split("\n");
        Map<String, String> help = new HashMap<>();
        Map<String, String> type = new HashMap<>();
        List<PrometheusMetric> metrics = new LinkedList<>();
        Arrays.stream(entries).forEach(e -> {
            if (e.startsWith("# HELP")) {
                extractMetadata(help, e);
            } else if (e.startsWith("# TYPE")) {
                extractMetadata(type, e);
            } else {
                String[] parts = e.split("[{}]");
                String key = parts[0];
                Map<String, String> tags = Arrays.stream(parts[1].split(","))
                        .map(t -> t.split("="))
                        .collect(Collectors.toMap(i -> i[0],
                                i -> i[1]
                                        .replaceAll("^\"", "")
                                        .replaceAll("\"$", "")));
                metrics.add(new PrometheusMetric(key, tags, parts[2].trim(), type.get(key), help.get(key)));
            }
        });

        return metrics;
    }

    private void extractMetadata(Map<String, String> target, String source) {
        String[] parts = source.split(" ");
        target.put(parts[2],
                Arrays.stream(Arrays.copyOfRange(parts, 3, parts.length))
                        .reduce("", (total, element) -> total + " " + element));
    }
}
