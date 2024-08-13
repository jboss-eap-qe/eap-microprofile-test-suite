/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.eap.qe.micrometer.multiple;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.eap.qe.micrometer.container.OpenTelemetryCollectorContainer;
import org.jboss.eap.qe.micrometer.container.PrometheusMetric;
import org.jboss.eap.qe.micrometer.util.MicrometerServerSetup;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.deployment.ConfigurationUtil;
import org.jboss.eap.qe.ts.common.docker.junit.DockerRequiredTests;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@ServerSetup(MicrometerServerSetup.class)
@Category(DockerRequiredTests.class)
public class MultipleWarTestCase extends BaseMultipleTestCase {
    @Deployment(name = SERVICE_ONE, order = 1)
    public static WebArchive createDeployment1() {
        return ShrinkWrap.create(WebArchive.class, SERVICE_ONE + ".war")
                .addClasses(TestApplication.class, DuplicateMetricResource1.class, BaseMultipleTestCase.class)
                .addAsWebInfResource(ConfigurationUtil.BEANS_XML_FILE_LOCATION, "beans.xml");

    }

    @Deployment(name = SERVICE_TWO, order = 2)
    public static WebArchive createDeployment2() {
        return ShrinkWrap.create(WebArchive.class, SERVICE_TWO + ".war")
                .addClasses(TestApplication.class, DuplicateMetricResource2.class, BaseMultipleTestCase.class)
                .addAsWebInfResource(ConfigurationUtil.BEANS_XML_FILE_LOCATION, "beans.xml");
    }

    @Test
    @RunAsClient
    @InSequence(1)
    public void checkPingCount(@ArquillianResource @OperateOnDeployment(SERVICE_ONE) URL serviceOne,
            @ArquillianResource @OperateOnDeployment(SERVICE_TWO) URL serviceTwo)
            throws URISyntaxException, InterruptedException {
        makeRequests(new URI(String.format("%s/%s", serviceOne, DuplicateMetricResource1.TAG)));
        makeRequests(new URI(String.format("%s/%s", serviceTwo, DuplicateMetricResource2.TAG)));

        List<PrometheusMetric> results = getMetricsByName(
                OpenTelemetryCollectorContainer.getInstance().fetchMetrics(DuplicateMetricResource1.METER_NAME),
                DuplicateMetricResource1.METER_NAME + "_total"); // Adjust for Prometheus naming conventions

        Assert.assertEquals(2, results.size());
        results.forEach(r -> Assert.assertEquals("" + REQUEST_COUNT, r.getValue()));
    }
}
