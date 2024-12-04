package org.jboss.eap.qe.microprofile.telemetry.metrics.namefellow;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;

@ApplicationScoped
public class PingOneService {
    public static final String MESSAGE = "pong one";
    public static final String PING_ONE_SERVICE_TAG = "ping-one-service-tag";

    @Inject
    private Meter meter;
    private LongCounter longCounter;

    @PostConstruct
    public void init() {
        longCounter = meter
                .counterBuilder("ping_count")
                .setDescription("Number of ping invocations")
                .build();
    }

    public String ping() {
        longCounter.add(1, Attributes.of(
                AttributeKey.stringKey("_app"), PING_ONE_SERVICE_TAG));
        return MESSAGE;
    }
}
