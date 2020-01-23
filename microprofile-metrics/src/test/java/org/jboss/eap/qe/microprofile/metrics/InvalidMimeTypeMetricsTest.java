package org.jboss.eap.qe.microprofile.metrics;

import static io.restassured.RestAssured.given;

import java.io.IOException;
import java.net.MalformedURLException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.ConfigurationException;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.ArquillianContainerProperties;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.ArquillianDescriptorWrapper;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.restassured.specification.RequestSpecification;

@RunWith(Arquillian.class)
public class InvalidMimeTypeMetricsTest {

    @Deployment
    public static JavaArchive createDeployment() {

        return ShrinkWrap.create(JavaArchive.class, InvalidMimeTypeMetricsTest.class.getSimpleName() + ".jar")
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    private static RequestSpecification metricsRequest;

    @BeforeClass
    public static void composeMetricsEndpointURL() throws MalformedURLException, ConfigurationException {
        ArquillianContainerProperties arqProps = new ArquillianContainerProperties(
                ArquillianDescriptorWrapper.getArquillianDescriptor());
        String metricsURL = "http://" + arqProps.getDefaultManagementAddress() + ":" + arqProps.getDefaultManagementPort()
                + "/metrics";
        metricsRequest = given().baseUri(metricsURL);
    }

    /**
     * @tpTestDetails Negative scenario to verify correct response when HTTP OPTIONS method accept header contains wrong
     *                MIME type - application/yaml
     * @tpPassCrit HTTP return code is 406
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @RunAsClient
    public void appYamlOptions() throws IOException {
        metricsRequest.header("Accept", "application/yaml")
                .options()
                .then()
                .statusCode(406);
    }

    /**
     * @tpTestDetails Negative scenario to verify correct response when HTTP OPTIONS method accept header contains wrong
     *                MIME type - application/video
     * @tpPassCrit HTTP return code is 406
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @RunAsClient
    public void appVideoOptions() throws IOException {
        metricsRequest.header("Accept", "application/video")
                .options()
                .then()
                .statusCode(406);
    }

    /**
     * @tpTestDetails Negative scenario to verify correct response when HTTP OPTIONS method accept header contains wrong
     *                MIME type - audio/mpeg
     * @tpPassCrit HTTP return code is 406
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @RunAsClient
    public void audioMpegOptions() throws IOException {
        metricsRequest.header("Accept", "audio/mpeg")
                .options()
                .then()
                .statusCode(406);
    }

    /**
     * @tpTestDetails Negative scenario to verify correct response when HTTP OPTIONS method accept header contains wrong
     *                MIME type - text/csv
     * @tpPassCrit HTTP return code is 406
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @RunAsClient
    public void textCSVOptions() throws IOException {
        metricsRequest.header("Accept", "text/csv")
                .options()
                .then()
                .statusCode(406);
    }

    /**
     * @tpTestDetails Negative scenario to verify correct response when HTTP OPTIONS method accept header contains wrong
     *                MIME type - application/octet-stream
     * @tpPassCrit HTTP return code is 406
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @RunAsClient
    public void appOctetOptions() throws IOException {
        metricsRequest.header("Accept", "application/octet-stream")
                .options()
                .then()
                .statusCode(406);
    }

    /**
     * @tpTestDetails Negative scenario to verify correct response when HTTP OPTIONS method accept header contains wrong
     *                MIME type - text/html
     * @tpPassCrit HTTP return code is 406
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @RunAsClient
    public void textHTMLOptions() throws IOException {
        metricsRequest.header("Accept", "text/html")
                .options()
                .then()
                .statusCode(406);
    }

    /**
     * @tpTestDetails Negative scenario to verify correct response when HTTP OPTIONS method accept header contains wrong
     *                MIME type - text/json
     * @tpPassCrit HTTP return code is 406
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @RunAsClient
    public void textJSONOptions() throws IOException {
        metricsRequest.header("Accept", "text/json")
                .options()
                .then()
                .statusCode(406);
    }

    /**
     * @tpTestDetails Negative scenario to verify correct response when HTTP OPTIONS method accept header contains wrong
     *                MIME type - text/x-plain
     * @tpPassCrit HTTP return code is 406
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @RunAsClient
    public void textXPlainOptions() throws IOException {
        metricsRequest.header("Accept", "text/x-plain")
                .options()
                .then()
                .statusCode(406);
    }

    /**
     * @tpTestDetails Negative scenario to verify correct response when HTTP GET method accept header contains wrong MIME
     *                type - application/yaml
     * @tpPassCrit HTTP return code is 406
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @RunAsClient
    public void appYamlGet() throws IOException {
        metricsRequest.header("Accept", "application/yaml")
                .get()
                .then()
                .statusCode(406);
    }

    /**
     * @tpTestDetails Negative scenario to verify correct response when HTTP GET method accept header contains wrong MIME
     *                type - application/video
     * @tpPassCrit HTTP return code is 406
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @RunAsClient
    public void appVideoGet() throws IOException {
        metricsRequest.header("Accept", "application/video")
                .get()
                .then()
                .statusCode(406);
    }

    /**
     * @tpTestDetails Negative scenario to verify correct response when HTTP GET method accept header contains wrong MIME
     *                type - audio/mpeg
     * @tpPassCrit HTTP return code is 406
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @RunAsClient
    public void audioMpegGet() throws IOException {
        metricsRequest.header("Accept", "audio/mpeg")
                .get()
                .then()
                .statusCode(406);
    }

    /**
     * @tpTestDetails Negative scenario to verify correct response when HTTP GET method accept header contains wrong MIME
     *                type - text/csv
     * @tpPassCrit HTTP return code is 406
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @RunAsClient
    public void textCSVGet() throws IOException {
        metricsRequest.header("Accept", "text/csv")
                .get()
                .then()
                .statusCode(406);
    }

    /**
     * @tpTestDetails Negative scenario to verify correct response when HTTP GET method accept header contains wrong MIME
     *                type - application/octet-stream
     * @tpPassCrit HTTP return code is 406
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @RunAsClient
    public void appOctetGet() throws IOException {
        metricsRequest.header("Accept", "application/octet-stream")
                .get()
                .then()
                .statusCode(406);
    }

    /**
     * @tpTestDetails Negative scenario to verify correct response when HTTP GET method accept header contains wrong MIME
     *                type - text/html
     * @tpPassCrit HTTP return code is 406
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @RunAsClient
    public void textHTMLGet() throws IOException {
        metricsRequest.header("Accept", "text/html")
                .get()
                .then()
                .statusCode(406);
    }

    /**
     * @tpTestDetails Negative scenario to verify correct response when HTTP GET method accept header contains wrong MIME
     *                type - text/json
     * @tpPassCrit HTTP return code is 406
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @RunAsClient
    public void textJSONGet() throws IOException {
        metricsRequest.header("Accept", "text/json")
                .get()
                .then()
                .statusCode(406);
    }

    /**
     * @tpTestDetails Negative scenario to verify correct response when HTTP GET method accept header contains wrong MIME
     *                type - text/x-plain
     * @tpPassCrit HTTP return code is 406
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @RunAsClient
    public void textXPlainGet() throws IOException {
        metricsRequest.header("Accept", "text/x-plain")
                .get()
                .then()
                .statusCode(406);
    }
}
