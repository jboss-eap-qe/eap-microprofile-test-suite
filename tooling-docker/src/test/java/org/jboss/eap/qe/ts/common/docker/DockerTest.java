package org.jboss.eap.qe.ts.common.docker;

import io.restassured.RestAssured;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;

import static org.hamcrest.Matchers.containsString;

public class DockerTest {

    private static final String DEFAULT_SERVER_BIND_ADDRESS = "127.0.0.1";
    private static final String CONTAINER_NAME_WILDFLY_1 = "wildfly1";
    private static final String CONTAINER_NAME_WILDFLY_2 = "wildfly2";
    private static final int EXPOSED_HTTP_PORT_WILDFLY_1 = 11111;
    private static final int EXPOSED_HTTP_PORT_WILDFLY_2 = 22222;
    private static final int EXPOSED_MANAGEMENT_PORT_WILDFLY_1 = 11990;
    private static final int EXPOSED_MANAGEMENT_PORT_WILDFLY_2 = 22990;

    @ClassRule
    public static Docker wildflyContainer = new Docker.Builder(CONTAINER_NAME_WILDFLY_1, "jboss/wildfly:18.0.0.Final")
            .setContainerReadyTimeout(60000)
            .setContainerReadyCondition(() -> {
                try {
                    URL url = new URL("http://" + DEFAULT_SERVER_BIND_ADDRESS + ":" + EXPOSED_MANAGEMENT_PORT_WILDFLY_1 + "/health");
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setRequestMethod("GET");
                    if (con.getResponseMessage().contains("OK")) {
                        return true;
                    }
                } catch (Exception ex) {
                    // exceptions means not ready
                    return false;
                }
                return false;
            })
            .wihtPortMapping(EXPOSED_HTTP_PORT_WILDFLY_1 + ":8080")
            .wihtPortMapping(EXPOSED_MANAGEMENT_PORT_WILDFLY_1 + ":9990")
            .withCmdArg("/opt/jboss/wildfly/bin/standalone.sh")
            .withCmdArg("-b=0.0.0.0")
            .withCmdArg("-bmanagement=0.0.0.0")
            .build();

    @ClassRule
    public static Docker wildflyContainer2 = new Docker.Builder(CONTAINER_NAME_WILDFLY_2, "jboss/wildfly:18.0.0.Final")
            .setContainerReadyTimeout(60000)
            .setContainerReadyCondition(() -> {
                try {
                    URL url = new URL("http://" + DEFAULT_SERVER_BIND_ADDRESS + ":" + EXPOSED_MANAGEMENT_PORT_WILDFLY_2 + "/health");
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setRequestMethod("GET");
                    if (con.getResponseMessage().contains("OK")) {
                        return true;
                    }
                } catch (Exception ex) {
                    // exceptions means not ready
                    return false;
                }
                return false;
            })
            .wihtPortMapping(EXPOSED_HTTP_PORT_WILDFLY_2 + ":8080")
            .wihtPortMapping(EXPOSED_MANAGEMENT_PORT_WILDFLY_2 + ":9990")
            .withCmdArg("/opt/jboss/wildfly/bin/standalone.sh")
            .withCmdArg("-b=0.0.0.0")
            .withCmdArg("-bmanagement=0.0.0.0")
            .build();

    @Test
    public void testHttpPortMappingWildfly1() {
            Assert.assertTrue(CONTAINER_NAME_WILDFLY_1 + " container does not listion on port " + EXPOSED_HTTP_PORT_WILDFLY_1,
                    isPortOpened(DEFAULT_SERVER_BIND_ADDRESS, EXPOSED_HTTP_PORT_WILDFLY_1));
    }

    @Test
    public void testManagementPortMappingWildfly1() {
        Assert.assertTrue(CONTAINER_NAME_WILDFLY_1 + " container does not listion on port " + EXPOSED_MANAGEMENT_PORT_WILDFLY_1,
                isPortOpened(DEFAULT_SERVER_BIND_ADDRESS, EXPOSED_MANAGEMENT_PORT_WILDFLY_1));
    }

    @Test
    public void testHttpPortMappingWildfly2() {
        Assert.assertTrue(CONTAINER_NAME_WILDFLY_2 + " container does not listion on port " + EXPOSED_HTTP_PORT_WILDFLY_2,
                isPortOpened(DEFAULT_SERVER_BIND_ADDRESS, EXPOSED_HTTP_PORT_WILDFLY_2));
    }

    @Test
    public void testManagementPortMappingWildfly2() {
        Assert.assertTrue(CONTAINER_NAME_WILDFLY_2 + " container does not listion on port " + EXPOSED_MANAGEMENT_PORT_WILDFLY_2,
                isPortOpened(DEFAULT_SERVER_BIND_ADDRESS, EXPOSED_MANAGEMENT_PORT_WILDFLY_2));
    }

    /**
     * Returns true if port on given addrress is open, otherwise false
     *
     * @param address IP address
     * @param port port to check
     */
    private boolean isPortOpened(String address, int port) {
        try {
            new Socket(address, port).close();
            return true;
        } catch (Exception ex)  {
            return false;
        }
    }

    @Test
    public void testWildlfy1WelcomePage() {
        RestAssured.when().get("http://" + DEFAULT_SERVER_BIND_ADDRESS + ":" + EXPOSED_HTTP_PORT_WILDFLY_1).then().assertThat()
                .body(containsString("Welcome to WildFly"));
    }

    @Test
    public void testWildlfy2WelcomePage() {
        RestAssured.when().get("http://" + DEFAULT_SERVER_BIND_ADDRESS + ":" + EXPOSED_HTTP_PORT_WILDFLY_2).then().assertThat()
                .body(containsString("Welcome to WildFly"));
    }

    @Test
    public void testWildlfy1HealthCheck() {
        RestAssured.when().get("http://" + DEFAULT_SERVER_BIND_ADDRESS + ":" + EXPOSED_MANAGEMENT_PORT_WILDFLY_1 + "/health").then().assertThat()
                .body(containsString("UP"));
    }

    @Test
    public void testWildlfy2HealthCheck() {
        RestAssured.when().get("http://" + DEFAULT_SERVER_BIND_ADDRESS + ":" + EXPOSED_MANAGEMENT_PORT_WILDFLY_2 + "/health").then().assertThat()
                .body(containsString("UP"));
    }
}