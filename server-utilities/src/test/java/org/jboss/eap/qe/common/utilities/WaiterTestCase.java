package org.jboss.eap.qe.common.utilities;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.jboss.eap.qe.microprofile.common.utilities.Waiter;
import org.junit.Assert;
import org.junit.Test;

public class WaiterTestCase {

    @Test
    public void testWaitingForConditionSucceeds() throws InterruptedException {
        final Date timeAtStart = new Date();

        final boolean conditionSatisfied = Waiter.waitFor(() -> new Date().getTime() - timeAtStart.getTime() > 1200, 2,
                TimeUnit.SECONDS);

        Assert.assertTrue("Condition was not satisfied within time frame!", conditionSatisfied);
    }

    @Test
    public void testWaitingForConditionFails() throws InterruptedException {
        final boolean conditionSatisfied = Waiter.waitFor(() -> false, 600, TimeUnit.MILLISECONDS);

        Assert.assertFalse("Condition was satisfied - weird, huh?", conditionSatisfied);
    }
}
