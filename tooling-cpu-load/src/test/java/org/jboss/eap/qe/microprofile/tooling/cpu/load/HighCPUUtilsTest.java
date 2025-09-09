package org.jboss.eap.qe.microprofile.tooling.cpu.load;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.awaitility.Awaitility;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.eap.qe.microprofile.tooling.cpu.load.utils.ProcessUtils;
import org.jboss.eap.qe.microprofile.tooling.cpu.load.utils.ProcessUtilsProvider;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.ArquillianContainerProperties;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.ArquillianDescriptorWrapper;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests tooling for HighCPUUtils. Test CPU on the system is exhausted.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class HighCPUUtilsTest {

    @Test
    public void testCauseMaximumCPULoadOnProcess() throws Exception {
        ProcessUtils processUtils = ProcessUtilsProvider.getProcessUtils();
        Assume.assumeNotNull("This test cannot be executed on this platform as ProcessUtils class was not " +
                "implemented for it.", processUtils);

        int loadDurationInSeconds = 30;
        Process cpuLoadProcess = new HighCPUUtils(processUtils).causeMaximumCPULoadOnContainer(
                ProcessUtils.CPUCoreMask.ALL_CORES, Duration.ofSeconds(loadDurationInSeconds));

        ArquillianContainerProperties props = new ArquillianContainerProperties(
                ArquillianDescriptorWrapper.getArquillianDescriptor());
        Awaitility.await("CPU load generator does not work and is not causing any CPU load.")
                .atMost(30, TimeUnit.SECONDS)
                // max CPU load is 1.0, if for example 9 from 10 CPU cores are under 100% load than expected load is 0.9
                // substract 0.02 is defining test load tolerance for the test as load might be slighly smaller like 0.98
                .until(() -> getCpuLoad(getConnection(props.getDefaultManagementAddress(),
                        props.getDefaultManagementPort())) > 0.98);
        // if process does not finish in timely fashion then kill the process and fail the test
        if (!cpuLoadProcess.waitFor(loadDurationInSeconds + 5, TimeUnit.SECONDS)) {
            processUtils.killProcess(cpuLoadProcess);
            Assert.fail("CPU load generator process did not finish");
        }
    }

    /**
     * Returns system CPU load from Wildfly/EAP server. Value between 0.0 (no load) and 1.0 (maximum load, all cores under 100%
     * load)
     *
     * @param connection MBeanServerConnection connection to Wildfly/EAP server
     * @return value between 0.0 (no load) and 1.0 (maximum load, all cores under 100% load)
     */
    private Double getCpuLoad(MBeanServerConnection connection) throws Exception {
        return (Double) connection.getAttribute(new ObjectName("java.lang:type=OperatingSystem"),
                "SystemCpuLoad");
    }

    private MBeanServerConnection getConnection(String host, int port) throws Exception {
        String url = "service:jmx:remote+http://" + host + ":" + port;
        JMXServiceURL serviceURL = new JMXServiceURL(url);
        JMXConnector jmxConnector = JMXConnectorFactory.connect(serviceURL);
        return jmxConnector.getMBeanServerConnection();
    }
}
