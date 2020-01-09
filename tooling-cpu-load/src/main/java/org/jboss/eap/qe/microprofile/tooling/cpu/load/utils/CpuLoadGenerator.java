package org.jboss.eap.qe.microprofile.tooling.cpu.load.utils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates load on CPU. Takes one argument - load duration. Specifies for how long to generate load. Default 1 min.
 */
public class CpuLoadGenerator implements Runnable {

    /**
     * How many threads exhausting CPU will be started
     */
    private static final int NUMBER_OF_THREADS = 200;

    /**
     * Duration of load. Default is 1 min.
     */
    private static Duration loadDuration = Duration.ofMinutes(1); // 1 min;

    /**
     * Reads load duration in millis as parameter
     */
    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0] != null && !"".equals(args[0])) {
            loadDuration = Duration.ofMillis(Long.valueOf(args[0]));
        }
        generateLoad();
    }

    /**
     * Generates CPU load
     */
    private static void generateLoad() throws Exception {
        // do not use any executors here, simply fire all threads
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < NUMBER_OF_THREADS; i++) {
            Thread t = new Thread(new CpuLoadGenerator());
            threads.add(t);
            t.start();
        }

        // wait for all threads to finish
        for (Thread t : threads) {
            t.join();
        }
    }

    /**
     * Computes the nth digit of Pi number
     */
    public void run() {
        double act = 0;
        long counter = 1;
        long start = System.currentTimeMillis();
        while (true) {
            act = act + (4.0 / (counter));
            act = act - (4.0 / (counter + 2));
            counter = counter + 4;
            if (System.currentTimeMillis() - start > loadDuration.toMillis()) {
                break;
            }
        }
    }
}
