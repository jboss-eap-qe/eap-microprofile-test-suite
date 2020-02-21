package org.jboss.eap.qe.microprofile.tooling.performance.memory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.fusesource.jansi.Ansi;
import org.jboss.eap.qe.microprofile.tooling.performance.core.Gauge;
import org.jboss.eap.qe.microprofile.tooling.performance.core.MeasurementException;
import org.junit.Assert;

/**
 * JMX based implementation of {@link Gauge}
 */
public class JMXBasedMemoryGauge implements Gauge<MemoryUsageRecord> {

    private final static int GC_CALLS = 2;
    private final static int POST_GC_GRACEFUL_WAIT_TIME_IN_MSEC = 1024 * (2);

    private final String address;
    private final int port;
    private final String url;
    private final JMXServiceURL jmxServiceURL;

    public JMXBasedMemoryGauge(String address, int port) throws MalformedURLException {
        this.address = address;
        this.port = port;
        this.url = String.format(
                "service:jmx:remote+http://%s:%d",
                address,
                port);
        jmxServiceURL = new JMXServiceURL(url);
    }

    private static void forceGC(MBeanServerConnection mbeanConn)
            throws MalformedObjectNameException, ReflectionException, MBeanException, InstanceNotFoundException, IOException {
        Object ret = mbeanConn.invoke(new ObjectName("java.lang:type=Memory"), "gc", null, null);
        Assert.assertNull(ret);
    }

    private static MemoryUsageRecord performOneMeasurement(MBeanServerConnection mbeanConn)
            throws MalformedObjectNameException, AttributeNotFoundException, MBeanException,
            ReflectionException, IOException, InstanceNotFoundException {

        Map<String, Long> results = new HashMap<>();
        CompositeDataSupport edenSpace;
        try {
            edenSpace = (CompositeDataSupport) mbeanConn
                    .getAttribute(new ObjectName("java.lang:type=MemoryPool,name=PS Eden Space"), "Usage");
        } catch (InstanceNotFoundException ex) {
            try {
                edenSpace = (CompositeDataSupport) mbeanConn
                        .getAttribute(new ObjectName("java.lang:type=MemoryPool,name=Eden Space"), "Usage");
            } catch (InstanceNotFoundException probablyJdk11) {
                edenSpace = (CompositeDataSupport) mbeanConn
                        .getAttribute(new ObjectName("java.lang:type=MemoryPool,name=G1 Eden Space"), "Usage");
            }
        }
        Long edenSpaceUsed = (Long) edenSpace.get("used");

        CompositeDataSupport oldGen;
        try {
            oldGen = (CompositeDataSupport) mbeanConn.getAttribute(new ObjectName("java.lang:type=MemoryPool,name=PS Old Gen"),
                    "Usage");
        } catch (InstanceNotFoundException ex) {
            try {
                oldGen = (CompositeDataSupport) mbeanConn
                        .getAttribute(new ObjectName("java.lang:type=MemoryPool,name=Tenured Gen"), "Usage");
            } catch (InstanceNotFoundException probablyJdk11) {
                oldGen = (CompositeDataSupport) mbeanConn
                        .getAttribute(new ObjectName("java.lang:type=MemoryPool,name=G1 Old Gen"), "Usage");
            }
        }
        Long oldGenUsed = (Long) oldGen.get("used");

        CompositeDataSupport survivorSpace;
        try {
            survivorSpace = (CompositeDataSupport) mbeanConn
                    .getAttribute(new ObjectName("java.lang:type=MemoryPool,name=PS Survivor Space"), "Usage");
        } catch (InstanceNotFoundException ex) {
            try {
                survivorSpace = (CompositeDataSupport) mbeanConn
                        .getAttribute(new ObjectName("java.lang:type=MemoryPool,name=Survivor Space"), "Usage");
            } catch (InstanceNotFoundException probablyJdk11) {
                survivorSpace = (CompositeDataSupport) mbeanConn
                        .getAttribute(new ObjectName("java.lang:type=MemoryPool,name=G1 Survivor Space"), "Usage");
            }
        }
        Long survivorSpaceUsed = (Long) survivorSpace.get("used");

        CompositeDataSupport metaSpace;
        try {
            metaSpace = (CompositeDataSupport) mbeanConn
                    .getAttribute(new ObjectName("java.lang:type=MemoryPool,name=PS Perm Gen"), "Usage");
        } catch (InstanceNotFoundException ex) {
            try {
                metaSpace = (CompositeDataSupport) mbeanConn
                        .getAttribute(new ObjectName("java.lang:type=MemoryPool,name=Perm Gen"), "Usage");
            } catch (InstanceNotFoundException probablyJava8) {
                // Java 8 doesn't have perm gen, it has meta space
                metaSpace = (CompositeDataSupport) mbeanConn
                        .getAttribute(new ObjectName("java.lang:type=MemoryPool,name=Metaspace"), "Usage");
            }
        }
        Long metaSpaceUsed = (Long) metaSpace.get("used");

        System.out.println(
                "***************************************** Measurement report **************************");
        System.out.println(Ansi.ansi().reset().a("[Gauge]: ").fgCyan()
                .a(String.format("Eden space:\t\t%s", edenSpaceUsed)).reset());
        System.out.println(Ansi.ansi().reset().a("[Gauge]: ").fgCyan()
                .a(String.format("Old gen space:\t\t%s", oldGenUsed)).reset());
        System.out.println(Ansi.ansi().reset().a("[Gauge]: ").fgCyan()
                .a(String.format("Survivor space:\t%s", survivorSpaceUsed)).reset());
        System.out.println(Ansi.ansi().reset().a("[Gauge]: ").fgCyan()
                .a(String.format("Metaspace:\t\t%s", metaSpaceUsed)).reset());

        MemoryUsageRecord record = new MemoryUsageRecord()
                .edenGenSpaceUsed(edenSpaceUsed)
                .oldGenSpaceUsed(oldGenUsed)
                .metaSpaceUsed(metaSpaceUsed)
                .survivorSpaceUsed(survivorSpaceUsed);

        System.out.println(
                "***************************************** Used heap: " + record.getHeapSpaceUsed()
                        + " *************************");
        System.out.println(
                "***************************************** Measured total: " + record.getTotalSpaceUsed()
                        + " *******************");
        return record;
    }

