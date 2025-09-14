package org.jboss.eap.qe.microprofile.jwt.keycloak;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.arquillian.cube.docker.impl.client.config.Await;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.await.AwaitStrategy;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;

/**
 * Provides a custom waiting strategy for Arquillian Cube DOcker APIs to check that the Keycloak container was started
 * successfully.
 * <p>
 * Used by {@link KeycloakIntegrationHighLevelScenarioTest}
 * </p>
 */
public class KeycloakContainerAwaitStrategy implements AwaitStrategy {

    Await params;
    DockerClientExecutor dockerClientExecutor;
    Cube<?> cube;

    private static final long CONTAINER_READY_TIMEOUT = 2;
    private static final TimeUnit CONTAINER_READY_TIMEOUT_TIME_UNIT = TimeUnit.MINUTES;

    public KeycloakContainerAwaitStrategy() {
    }

    public void setCube(Cube<?> cube) {
        this.cube = cube;
    }

    public void setDockerClientExecutor(DockerClientExecutor dockerClientExecutor) {
        this.dockerClientExecutor = dockerClientExecutor;
    }

    public void setParams(Await params) {
        this.params = params;
    }

    @Override
    public boolean await() {
        try {
            Awaitility.await()
                    .atMost(CONTAINER_READY_TIMEOUT, CONTAINER_READY_TIMEOUT_TIME_UNIT)
                    .until(() -> isContainerReady(KeycloakConfigurator.KEYCLOAK_EXPOSED_HTTP_PORT));
        } catch (ConditionTimeoutException ex) {
            return false;
        }
        return true;
    }

    private static boolean isContainerReady(int port) {
        try {
            URL url = new URL("http://" + KeycloakConfigurator.KEYCLOAK_INSTANCE_HOSTNAME + ":" + port + "/health/ready");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            boolean ready = connection.getResponseCode() == HttpURLConnection.HTTP_OK;
            if (ready) {
                System.out.println(
                        "Let's wait additional 10 seconds before the post-start tasks (like creation of admin user) on container are done.");
                Thread.sleep(10000L);
            }
            return ready;
        } catch (Exception ex) {
            return false;
        }
    }
}
