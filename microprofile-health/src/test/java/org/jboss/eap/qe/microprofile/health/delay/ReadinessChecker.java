package org.jboss.eap.qe.microprofile.health.delay;

import static io.restassured.RestAssured.get;
import static org.jboss.eap.qe.microprofile.health.delay.ReadinessState.DOWN_NO_CHECK;
import static org.jboss.eap.qe.microprofile.health.delay.ReadinessState.DOWN_NO_CONTENT;
import static org.jboss.eap.qe.microprofile.health.delay.ReadinessState.UNABLE_TO_CONNECT;
import static org.jboss.eap.qe.microprofile.health.delay.ReadinessState.UP_NO_CHECK;
import static org.jboss.eap.qe.microprofile.health.delay.ReadinessState.UP_WITH_CHECK;
import static org.jboss.eap.qe.microprofile.health.delay.ReadinessState.UP_WITH_DEFAULT_CHECK;

import java.net.ConnectException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.eap.qe.microprofile.health.tools.HealthUrlProvider;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.ArquillianContainerProperties;

import io.restassured.response.Response;

/**
 * Performs calls to the readiness health checks probes and stores the returned states
 */
public class ReadinessChecker implements Callable<Boolean> {

    private AtomicBoolean shouldStop = new AtomicBoolean(false);

    private List<ReadinessState> states = new CopyOnWriteArrayList<>();

    public static final String DEFAULT_READINESS_CHECK_NAME_PREFIX = "ready-deployment.";

    private static ArquillianContainerProperties arqProps;

    public ReadinessChecker(ArquillianContainerProperties arqProps) {
        this.arqProps = arqProps;
    }

    @Override
    public Boolean call() {
        addState(ReadinessState.START());
        while (!shouldStop.get()) {
            try {
                Response response = get(HealthUrlProvider.readyEndpoint(arqProps));
                String responseContent = response.then().extract().asString();
                if (!responseContent.isEmpty()) {
                    if (response.getBody().path("checks") instanceof List) {
                        List<Map<String, String>> checks = response.getBody().path("checks");
                        if (response.getStatusCode() == 503) {
                            if (checks == null) {
                                addState(DOWN_NO_CONTENT());
                            } else if (checks.size() == 0) {
                                addState(DOWN_NO_CHECK());
                            } else if (checks.get(0).get("name").equals(DelayedReadinessHealthCheck.NAME)) {
                                addState(ReadinessState.DOWN_WITH_CHECK());
                            }
                        } else if (response.getStatusCode() == 200) {
                            if (checks == null) {
                                throw new RuntimeException("Readiness probe is UP (200) however missing JSON content");
                            }
                            if (checks.size() == 0) {
                                addState(UP_NO_CHECK());
                            } else if (checks.get(0).get("name").equals(DelayedReadinessHealthCheck.NAME)) {
                                addState(UP_WITH_CHECK());
                            } else if (checks.get(0).get("name").startsWith(DEFAULT_READINESS_CHECK_NAME_PREFIX)) {
                                addState(UP_WITH_DEFAULT_CHECK());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                if (e instanceof ConnectException) {
                    addState(UNABLE_TO_CONNECT());
                } else {
                    throw new RuntimeException(e);
                }
            }
        }
        addState(ReadinessState.END());
        return true;
    }

    private void addState(ReadinessState state) {
        if (states.isEmpty() || !states.get(lastIndexOfStates()).equals(state)) {
            states.add(state);
        }
    }

    private int lastIndexOfStates() {
        return states.size() - 1;
    }

    public void stop() {
        shouldStop.set(true);
    }

    public List<ReadinessState> getStates() {
        return Collections.unmodifiableList(states);
    }
}
