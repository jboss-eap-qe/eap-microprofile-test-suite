package org.jboss.eap.qe.microprofile.metrics;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.eap.qe.microprofile.metrics.namefellow.PingApplication;
import org.jboss.eap.qe.microprofile.metrics.namefellow.PingOneResource;
import org.jboss.eap.qe.microprofile.metrics.namefellow.PingOneService;
import org.jboss.eap.qe.microprofile.metrics.namefellow.PingTwoResource;
import org.jboss.eap.qe.microprofile.metrics.namefellow.PingTwoService;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Ignore;

/**
 * Deployment with sub-deployments scenario.
 */
@Ignore("https://issues.redhat.com/browse/WFLY-12854")
public class SubdeploymentNamefellowMetricsTest extends NamefellowMetricsTest {

    @Deployment
    public static EnterpriseArchive createDeployment() {

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class,
                SubdeploymentNamefellowMetricsTest.class.getSimpleName() + ".ear");

        ear.addAsModule(ShrinkWrap.create(WebArchive.class, "dep1.war")
                .addClasses(PingApplication.class, PingOneService.class, PingOneResource.class, PingTwoResource.class)
                .addAsManifestResource(new StringAsset("mp.metrics.appName=dep1"), "microprofile-config.properties")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml"));

        ear.addAsModule(ShrinkWrap.create(JavaArchive.class, "dep2.jar")
                .addClasses(PingTwoService.class)
                .addAsManifestResource(new StringAsset("mp.metrics.appName=dep2"), "microprofile-config.properties")
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

        ear.setApplicationXML(new StringAsset("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<application xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/application_7.xsd\" version=\"7\">\n"
                + "  <display-name>metrics</display-name>\n"
                + "  <module>\n"
                + "    <web>\n"
                + "      <web-uri>dep1.war</web-uri>\n"
                + "      <context-root>/dep</context-root>\n"
                + "    </web>\n"
                + "  </module>\n"
                + "  <module>\n"
                + "    <java>dep2.jar</java>\n"
                + "  </module>\n"
                + "</application>"));

        return ear;
    }

    @Override
    String getPingOneURL() {
        return "http://localhost:8080/dep1/ping-one";
    }

    @Override
    String getPingTwoURL() {
        return "http://localhost:8080/dep1/ping-two";
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
