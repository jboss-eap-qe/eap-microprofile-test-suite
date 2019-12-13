package org.jboss.eap.qe.microprofile.metrics.integration.config;

import org.junit.Test;

public abstract class CustomMetricDynamicBaseTest extends CustomMetricBaseTest {

    /**
     * @tpTestDetails High-level multi-component customer scenario to verify custom counter metric is incremented
     *                accordingly to the number of a CDI bean invocations.
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
