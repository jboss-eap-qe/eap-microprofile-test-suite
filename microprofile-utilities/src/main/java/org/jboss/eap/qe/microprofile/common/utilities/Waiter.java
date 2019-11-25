package org.jboss.eap.qe.microprofile.common.utilities;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Class implementing waiting on condition functionality
 */
public final class Waiter {

    private Waiter() {
        //intentionally left empty
    }

    /**
     * Wait for condition to become true
     * @param condition condition which will be checked
     * @param time time for which it will be waited. The wait will take this amount or less if condition is
     *             satisfied.
     * @param timeUnit time unit of wait
     * @return true if condition was satisfied anytime during wait, false otherwise
     * @throws InterruptedException
     */
    public static boolean waitFor(final Condition condition, final long time, final TimeUnit timeUnit) throws InterruptedException {
        final long start = new Date().getTime();
        final long timeout = timeUnit.toMillis(time);
        while (new Date().getTime() - start < timeout) {
            if (condition.check()) {
                return true;
            }
            Thread.sleep(500);
        }
        return false;
    }

    @FunctionalInterface
    public interface Condition {

        /**
         * Check a condition
         * @return true if a condition was satisfied, false otherwise.
         */
        boolean check();

    }

}
