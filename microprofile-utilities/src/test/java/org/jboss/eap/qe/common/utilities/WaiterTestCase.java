package org.jboss.eap.qe.common.utilities;

import org.jboss.eap.qe.microprofile.common.utilities.Waiter;
import org.junit.Assert;
import org.junit.Test;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class WaiterTestCase {

    @Test
    public void testWaitingForConditionSucceeds() throws InterruptedException {
        final Date timeAtStart = new Date();

        final boolean conditionSatisfied = Waiter.waitFor(() -> new Date().getTime() - timeAtStart.getTime() > 4000, 5, TimeUnit.SECONDS);

        Assert.assertTrue("Condition was not satisfied within time frame!", conditionSatisfied);
    }

    @Test
    public void testWaitingForConditionFails() throws InterruptedException {
        final boolean conditionSatisfied = Waiter.waitFor(() -> false, 3, TimeUnit.SECONDS);

        Assert.assertFalse("Condition was satisfied - weird, huh?", conditionSatisfied);
    }
}
