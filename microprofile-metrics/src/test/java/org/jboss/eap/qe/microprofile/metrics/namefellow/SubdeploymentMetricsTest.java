package org.jboss.eap.qe.microprofile.metrics.namefellow;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;

import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.ConfigurationException;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.ArquillianContainerProperties;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.ArquillianDescriptorWrapper;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.deployment.ConfigurationUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

/**
 * Deployment with sub-deployments scenario.
 */
@Ignore("https://issues.redhat.com/browse/WFLY-12854")
@RunWith(Arquillian.class)
public class SubdeploymentMetricsTest {

    public static final String PING_APP = "ping-app";
    public static final String PING_TWO_SERVICE = "ping-two-service";
    public static final String PING_ONE_SERVICE_TAG = "ping-one-service-tag";
    public static final String PING_TWO_SERVICE_TAG = "ping-two-service-tag";

    @Deployment(name = PING_APP)
    public static EnterpriseArchive createDeployment() {

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class,
                SubdeploymentMetricsTest.class.getSimpleName() + ".ear");

        ear.addAsModule(ShrinkWrap.create(WebArchive.class, PING_APP + ".war")
                .addClasses(PingApplication.class, PingOneService.class, PingOneResource.class, PingTwoResource.class)
                .addAsManifestResource(new StringAsset("mp.metrics.appName=" + PING_ONE_SERVICE_TAG),
                        "microprofile-config.properties")
                .addAsWebInfResource(new StringAsset(ConfigurationUtil.BEANS_XML_FILE_CONTENT), "beans.xml"));

        ear.addAsModule(ShrinkWrap.create(JavaArchive.class, PING_TWO_SERVICE + ".jar")
                .addClasses(PingTwoService.class)
                .addAsManifestResource(new StringAsset("mp.metrics.appName=" + PING_TWO_SERVICE_TAG),
                        "microprofile-config.properties")
                .addAsManifestResource(new StringAsset(ConfigurationUtil.BEANS_XML_FILE_CONTENT), "beans.xml"));

        ear.setApplicationXML(new StringAsset("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<application xmlns=\"https://jakarta.ee/xml/ns/jakartaee\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/application_9.xsd\" version=\"9\">\n"
                + "  <display-name>metrics</display-name>\n"
                + "  <module>\n"
                + "    <web>\n"
                + "      <web-uri>" + PING_APP + ".war</web-uri>\n"
                + "      <context-root>/dep</context-root>\n"
                + "    </web>\n"
                + "  </module>\n"
                + "  <module>\n"
                + "    <java>" + PING_TWO_SERVICE + ".jar</java>\n"
                + "  </module>\n"
                + "</application>"));

        return ear;
    }

    private static RequestSpecification jsonMetricsRequest;
    private static RequestSpecification textMetricsRequest;

    @BeforeClass
    public static void prepare() throws ConfigurationException {
        ArquillianContainerProperties arqProps = new ArquillianContainerProperties(
                ArquillianDescriptorWrapper.getArquillianDescriptor());
        String url = "http://" + arqProps.getDefaultManagementAddress() + ":" + arqProps.getDefaultManagementPort()
                + "/metrics";
        jsonMetricsRequest = given()
                .baseUri(url)
                .accept(ContentType.JSON);
        textMetricsRequest = given()
                .baseUri(url)
                .accept(ContentType.TEXT);
    }

    /**
     * @tpTestDetails High level scenario to verify two none-reusable counter metrics of the same name are registered
     *                and tagged properly. The information is available under {@code /metrics} endpoint via HTTP OPTIONS.
     *                Metrics are in separate archives - sub-deployment scenario.
     * @tpPassCrit Metrics are tagged properly
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @RunAsClient
    public void applicationMetricsAreRegisteredAtDeploymentTime() {
        jsonMetricsRequest.options().then()
                .contentType(ContentType.JSON)
                .header("Content-Type", containsString("application/json"))
                .body("$", hasKey("application"),
                        "application", hasKey("ping-count"),
                        "application.ping-count", hasKey("tags"), // 11 at `/` + 1 at `/another-hello`
                        "application.ping-count.tags", hasSize(2),
                        "application.ping-count.tags[0]", hasSize(1),
                        "application.ping-count.tags[1]", hasSize(1),
                        "application.ping-count.tags.flatten()",
                        contains("_app=" + PING_ONE_SERVICE_TAG, "_app=" + PING_TWO_SERVICE_TAG));
    }

    /**
     * @tpTestDetails High level scenario to verify two none-reusable counter metrics of the same name are incremented
     *                properly according to the number of a CDI beans invocation.
     *                Metrics are in separate archives - sub-deployment scenario.
     * @tpPassCrit Counters have correct values (according to number of the CDI bean invocations) in JSON and prometheus format.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @RunAsClient
    public void dataTest(@ArquillianResource @OperateOnDeployment(PING_APP) URL pingUrl) {

        get(pingUrl.toString() + PingOneResource.RESOURCE)
                .then()
                .statusCode(200)
                .body(equalTo(PingOneService.MESSAGE));

        get(pingUrl.toString() + PingTwoResource.RESOURCE)
                .then()
                .statusCode(200)
                .body(equalTo(PingTwoService.MESSAGE));

        get(pingUrl.toString() + PingTwoResource.RESOURCE).then().statusCode(200);
        get(pingUrl.toString() + PingTwoResource.RESOURCE).then().statusCode(200);
        get(pingUrl.toString() + PingTwoResource.RESOURCE).then().statusCode(200);

        get(pingUrl.toString() + PingOneResource.RESOURCE).then().statusCode(200);

        jsonDataTest();
        prometheusDataTest();
    }

    /**
     * Verify correct data of counters in JSON format. ping one: 2, ping-two: 4
     */
    private void jsonDataTest() {
        jsonMetricsRequest.get().then()
                .contentType(ContentType.JSON)
                .header("Content-Type", containsString("application/json"))
                .body("$", hasKey("application"),
                        "application", hasKey("ping-count;_app=" + PING_ONE_SERVICE_TAG),
                        "application.ping-count;_app=" + PING_ONE_SERVICE_TAG, equalTo(2),

                        "application", hasKey("ping-count;_app=" + PING_TWO_SERVICE_TAG),
                        "application.ping-count;_app=" + PING_TWO_SERVICE_TAG, equalTo(4));
    }

    /**
     * Verify correct data of counters in prometheus format. ping one: 2, ping-two: 4
     */
    private void prometheusDataTest() {
        textMetricsRequest.get().then()
                .contentType(ContentType.TEXT)
                .header("Content-Type", containsString("text/plain"))
                .body(
                        containsString("application_ping_count_total{_app=\"" + PING_TWO_SERVICE_TAG + "\"} 4.0"),
                        containsString("application_ping_count_total{_app=\"" + PING_ONE_SERVICE_TAG + "\"} 2.0"));
    }
}
