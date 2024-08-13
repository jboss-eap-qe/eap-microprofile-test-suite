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
import org.jboss.arquillian.container.test.api.Testable;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.eap.qe.micrometer.container.OpenTelemetryCollectorContainer;
import org.jboss.eap.qe.micrometer.container.PrometheusMetric;
import org.jboss.eap.qe.micrometer.util.MicrometerServerSetup;
import org.jboss.eap.qe.ts.common.docker.junit.DockerRequiredTests;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@ServerSetup(MicrometerServerSetup.class)
@Category(DockerRequiredTests.class)
public class EarDeploymentTestCase extends BaseMultipleTestCase {
    protected static final String ENTERPRISE_APP = "enterprise-app";

    @Deployment(name = ENTERPRISE_APP, testable = false)
    public static EnterpriseArchive createDeployment() {
        return ShrinkWrap.create(EnterpriseArchive.class, ENTERPRISE_APP + ".ear")
                .addAsModules(MultipleWarTestCase.createDeployment1(),
                        Testable.archiveToTest(MultipleWarTestCase.createDeployment2()))
                .setApplicationXML(new StringAsset("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<application xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" " +
                        "       xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                        "       xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/application_10.xsd\" "
                        +
                        "       version=\"10\">\n"
                        + "  <display-name>metrics</display-name>\n"
                        + "  <module>\n"
                        + "    <web>\n"
                        + "      <web-uri>" + SERVICE_ONE + ".war</web-uri>\n"
                        + "    </web>\n"
                        + "  </module>\n"
                        + "  <module>\n"
                        + "    <web>\n"
                        + "      <web-uri>" + SERVICE_TWO + ".war</web-uri>\n"
                        + "    </web>\n"
                        + "  </module>\n"
                        + "</application>"));
    }

    @Test
    @RunAsClient
    public void dataTest(@ArquillianResource @OperateOnDeployment(ENTERPRISE_APP) URL earUrl)
            throws URISyntaxException, InterruptedException {
        makeRequests(new URI(String.format("%s/%s/%s/%s", earUrl, ENTERPRISE_APP, SERVICE_ONE, DuplicateMetricResource1.TAG)));
        makeRequests(new URI(String.format("%s/%s/%s/%s", earUrl, ENTERPRISE_APP, SERVICE_TWO, DuplicateMetricResource2.TAG)));

        List<PrometheusMetric> results = getMetricsByName(
                OpenTelemetryCollectorContainer.getInstance().fetchMetrics(DuplicateMetricResource1.METER_NAME),
                DuplicateMetricResource1.METER_NAME + "_total"); // Adjust for Prometheus naming conventions

        Assert.assertEquals(2, results.size());
        results.forEach(r -> Assert.assertEquals("" + REQUEST_COUNT, r.getValue()));
    }
}
