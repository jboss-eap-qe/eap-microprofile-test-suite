package org.jboss.eap.qe.microprofile.metrics;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.eap.qe.microprofile.metrics.namefellow.PingApplication;
import org.jboss.eap.qe.microprofile.metrics.namefellow.PingOneResource;
import org.jboss.eap.qe.microprofile.metrics.namefellow.PingOneService;
import org.jboss.eap.qe.microprofile.metrics.namefellow.PingTwoResource;
import org.jboss.eap.qe.microprofile.metrics.namefellow.PingTwoService;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 * Multiple deployment scenario.
 */
public class MultipleDeploymentsNamefellowMetricsTest extends NamefellowMetricsTest {

    @Deployment(name = "dep1.war", order = 1)
    public static WebArchive createDeployment1() {
        return ShrinkWrap.create(WebArchive.class, "dep1.war")
                .addClasses(PingApplication.class, PingOneService.class, PingOneResource.class)
                .addAsManifestResource(new StringAsset("mp.metrics.appName=dep1"), "microprofile-config.properties");

    }

    @Deployment(name = "dep2.war", order = 2)
    public static WebArchive createDeployment2() {
        return ShrinkWrap.create(WebArchive.class, "dep2.war")
                .addClasses(PingApplication.class, PingTwoService.class, PingTwoResource.class)
                .addAsManifestResource(new StringAsset("mp.metrics.appName=dep2"), "microprofile-config.properties");
    }

    @Override
    String getPingOneURL() {
        return "http://localhost:8080/dep1/ping-one";
    }

    @Override
    String getPingTwoURL() {
        return "http://localhost:8080/dep2/ping-two";
    }

    @Override
    String getPingOneTag() {
        return "dep1";
    }

    @Override
    String getPingTwoTag() {
        return "dep2";
    }
}
