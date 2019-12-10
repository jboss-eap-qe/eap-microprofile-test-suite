package org.jboss.eap.qe.microprofile.common.utilities;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

/**
 * Class implementing waiting on condition functionality
 */
public final class Waiter {

    private Waiter() {
        //intentionally left empty
    }

    /**
     * Wait for condition to become true. First check is done without additional wait and there is delay of 500
     * milliseconds between checks.
     * @param condition condition which will be checked
     * @param time time for which it will be waited. The wait will take this amount or less if condition is
     *             satisfied.
     * @param timeUnit time unit of wait
     * @return true if condition was satisfied anytime during wait, false otherwise
     * @throws InterruptedException
     */
    public static boolean waitFor(final BooleanSupplier condition, final long time, final TimeUnit timeUnit) throws InterruptedException {
        final long start = new Date().getTime();
        final long timeout = timeUnit.toMillis(time);
        while (new Date().getTime() - start < timeout) {
            if (condition.getAsBoolean()) {
                return true;
            }
            Thread.sleep(500);
        }
        return false;
    }

}