    /**
     * Measures current memory footprint for JVM server process.
     *
     * Performs a measurement using JMX MBeans.
     * Current implementation <b>must</b> consider https://issues.redhat.com/projects/REMJMX/issues/REMJMX-168
     * thus we must set up a connection each time...
     *
     * @return Instance of {@link MemoryUsageRecord} storing results for different JVM memory segments (old gen,
     *         metaspace etc.)
     * @throws MeasurementException Wrapper for exceptions thrown during memory measurement
     */
    public MemoryUsageRecord measure() throws MeasurementException {
        MemoryUsageRecord result = null;

        final JMXConnector jmxConnector;
        try {
            jmxConnector = JMXConnectorFactory.connect(jmxServiceURL, null);
            final MBeanServerConnection mBeanServerConnection = jmxConnector.getMBeanServerConnection();
            try {
                for (int i = 0; i < GC_CALLS; i++) {
                    forceGC(mBeanServerConnection);
                    Thread.sleep(POST_GC_GRACEFUL_WAIT_TIME_IN_MSEC);
                }
                forceGC(mBeanServerConnection);
                Thread.sleep(POST_GC_GRACEFUL_WAIT_TIME_IN_MSEC);
                result = performOneMeasurement(mBeanServerConnection);
            } finally {
                jmxConnector.close();
            }
            return result;
        } catch (IOException | InterruptedException | AttributeNotFoundException | InstanceNotFoundException | MBeanException
                | MalformedObjectNameException | ReflectionException e) {
            throw new MeasurementException(e);
        }
    }
}
