package org.jboss.eap.qe.microprofile.tooling.cpu.load.utils;

import java.lang.reflect.Field;

/**
 * Utility class for working with Linux process.
 */
public class LinuxProcessUtils implements ProcessUtils {

    /*
     * CPU masks as used by "taskset" command
     */
    private static final String MASK_FIRST_CORE = "0x00000001";
    private static final String MASK_ALL_CORES = "0xFFFFFFFF";

    public static final String LINUX_OS_NAME = "Linux";

    /**
     * Returns pid of given process.
     *
     * @param process process
     * @return pid of the process
     */
    @Override
    public int getProcessId(Process process) {

        int pid;
        try {
            Field f = process.getClass().getDeclaredField("pid");
            f.setAccessible(true);
            pid = f.getInt(process);
        } catch (Throwable e) {
            throw new IllegalStateException("Unable to get process id for " + process.getClass().getSimpleName(), e);
        }

        if (pid == 0) {
            throw new IllegalStateException("Process ID is 0 for " + process.getClass().getSimpleName());
        }

        return pid;
    }

    /**
     * Binds process to given CPU cores.
     *
     * @param pid process id
     * @param cpuCoreMask cpu cores to which process will be bound on
     *        see {@link org.jboss.eap.qe.microprofile.tooling.cpu.load.utils.ProcessUtils.CPUCoreMask}
     * @throws Exception throws exception if this operation fails
     */
    @Override
    public void bindProcessToCPU(int pid, CPUCoreMask cpuCoreMask) throws Exception {
        String cmd = "taskset -a -p " + getCPUMask(cpuCoreMask) + " " + pid;
        if (Runtime.getRuntime().exec(cmd).waitFor() != 0) {
            throw new Exception("Command: " + cmd + " failed.");
        }
    }

    /**
     * Returns CPU core mask used in taskset command
     *
     * @param cpuCoreMask cpu cores to which process will be bound on
     *        see {@link org.jboss.eap.qe.microprofile.tooling.cpu.load.utils.ProcessUtils.CPUCoreMask}
     * @return CPU core mask as used in taskset command
     */
    public String getCPUMask(CPUCoreMask cpuCoreMask) throws Exception {
        switch (cpuCoreMask) {
            case FIRST_CORE:
                return MASK_FIRST_CORE;
            case ALL_CORES:
                return MASK_ALL_CORES;
            default:
                throw new Exception("Such CPU mask does not exist: " + cpuCoreMask);
        }
    }

    /**
     * Kills process (kill -9 ...)
     *
     * @param process process to kill
     */
    @Override
    public void killProcess(Process process) throws Exception {
        String cmd = "kill -9 " + getProcessId(process);
        Runtime.getRuntime().exec(cmd).waitFor();
    }

}
