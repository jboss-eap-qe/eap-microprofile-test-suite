package org.jboss.eap.qe.microprofile.manual.mode.health.delay;

import java.sql.Timestamp;
import java.util.Date;

public class ReadinessState {

    private final ReadinessStateType state;
    private final long time;

    /**
     * beginning of the list of states (to determine start)
     */
    public static ReadinessState START() {
        return new ReadinessState(ReadinessStateType.START);
    }

    /**
     * unable to connect ({@link java.net.ConnectException} is thrown)
     */
    public static ReadinessState UNABLE_TO_CONNECT() {
        return new ReadinessState(ReadinessStateType.UNABLE_TO_CONNECT);
    }

    /**
     * Return code 200, no custom checks
     */
    public static ReadinessState UP_NO_CHECK() {
        return new ReadinessState(ReadinessStateType.UP_WITHOUT_CUSTOM_CHECK);
    }

    /**
     * Return code 200, {@link DelayedReadinessHealthCheck#NAME} is in custom checks
     */
    public static ReadinessState UP_WITH_CHECK() {
        return new ReadinessState(ReadinessStateType.UP_WITH_CUSTOM_CHECK);
    }

    /**
     * Return code 200, check name starts with {@link ReadinessChecker#DEFAULT_READINESS_CHECK_NAME_PREFIX}
     */
    public static ReadinessState UP_WITH_DEFAULT_CHECK() {
        return new ReadinessState(ReadinessStateType.UP_WITH_DEFAULT_CHECK);
    }

    /**
     * Return code 503, no custom checks
     */
    public static ReadinessState DOWN_NO_CHECK() {
        return new ReadinessState(ReadinessStateType.DOWN_WITHOUT_CUSTOM_CHECK);
    }

    /**
     * Return code 503, {@link DelayedReadinessHealthCheck#NAME} is in custom checks
     */
    public static ReadinessState DOWN_WITH_CHECK() {
        return new ReadinessState(ReadinessStateType.DOWN_WITH_CUSTOM_CHECK);
    }

    /**
     * Return code 503 without any content...typically returned during shutting down the server
     */
    public static ReadinessState DOWN_NO_CONTENT() {
        return new ReadinessState(ReadinessStateType.DOWN_NO_CONTENT);
    }

    /**
     * end of the list of states (to determine end)
     */
    public static ReadinessState END() {
        return new ReadinessState(ReadinessStateType.END);
    }

    private ReadinessState(ReadinessStateType state) {
        this.state = state;
        time = new Date().getTime();
    }

    @Override
    public String toString() {
        return state.toString();
    }

    public String toString(boolean withTimestamp) {
        return state.toString() + (withTimestamp ? " at " + new Timestamp(time) : "");
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ReadinessState) {
            ReadinessState state = (ReadinessState) obj;
            return this.state == state.state;
        }
        return false;
    }

    private enum ReadinessStateType {
        START("start"),
        UNABLE_TO_CONNECT("unable to connect"),
        UP_WITHOUT_CUSTOM_CHECK("UP (200), custom check NOT installed"),
        UP_WITH_CUSTOM_CHECK("UP (200), custom check installed"),
        UP_WITH_DEFAULT_CHECK("UP (200), default check installed"),
        DOWN_WITHOUT_CUSTOM_CHECK("DOWN (503), custom check NOT installed"),
        DOWN_WITH_CUSTOM_CHECK("DOWN (503), custom check installed"),
        DOWN_NO_CONTENT("DOWN (503), no JSON content"),
        END("end"),
        ;

        private final String description;

        ReadinessStateType(String description) {
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }
}
