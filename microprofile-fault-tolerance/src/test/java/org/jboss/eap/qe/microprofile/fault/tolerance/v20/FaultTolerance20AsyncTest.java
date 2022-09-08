package org.jboss.eap.qe.microprofile.fault.tolerance.v20;

import static io.restassured.RestAssured.get;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceException;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.eap.qe.microprofile.fault.tolerance.deployments.v20.AsyncHelloService;
import org.jboss.eap.qe.microprofile.fault.tolerance.util.MicroProfileFaultToleranceServerConfiguration;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.deployment.ConfigurationUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class FaultTolerance20AsyncTest {

    @ArquillianResource
    protected URL baseApplicationUrl;

    @Deployment
    public static Archive<?> deployment() {
        return ShrinkWrap.create(WebArchive.class, FaultTolerance20AsyncTest.class.getSimpleName() + ".war")
                .addClasses(TimeoutException.class, FaultToleranceException.class)
                .addPackages(true, AsyncHelloService.class.getPackage())
                .addAsManifestResource(new StringAsset(ConfigurationUtil.BEANS_XML_FILE_CONTENT), "beans.xml");
    }

    @BeforeClass
    public static void setup() throws Exception {
        MicroProfileFaultToleranceServerConfiguration.enableFaultTolerance();
    }

    /**
     * @tpTestDetails Deploy MP FT application with 1 second @Timeout and @Asynchronous annotation on service method and call
     *                it. There is no fail.
     * @tpPassCrit Method returns expected output as finished in defined timeout.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @RunAsClient
    public void timeoutOkCompletionStage() {
        get(baseApplicationUrl + "async?operation=timeout").then().assertThat()
                .body(containsString("Hello from @Timeout method"));
    }

    /**
     * @tpTestDetails Deploy MP FT application with 1 second @Timeout and @Asynchronous annotation on service method and call
     *                it. Call takes longer than defined timeout.
     * @tpPassCrit Fallback method is called as not finished in defined timeout
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @RunAsClient
    public void timeoutFailureCompletionStage() {
        get(baseApplicationUrl + "async?operation=timeout&fail=true").then().assertThat()
                .body(containsString("Fallback Hello"));
    }

    /**
     * @tpTestDetails Deploy MP FT application with 1 second @Timeout, @BulkHead
     *                and @Asynchronous and @Retry annotations on service method and call it. Call takes less than defined
     *                timeout.
     * @tpPassCrit Invocation succeeds as fail was present
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @RunAsClient
    public void bulkheadTimeoutRetryOK() {
        get(baseApplicationUrl + "async?operation=bulkhead-timeout-retry").then().assertThat()
                .body(containsString("Hello from @Bulkhead @Timeout @Retry method"));
    }

    /**
     * @tpTestDetails Deploy MP FT application with 1 second @Timeout, @Retry, @BulkHead
     *                and @Asynchronous annotation on service method and call 40 times. Call takes longer than defined timeout.
     * @tpPassCrit All 40 invocation ends in Fallback method as timeout and retires were exceeded
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @RunAsClient
    public void bulkheadTimeoutRetryFailure() throws InterruptedException {
        Map<String, Integer> expectedResponses = new HashMap<>();
        expectedResponses.put("Hello from @Bulkhead @Timeout @Retry method", 0);
        expectedResponses.put("Fallback Hello", 40);

        testBulkhead(40, baseApplicationUrl + "async?operation=bulkhead-timeout-retry&fail=true", expectedResponses);
    }

    private static void testBulkhead(int parallelRequests, String url, Map<String, Integer> expectedResponses)
            throws InterruptedException {
        Set<String> violations = Collections.newSetFromMap(new ConcurrentHashMap<>());
        Queue<String> seenResponses = new ConcurrentLinkedQueue<>();

        ExecutorService executor = Executors.newFixedThreadPool(parallelRequests);
        for (int i = 0; i < parallelRequests; i++) {
            executor.submit(() -> {
                try {
                    seenResponses.add(get(url).asString());
                } catch (Exception e) {
                    violations.add("Unexpected exception: " + e.getMessage());
                }
            });
        }
        executor.shutdown();
        boolean finished = executor.awaitTermination(10, TimeUnit.SECONDS);
        assertThat(finished, is(true));

        for (String seenResponse : seenResponses) {
            if (!expectedResponses.containsKey(seenResponse)) {
                violations.add("Unexpected response: " + seenResponse);
            }
        }
        for (Map.Entry<String, Integer> expectedResponse : expectedResponses.entrySet()) {
            int count = 0;
            for (String seenResponse : seenResponses) {
                if (expectedResponse.getKey().equals(seenResponse)) {
                    count++;
                }
            }
            if (count != expectedResponse.getValue()) {
                violations.add("Expected to see " + expectedResponse.getValue() + " occurrence(s) but seen " + count
                        + ": " + expectedResponse.getKey());
            }
        }
        assertThat(violations, is(empty()));

    }

    /**
     * @tpTestDetails Deploy MP FT application with @Retry, @CircuitBreaker and @Fallback annotation on service method.
     *                Call 20x fail URL, circuit is OPEN. Call 10x correct URL on opened circuit -> still returns fallback
     *                response.
     *                The window of 20 calls now contains 10 fail and 10 correct responses, this equals 0.5
     *                failureRatio. @CircuitBreaker delay
     *                is 5 seconds default.
     * @tpPassCrit Call after 5s and check circuit is CLOSED.OK response is returned
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @RunAsClient
    public void retryCircuitBreakerFailure() throws IOException, InterruptedException {
        testCircuitBreakerFailure(baseApplicationUrl + "async?operation=retry-circuit-breaker",
                "Fallback Hello",
                "Hello from @Retry @CircuitBreaker method");
    }

    /**
     * @tpTestDetails Deploy MP FT application with @Timeout, @CircuitBreaker and @Fallback annotation on service method.
     * @tpPassCrit Call and check circuit is CLOSED. OK response is returned
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @RunAsClient
    public void retryCircuitBreakerTimeoutOK() {
        get(baseApplicationUrl + "async?operation=retry-circuit-breaker-timeout").then().assertThat()
                .body(containsString("Hello from @Retry @CircuitBreaker @Timeout method"));
    }

    /**
     * @tpTestDetails Deploy MP FT application with @Timeout, @CircuitBreaker and @Fallback annotation on service method.
     *                Call 20x fail URL, circuit is OPEN. Call 10x correct URL on opened circuit -> still returns fallback
     *                response.
     *                The window of 20 calls now contains 10 fail and 10 correct responses, this equals 0.5
     *                failureRatio. @CircuitBreaker delay
     *                is 5 seconds default.
     * @tpPassCrit Call after 5s and check circuit is CLOSED.OK response is returned
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @RunAsClient
    public void retryCircuitBreakerTimeoutFailure() throws IOException, InterruptedException {
        testCircuitBreakerFailure(baseApplicationUrl + "async?operation=retry-circuit-breaker-timeout",
                "Fallback Hello",
                "Hello from @Retry @CircuitBreaker @Timeout method");
    }

    private static void testCircuitBreakerFailure(String url, String expectedFallbackResponse, String expectedOkResponse)
            throws IOException, InterruptedException {
        // call 20x fail URL, circuit is OPEN
        for (int i = 0; i < 20; i++) {
            get(url + "&fail=true").then().assertThat().body(containsString(expectedFallbackResponse));
        }
        // call 10x correct URL on opened circuit -> still returns fallback response
        for (int i = 0; i < 10; i++) {
            get(url).then().assertThat().body(containsString(expectedFallbackResponse));
        }
        // the window of 20 calls now contains 10 fail and 10 correct responses, this equals 0.5 failureRatio
        // @CircuitBreaker.delay is 5 seconds default, then circuit is CLOSED and OK response is returned
        Thread.sleep(5000L);
        await().atMost(1, TimeUnit.SECONDS).untilAsserted(() -> {
            get(url).then().assertThat().body(containsString(expectedOkResponse));
        });
    }

    /**
     * @tpTestDetails Deploy MP FT application with @Timeout, @Retry and @Fallback annotation on service method. Call
     *                takes longer than 1 sec.
     * @tpPassCrit Check that Fallback method is called.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @RunAsClient
    public void retryTimeout() {
        get(baseApplicationUrl + "async?operation=retry-timeout&fail=true").then().assertThat()
                .body(containsString("Fallback Hello"));
    }

    /**
     * @tpTestDetails Deploy MP FT application with @Timeout, @CircuitBreaker and @Fallback annotation on service method. Call
     *                takes less than 1 sec.
     * @tpPassCrit Check that OK response is returned as there is no failure
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @RunAsClient
    public void timeoutCircuitBreakerOK() {
        get(baseApplicationUrl + "async?operation=timeout-circuit-breaker").then().assertThat()
                .body(containsString("Hello from @Timeout @CircuitBreaker method"));
    }

    /**
     * @tpTestDetails Deploy MP FT application with @Timeout, @CircuitBreaker and @Fallback annotation on service method.
     *                Call 20x fail URL (call takes longer then 1 second), circuit is OPEN. Call 10x correct URL on opened
     *                circuit -> still returns fallback response.
     *                The window of 20 calls now contains 10 fail and 10 correct responses, this equals 0.5
     *                failureRatio. @CircuitBreaker delay
     *                is 5 seconds default.
     * @tpPassCrit Call after 5s and check circuit is CLOSED.OK response is returned
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @RunAsClient
    public void timeoutCircuitBreakerFailure() throws IOException, InterruptedException {
        testCircuitBreakerFailure(baseApplicationUrl + "async?operation=timeout-circuit-breaker",
                "Fallback Hello",
                "Hello from @Timeout @CircuitBreaker method");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        MicroProfileFaultToleranceServerConfiguration.disableFaultTolerance();
    }
}
