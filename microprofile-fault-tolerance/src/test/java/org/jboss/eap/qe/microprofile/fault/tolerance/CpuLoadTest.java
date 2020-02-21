package org.jboss.eap.qe.microprofile.fault.tolerance;

import static io.restassured.RestAssured.get;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import java.net.URL;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.eap.qe.microprofile.fault.tolerance.deployments.load.LoadService;
import org.jboss.eap.qe.microprofile.fault.tolerance.util.MicroProfileFaultToleranceServerConfiguration;
import org.jboss.eap.qe.microprofile.tooling.cpu.load.HighCPUUtils;
import org.jboss.eap.qe.microprofile.tooling.cpu.load.utils.ProcessUtils;
import org.jboss.eap.qe.microprofile.tooling.cpu.load.utils.ProcessUtilsProvider;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test class verifies correct behaviour of Fault Tolerance under high CPU load.
 */
@RunAsClient
@RunWith(Arquillian.class)
public class CpuLoadTest {

    // how long to keep CPU under 100% load
    private static final Duration DEFAULT_CPU_LOAD_DURATION = Duration.ofSeconds(10);

    @ArquillianResource
    protected URL baseApplicationUrl;

    // platform dependent utils for work with processes
    private static ProcessUtils processUtils = ProcessUtilsProvider.getProcessUtils();

    @Deployment(testable = false)
    public static Archive<?> createFirstWarDeployment() {
        String mpConfig = "Timeout/enabled=true";
        return ShrinkWrap.create(WebArchive.class, CpuLoadTest.class.getSimpleName() + ".war")
                .addPackage(LoadService.class.getPackage())
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsManifestResource(new StringAsset(mpConfig), "microprofile-config.properties");
    }

    @BeforeClass
    public static void serverSetup() throws Exception {
        Assume.assumeNotNull("This test cannot be executed on this platform as ProcessUtils class was not " +
                "implemented for it.", processUtils);
        MicroProfileFaultToleranceServerConfiguration.enableFaultTolerance();
    }

    /**
     * @tpTestDetails Deploy MP FT application and call service with @Timeout annotation. During execution
     *                of this method cause 10s CPU load.
     * @tpPassCrit Verify that method throws {@link org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException}
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testCpuLoadWithTimeout() throws Exception {
        // call service which takes 5 sec
        Future<String> underCpuLoadCall = Executors.newSingleThreadExecutor().submit(
                () -> get(baseApplicationUrl + "?operation=timeout").body().asString());
        causeCpuLoadOnServer(DEFAULT_CPU_LOAD_DURATION);
        assertThat(underCpuLoadCall.get(),
                containsString("org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException"));
    }

    /**
     * @tpTestDetails Deploy MP FT application and call service with @Retry annotation. During execution
     *                of this method cause 10s CPU load.
     * @tpPassCrit Verify that method throws Exception once max retry is reached.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testCpuLoadWithRetry() throws Exception { //
        Future<String> underCpuLoadCall = Executors.newSingleThreadExecutor().submit(
                () -> get(baseApplicationUrl + "?operation=retry").body().asString());
        causeCpuLoadOnServer(DEFAULT_CPU_LOAD_DURATION);
        assertThat(underCpuLoadCall.get(), containsString("Exception from @Retry method."));
    }

    /**
     * @tpTestDetails Deploy MP FT application and call service with @Fallback annotation. During execution
     *                of this method cause 10s CPU load.
     * @tpPassCrit Verify that @Fallback method was called.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testCpuLoadWithFallback() throws Exception {
        Future<String> underCpuLoadCall = Executors.newSingleThreadExecutor().submit(
                () -> get(baseApplicationUrl + "?operation=timeoutWithFallback").body().asString());
        causeCpuLoadOnServer(DEFAULT_CPU_LOAD_DURATION);
        assertThat(underCpuLoadCall.get(), containsString("Hello from @Fallback method"));
    }

    /**
     * Causes 100% CPU on load on server
     *
     * @param duration how long to generate 100% CPU load
     */
    private void causeCpuLoadOnServer(Duration duration) throws Exception {
        Process highCpuLoader = null;
        try {
            highCpuLoader = new HighCPUUtils(processUtils).causeMaximumCPULoadOnContainer(duration);
        } finally {
            if (highCpuLoader != null) {
                // wait for CPU load generator to finish
                // defensive programming - kill it if process survives load duration time with 5000 ms tolerance
                if (!highCpuLoader.waitFor(duration.toMillis() + 5000, TimeUnit.MILLISECONDS)) {
                    processUtils.killProcess(highCpuLoader);
                }
            }
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        MicroProfileFaultToleranceServerConfiguration.disableFaultTolerance();
    }
}
