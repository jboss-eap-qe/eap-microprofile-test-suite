package org.jboss.eap.qe.microprofile.metrics.integration.config;

import org.junit.Test;

public abstract class CustomMetricDynamicBaseTest extends CustomMetricBaseTest {

    /**
     * @tpTestDetails High level customer scenario to verify correct behaviour when custom counter metric is used.
     *                The counter increment is changed dynamically and depends on MP Config property value and is provided
     *                by a CDI bean.
     * @tpPassCrit Counter metric is incremented by configured value that is changed dynamically
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testCustomMetricWithChange() throws Exception {
        setConfigProperties(5);
        performRequest();
        testMetricForValue(5);
        setConfigProperties(12);
        performRequest();
        testMetricForValue(17);
    }

}
