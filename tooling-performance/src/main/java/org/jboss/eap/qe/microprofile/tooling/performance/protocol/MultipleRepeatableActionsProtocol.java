package org.jboss.eap.qe.microprofile.tooling.performance.protocol;

import java.util.concurrent.Callable;

import org.fusesource.jansi.Ansi;
import org.jboss.eap.qe.microprofile.tooling.performance.core.MeasurementException;
import org.jboss.eap.qe.microprofile.tooling.performance.core.StressTestException;
import org.jboss.eap.qe.microprofile.tooling.performance.core.StressTestProtocol;
import org.jboss.eap.qe.microprofile.tooling.performance.core.StressTester;

/**
 * A stress test protocol that performs multiple subsequent actions and asks the tester to perform a
 * measurement every number of attempts
 */
public class MultipleRepeatableActionsProtocol implements StressTestProtocol {

    private final int redeployIterations;
    private final int probingIntervalAttempts;
    private final Callable<Void> repeatableActions;

    public MultipleRepeatableActionsProtocol(int redeployIterations, int probingIntervalAttempts,
            Callable<Void> repeatableActions) {
        this.redeployIterations = redeployIterations;
        this.probingIntervalAttempts = probingIntervalAttempts;
        this.repeatableActions = repeatableActions;
    }

    @Override
    public void run(StressTester tester) throws StressTestException {

        //  run the whole specific protocol - i.e. deploy/updeploy several times and probe intermittently
        for (int i = 1; i <= redeployIterations; i++) {
            //  actions to be done at each iteration
            try {
                repeatableActions.call();
            } catch (Exception e) {
                throw new StressTestException(e);
            }
            System.out.println(Ansi.ansi().reset().a("[Protocol]: ").fgCyan()
                    .a(String.format("Attempt %d done...", i)).reset());
            if (i % (probingIntervalAttempts) == 0) {
                //  probe each PROBING_INTERVAL_ATTEMPTS
                try {
                    tester.probe();
                } catch (MeasurementException e) {
                    throw new StressTestException(e);
                }
            }
        }
    }
}
