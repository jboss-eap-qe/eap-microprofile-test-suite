package org.jboss.eap.qe.microprofile.tooling.cpu.load.utils;

/**
 * Provides platform specific utility class for work with processes.
 */
public class ProcessUtilsProvider {
    /**
     * Returns platform dependent utility class for work with processes for current operating system.
     *
     * @return process utility class for work with processes for current operating system or null if such implementation
     *         does not exist
     */
    public static ProcessUtils getProcessUtils() {
        ProcessUtils processUtils = null;
        if (System.getProperty("os.name").contains(LinuxProcessUtils.LINUX_OS_NAME)) {
            processUtils = new LinuxProcessUtils();
        }
        return processUtils;
    }
}
