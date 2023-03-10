package org.jboss.eap.qe.microprofile.fault.tolerance.v20;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import jakarta.inject.Inject;

import java.net.URL;

import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceException;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.eap.qe.microprofile.fault.tolerance.deployments.v20.AsyncHelloService;
import org.jboss.eap.qe.microprofile.fault.tolerance.deployments.v20.PriorityServlet;
import org.jboss.eap.qe.microprofile.fault.tolerance.deployments.v20.priority.AfterInterceptor;
import org.jboss.eap.qe.microprofile.fault.tolerance.deployments.v20.priority.BeforeInterceptor;
import org.jboss.eap.qe.microprofile.fault.tolerance.deployments.v20.priority.InterceptorsContext;
import org.jboss.eap.qe.microprofile.fault.tolerance.util.MicroProfileFaultToleranceServerConfiguration;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.deployment.ConfigurationUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.restassured.RestAssured;

@RunWith(Arquillian.class)
public class PriorityOrderTest {

    @ArquillianResource
    protected URL baseApplicationUrl;

    @Deployment
    public static Archive<?> deployment() {
        String mpConfig = "mp.fault.tolerance.interceptor.priority=5000";
        return ShrinkWrap.create(WebArchive.class, PriorityOrderTest.class.getSimpleName() + ".war")
                .addPackages(true, AsyncHelloService.class.getPackage())
                .addClasses(TimeoutException.class, FaultToleranceException.class)
                .addAsManifestResource(ConfigurationUtil.BEANS_XML_FILE_LOCATION, "beans.xml")
                .addAsManifestResource(new StringAsset(mpConfig), "microprofile-config.properties");
    }

    @Inject
    private InterceptorsContext context;

    @BeforeClass
    public static void setup() throws Exception {
        MicroProfileFaultToleranceServerConfiguration.enableFaultTolerance();
    }

    /**
     * @tpTestDetails Tests priority order according to annotation: BeforeInterceptor with priority 4000 is before
     *                AfterInterceptor with priority 6000
     * @tpPassCrit Check interceptors were called in correct order (so BeforeInterceptor is before AfterInterceptor)
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @RunAsClient
    public void basicPriority() {
        String response = RestAssured.when().get(baseApplicationUrl + "priority?operation=retry&fail=false").asString();
        assertThat(response, equalTo("Hello from method: [" + PriorityServlet.class.getSimpleName() +
                ", " + BeforeInterceptor.class.getSimpleName() +
                ", " + AfterInterceptor.class.getSimpleName() +
                ", Inside method]"));
    }

    /**
     * @tpTestDetails Tests @Retry with priority loaded from microprofile-config.properties:
     *                mp.fault.tolerance.interceptor.priority=5000
     * @tpPassCrit Expected order contains second AfterInterceptor (priority 6000 > 5000) but only one BeforeInterceptor
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @RunAsClient
    public void priorityWithRetry() {
        String response = RestAssured.when().get(baseApplicationUrl + "priority?operation=retry&fail=true").asString();
        assertThat(response, is("Fallback Hello: [" + PriorityServlet.class.getSimpleName() +
                ", " + BeforeInterceptor.class.getSimpleName() +
                ", " + AfterInterceptor.class.getSimpleName() +
                ", Inside method, " +
                AfterInterceptor.class.getSimpleName() +
                ", Inside method, processFallback]"));
    }

    @After
    public void clearQueue() {
        if (context != null) {
            context.getOrderQueue().clear();
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        MicroProfileFaultToleranceServerConfiguration.disableFaultTolerance();
    }
}
