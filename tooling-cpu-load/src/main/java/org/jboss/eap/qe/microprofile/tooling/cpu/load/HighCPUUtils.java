package org.jboss.eap.qe.microprofile.tooling.cpu.load;

import static org.jboss.eap.qe.microprofile.tooling.cpu.load.utils.ProcessUtils.CPUCoreMask.ALL_CORES;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.jboss.eap.qe.microprofile.tooling.cpu.load.utils.CpuLoadGenerator;
import org.jboss.eap.qe.microprofile.tooling.cpu.load.utils.JavaProcessBuilder;
import org.jboss.eap.qe.microprofile.tooling.cpu.load.utils.ProcessUtils;

/**
 * Utility class causing 100% CPU load on CPU cores with Wildfly/EAP server.
 */
public class HighCPUUtils {

    private ProcessUtils processUtils;

    public HighCPUUtils(ProcessUtils processUtils) {
        this.processUtils = processUtils;
    }

    /**
     * Simulates that EAP/Wildfly server will be under 100% CPU load.
     * <p>
     * This method binds process with Wildfly/EAP to single CPU core (to 1st core) and then causes 100% load on this core so
     * Wildfly/EAP process has very little CPU time (almost not scheduled) on this core.
     *
     * @param durationOfLoad how long will be CPU with EAP/Wildfly server under 100% load
     * @return return process of CPU load generator
     * @throws Exception if causing CPU load on given process fails
     */
    public Process causeMaximumCPULoadOnContainer(Duration durationOfLoad)
            throws Exception {
        return causeMaximumCPULoadOnContainer(ProcessUtils.CPUCoreMask.FIRST_CORE, durationOfLoad);
    }

    /**
     * Simulates that EAP/Wildfly server will be under 100% CPU load.
     * <p>
     * This method binds process with Wildfly/EAP to given CPU cores and then causes 100% load on those cores so Wildfly/EAP
     * process has very little CPU time (almost not scheduled) on those cores.
     * <p>
     * WARNING: Remember that if all CPU cores will be specified then it might crash the test when under 100% CPU cores
     * thus it's recommended to consume just single core.
     *
     * @param cpuCoreMask see {@link ProcessUtils.CPUCoreMask} for possible values
     * @param durationOfLoad how long will be CPU with EAP/Wildfly server under 100% load
     * @return return process of CPU load generator
     * @throws Exception if causing CPU load on given process fails
     */
    protected Process causeMaximumCPULoadOnContainer(ProcessUtils.CPUCoreMask cpuCoreMask, Duration durationOfLoad)
            throws Exception {

        int containerProcessId = processUtils.getServerProcessId();
        // bind Wildfly/EAP process to given CPU cores
        processUtils.bindProcessToCPU(containerProcessId, cpuCoreMask);

        // start CPU load generator and bind to the same CPU cores
        Process cpuLoadProcess = generateLoadInSeparateProcess(durationOfLoad);
        int cpuLoadProcessId = processUtils.getProcessId(cpuLoadProcess);
        processUtils.bindProcessToCPU(cpuLoadProcessId, cpuCoreMask);

        // safety hook - kill cpu load generator when load duration timeout is exceeded
        new Thread(() -> {
            try {
                if (!cpuLoadProcess.waitFor(durationOfLoad.toMillis(), TimeUnit.MILLISECONDS)) {
                    processUtils.killProcess(cpuLoadProcess);
                }
            } catch (Exception e) {
                throw new RuntimeException(
                        "Failed to bind Wildfly/EAP server process back to all cpu cores. ", e);
            }
        }).start();

        // shutdown hook -  automatically returns Wildfly/EAP server to all cores after CPU load process has finished
        new Thread(() -> {
            try {
                cpuLoadProcess.waitFor();
                processUtils.bindProcessToCPU(containerProcessId, ALL_CORES);
            } catch (Exception e) {
                throw new RuntimeException(
                        "Failed to bind Wildfly/EAP server process back to all cpu cores. ", e);
            }
        }).start();

        return cpuLoadProcess;
    }

    private Process generateLoadInSeparateProcess(Duration durationOfLoad) throws Exception {
        return new JavaProcessBuilder()
                .addClasspathEntry(System.getProperty("java.class.path"))
                .addArgument(String.valueOf(durationOfLoad.toMillis()))
                .setMainClass(CpuLoadGenerator.class.getName())
                .startProcess();
    }
}
