package org.jboss.eap.qe.microprofile.metrics.hello;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.annotation.RegistryType;

@ApplicationScoped
public class MetricsSummary {
    @Inject
    @RegistryType(type = MetricRegistry.Type.BASE)
    private MetricRegistry baseMetrics;

    @Inject
    @RegistryType(type = MetricRegistry.Type.VENDOR)
    private MetricRegistry vendorMetrics;

    @Inject
    @RegistryType(type = MetricRegistry.Type.APPLICATION)
    private MetricRegistry appMetrics;

    public JsonObject summarizeAllRegistries() {
        JsonArrayBuilder base = Json.createArrayBuilder();
        baseMetrics.getMetrics()
                .keySet()
                .stream()
                .map(MetricID::getName)
                .forEach(base::add);

        JsonArrayBuilder vendor = Json.createArrayBuilder();
        vendorMetrics.getMetrics()
                .keySet()
                .stream()
                .map(MetricID::getName)
                .forEach(vendor::add);

        JsonArrayBuilder app = Json.createArrayBuilder();
        appMetrics.getMetrics()
                .keySet()
                .stream()
                .map(MetricID::getName)
                .forEach(app::add);

        return Json.createObjectBuilder()
                .add("base", base)
                .add("vendor", vendor)
                .add("app", app)
                .build();
    }

    public JsonObject summarizeAppRegistry() {
        JsonArrayBuilder appCounters = Json.createArrayBuilder();
        appMetrics.getCounters()
                .keySet()
                .stream()
                .map(MetricID::getName)
                .forEach(appCounters::add);

        JsonArrayBuilder appTimers = Json.createArrayBuilder();
        appMetrics.getTimers()
                .keySet()
                .stream()
                .map(MetricID::getName)
                .forEach(appTimers::add);

        JsonArrayBuilder appMeters = Json.createArrayBuilder();
        appMetrics.getMeters()
                .keySet()
                .stream()
                .map(MetricID::getName)
                .forEach(appMeters::add);

        JsonArrayBuilder appGauges = Json.createArrayBuilder();
        appMetrics.getGauges()
                .keySet()
                .stream()
                .map(MetricID::getName)
                .forEach(appGauges::add);

        JsonArrayBuilder appConcurrentGauges = Json.createArrayBuilder();
        appMetrics.getConcurrentGauges()
                .keySet()
                .stream()
                .map(MetricID::getName)
                .forEach(appConcurrentGauges::add);

        JsonArrayBuilder appHistograms = Json.createArrayBuilder();
        appMetrics.getHistograms()
                .keySet()
                .stream()
                .map(MetricID::getName)
                .forEach(appHistograms::add);

        return Json.createObjectBuilder()
                .add("app-counters", appCounters)
                .add("app-timers", appTimers)
                .add("app-meters", appMeters)
                .add("app-gauges", appGauges)
                .add("app-concurrent-gauges", appConcurrentGauges)
                .add("app-histograms", appHistograms)
                .build();
    }
}
