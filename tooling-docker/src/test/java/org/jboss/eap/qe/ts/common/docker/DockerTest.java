package org.jboss.eap.qe.ts.common.docker;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpStatus;
import org.jboss.eap.qe.ts.common.docker.junit.DockerRequiredTests;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(DockerRequiredTests.class)
public class DockerTest {

    private static final String DEFAULT_SERVER_BIND_ADDRESS = "127.0.0.1";

    private static final String WILDFLY_ONE_CONTAINER_NAME = "wildfly-server-one";
    private static final int WILDFLY_ONE_EXPOSED_HTTP_PORT = 11111;
    private static final int WILDFLY_ONE_EXPOSED_MANAGEMENT_PORT = 11990;

    private static final String WILDFLY_TWO_CONTAINER_NAME = "wildfly-server-two";
    private static final int WILDFLY_TWO_EXPOSED_HTTP_PORT = 22222;
    private static final int WILDFLY_TWO_EXPOSED_MANAGEMENT_PORT = 22990;

    @ClassRule
    public static Docker wildFlyOne = new Docker.Builder(WILDFLY_ONE_CONTAINER_NAME,
            "quay.io/wildfly/wildfly")
            .setContainerReadyTimeout(2, TimeUnit.MINUTES)
            .setContainerReadyCondition(DockerTest::isWildFlyOneReady)
            .withPortMapping(WILDFLY_ONE_EXPOSED_HTTP_PORT + ":8080")
            .withPortMapping(WILDFLY_ONE_EXPOSED_MANAGEMENT_PORT + ":9990")
            .withCmdArg("/opt/jboss/wildfly/bin/standalone.sh")
            .withCmdArg("-b=0.0.0.0")
            .withCmdArg("-bmanagement=0.0.0.0")
            .build();

    @ClassRule
    public static Docker wildFlyTwo = new Docker.Builder(WILDFLY_TWO_CONTAINER_NAME,
            "quay.io/wildfly/wildfly")
            .setContainerReadyTimeout(2, TimeUnit.MINUTES)
            .setContainerReadyCondition(DockerTest::isWildFlyTwoReady)
            .withPortMapping(WILDFLY_TWO_EXPOSED_HTTP_PORT + ":8080")
            .withPortMapping(WILDFLY_TWO_EXPOSED_MANAGEMENT_PORT + ":9990")
            .withCmdArg("/opt/jboss/wildfly/bin/standalone.sh")
            .withCmdArg("-b=0.0.0.0")
            .withCmdArg("-bmanagement=0.0.0.0")
            .build();

    private static boolean isWildFlyOneReady() {
        return isContainerReady(WILDFLY_ONE_EXPOSED_HTTP_PORT);
    }

    private static boolean isWildFlyTwoReady() {
        return isContainerReady(WILDFLY_TWO_EXPOSED_HTTP_PORT);
    }

    private static boolean isContainerReady(int port) {
        try {
            URL url = new URL("http://" + DEFAULT_SERVER_BIND_ADDRESS + ":" + port);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            return connection.getResponseCode() == HttpStatus.SC_OK;
        } catch (Exception ex) {
            return false;
        }
    }

    private static boolean portOpened(int port) {
        try {
            new Socket(DEFAULT_SERVER_BIND_ADDRESS, port).close();
        } catch (Exception ex) {
            return false;
        }
        return true;
    }

    @Test
    public void testHttpPortMappingForWildFlyOne() {
        assertThat(WILDFLY_ONE_CONTAINER_NAME + " container is not listening on port " + WILDFLY_ONE_EXPOSED_HTTP_PORT,
                portOpened(WILDFLY_ONE_EXPOSED_HTTP_PORT), is(true));
    }

    @Test
    public void testManagementPortMappingForWildFlyOne() {
        assertThat(WILDFLY_ONE_CONTAINER_NAME + " container is not listening on port " + WILDFLY_ONE_EXPOSED_MANAGEMENT_PORT,
                portOpened(WILDFLY_ONE_EXPOSED_MANAGEMENT_PORT), is(true));
    }

    @Test
    public void testHttpPortMappingForWildFlyTwo() {
        assertThat(WILDFLY_TWO_CONTAINER_NAME + " container is not listening on port " + WILDFLY_TWO_EXPOSED_HTTP_PORT,
                portOpened(WILDFLY_TWO_EXPOSED_HTTP_PORT), is(true));
    }

    @Test
    public void testManagementPortMappingForWildFlyTwo() {
        assertThat(WILDFLY_TWO_CONTAINER_NAME + " container is not listening on port " + WILDFLY_TWO_EXPOSED_MANAGEMENT_PORT,
                portOpened(WILDFLY_TWO_EXPOSED_MANAGEMENT_PORT), is(true));
    }
}
