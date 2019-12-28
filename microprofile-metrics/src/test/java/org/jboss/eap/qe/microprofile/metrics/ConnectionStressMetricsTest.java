package org.jboss.eap.qe.microprofile.metrics;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.eap.qe.microprofile.metrics.hello.MetricsApp;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.restassured.http.ContentType;

@RunWith(Arquillian.class)
public class ConnectionStressMetricsTest {

    @ContainerResource
    ManagementClient managementClient;
    @ArquillianResource
    private URL deploymentUrl;
    private URL metricsURL;

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, ConnectionStressMetricsTest.class.getSimpleName() + ".war")
                .addPackage(MetricsApp.class.getPackage())
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Before
    public void composeHealthEndpointURL() throws MalformedURLException {
        metricsURL = new URL("http://" + managementClient.getMgmtAddress() + ":" + managementClient.getMgmtPort() + "/metrics");
    }

    private HttpURLConnection getHTTPConn(URL url) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setReadTimeout(30 * 1000);
        connection.connect();

        return connection;
    }

    /**
     * @tpTestDetails Negative scenario to verify metrics are available when lots of connections are opened
     * @tpPassCrit Counter metric is incremented according to number of invocation of the CDI bean even if there are lots
     *             of opened connections.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @RunAsClient
    public void stressTest() throws Exception {
        final List<HttpURLConnection> connections = new ArrayList<>(2000);
        try {
            for (int i = 0; i < 1000; i++) {
                connections.add(getHTTPConn(deploymentUrl));
            }
            for (int i = 0; i < 1000; i++) {
                connections.add(getHTTPConn(metricsURL));
            }

            for (int i = 0; i < 10; i++) {

                get(deploymentUrl.toString()).then().statusCode(200);
            }

            given()
                    .baseUri(metricsURL.toString())
                    .accept(ContentType.JSON)
                    .get().then()
                    .contentType(ContentType.JSON)
                    .header("Content-Type", containsString("application/json"))
                    .body("$", hasKey("application"),
                            "application", hasKey("hello-count"),
                            "application.hello-count", equalTo(10));

        } finally {
            connections.forEach(HttpURLConnection::disconnect);
        }
    }
}
