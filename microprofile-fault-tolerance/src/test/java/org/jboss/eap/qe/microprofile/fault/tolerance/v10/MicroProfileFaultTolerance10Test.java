package org.jboss.eap.qe.microprofile.fault.tolerance.v10;

import static io.restassured.RestAssured.get;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientHelper.executeCliCommand;

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
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.eap.qe.microprofile.fault.tolerance.deployments.v10.HelloService;
import org.jboss.eap.qe.microprofile.fault.tolerance.util.MicroProfileFaultToleranceServerConfiguration;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.deployment.ConfigurationUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;

@RunWith(Arquillian.class)
@RunAsClient
public class MicroProfileFaultTolerance10Test {

    @ArquillianResource
    protected URL baseApplicationUrl;

    @Deployment(testable = false)
    public static Archive<?> deployment() {
        return ShrinkWrap.create(WebArchive.class, MicroProfileFaultTolerance10Test.class.getSimpleName() + ".war")
                .addPackages(true, HelloService.class.getPackage())
                .addAsManifestResource(new StringAsset(ConfigurationUtil.BEANS_XML_FILE_CONTENT), "beans.xml");
    }

    @BeforeClass
    public static void setup() throws Exception {
        MicroProfileFaultToleranceServerConfiguration.enableFaultTolerance();
    }

    /**
     * @tpTestDetails Deploy MP FT application with 1 second @Timeout annotation on service method and call it.
     * @tpPassCrit Method returns expected output as finished in defined timeout
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void timeoutOk() {
        get(baseApplicationUrl + "?operation=timeout&context=foobar").then()
                .assertThat()
                .body(containsString("Hello from @Timeout method, context = foobar"));
    }

    /**
     * @tpTestDetails Deploy MP FT application with 1 second @Timeout annotation and @Fallback on service method and call it.
     * @tpPassCrit Fallback method was called as method did not finish in 1 second timeout
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void timeoutFailure() {
        get(baseApplicationUrl + "?operation=timeout&context=foobar&fail=true").then()
                .assertThat()
                .body(containsString("Fallback Hello, context = foobar"));
    }

    /**
     * @tpTestDetails Deploy MP FT application with 1 second @Timeout and @Asynchronous annotation on service method and call
     *                it.
     * @tpPassCrit Method returns expected output as finished in defined timeout
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void timeoutOkAsync() {
        get(baseApplicationUrl + "?operation=timeout").then()
                .assertThat()
                .body(containsString("Hello from @Timeout method"));
    }

    /**
     * @tpTestDetails Deploy MP FT application with 1 second @Timeout, @Asynchronous and @Fallback annotation on service method
     *                and call it.
     * @tpPassCrit Fallback method was called as method did not finish in 1 second timeout
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void timeoutFailureAsync() {
        get(baseApplicationUrl + "?operation=timeout&fail=true").then()
                .assertThat()
                .body(containsString("Fallback Hello"));
    }

    /**
     * @tpTestDetails Deploy MP FT application with @Retry annotation on service method and call it.
     * @tpPassCrit Method returns expected output as no exception was thrown
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void retryOk() {
        get(baseApplicationUrl + "?operation=retry&context=foobar").then()
                .assertThat()
                .body(containsString("Hello from @Retry method, context = foobar"));
    }

    /**
     * @tpTestDetails Deploy MP FT application with @Retry and @Fallback annotation on service method and call it.
     * @tpPassCrit Fallback method was called as method throw exception until max retries reached
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void retryFailure() {
        get(baseApplicationUrl + "?operation=retry&context=foobar&fail=true").then()
                .assertThat()
                .body(containsString("Fallback Hello, context = foobar"));
    }

    /**
     * @tpTestDetails Deploy MP FT application with @Retry(maxRetries = -1) and call it.
     * @tpPassCrit Verify that method is retrying for every exception
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void infiniteRetryFailure() throws Exception {

        // this call will end up in infinite retry, it will finish once system property isInfiniteRetryInProgress=true
        Future<String> infiniteRetryCall = Executors.newSingleThreadExecutor()
                .submit(() -> get(baseApplicationUrl + "?operation=infiniteRetry&context=foobar").body().asString());

        // wait for 10 sec until @Retry method was called at least once
        Awaitility.await("Infinite @Retry is not working. Check that microprofile fault tolerance is working.")
                .atMost(Duration.TEN_SECONDS).until(() -> get(baseApplicationUrl + "?operation=isInfiniteRetryInProgress")
                        .body().print().equals("true"));

        try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
            executeCliCommand(client,
                    "/system-property=" + HelloService.EXIT_INFINITE_RETRY_PROPERTY_NAME + ":add(value=true)");
        }

        Awaitility.await().atMost(Duration.FIVE_SECONDS)
                .until(() -> infiniteRetryCall.get().contains("Hello from infinite @Retry method, context"));

    }

    /**
     * @tpTestDetails Deploy MP FT application with @Retry and @Asynchronous annotation on service method and call it.
     * @tpPassCrit Method returns expected output as no exception was thrown
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void retryOkAsync() {
        get(baseApplicationUrl + "?operation=retry").then()
                .assertThat()
                .body(containsString("Hello from @Retry method"));
    }

    /**
     * @tpTestDetails Deploy MP FT application with @Retry and @Fallback annotation on service method and call it.
     * @tpPassCrit Fallback method was called as method throw exception until max retries reached
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void retryFailureAsync() {
        get(baseApplicationUrl + "?operation=retry&fail=true").then()
                .assertThat()
                .body(containsString("Fallback Hello"));
    }

    /**
     * @tpTestDetails Deploy MP FT application with @CircuitBreaker annotation on service method and call it.
     * @tpPassCrit Method returns expected output as no exception was thrown
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void circuitBreakerOk() {
        get(baseApplicationUrl + "?operation=circuit-breaker&context=foobar").then()
                .assertThat()
                .body(containsString("Hello from @CircuitBreaker method, context = foobar"));
    }

    /**
     * @tpTestDetails Deploy MP FT application with @CircuitBreaker and @Fallback annotation on service method and call it.
     *                As exception was thrown fallback medhod is invoked and returned. Circuit breaker is open. Call again until
     *                circuit breaker is closed.
     * @tpPassCrit Fallback method was called as @CircuitBreaker method has thrown exception. Once caused circuit breaker is
     *             closed
     *             again verify output from @CircuitBreaker method is returned.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void circuitBreakerFailure() {
        testCircuitBreakerFailure(baseApplicationUrl + "?operation=circuit-breaker&context=foobar",
                "Fallback Hello, context = foobar",
                "Hello from @CircuitBreaker method, context = foobar");
    }

    /**
     * @tpTestDetails Deploy MP FT application with @CircuitBreaker, @Asynchronous and @Fallback annotation on service method
     *                and call it.
     * @tpPassCrit Method returns expected output as no exception was thrown
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void circuitBreakerOkAsync() {
        get(baseApplicationUrl + "async?operation=circuit-breaker").then()
                .assertThat()
                .body(containsString("Hello from @CircuitBreaker method"));
    }

    /**
     * @tpTestDetails Deploy MP FT application with @CircuitBreaker, @Asynchronous and @Fallback annotation on service method
     *                and call it.
     *                As exception was thrown fallback medhod is invoked and returned. Circuit breaker is open. Call again until
     *                circuit breaker is closed.
     * @tpPassCrit Fallback method was called as @CircuitBreaker method has thrown exception. Once caused circuit breaker is
     *             closed
     *             again verify output from @CircuitBreaker method is returned.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void circuitBreakerFailureAsync() {
        testCircuitBreakerFailure(baseApplicationUrl + "async?operation=circuit-breaker",
                "Fallback Hello",
                "Hello from @CircuitBreaker method");
    }

    private static void testCircuitBreakerFailure(String url, String expectedFallbackResponse, String expectedOkResponse) {
        int initialRequestsCount = 20;

        for (int i = 0; i < initialRequestsCount; i++) {
            get(url + "&fail=true").then().assertThat()
                    .body(containsString(expectedFallbackResponse));
        }

        int failuresCount = 10;

        for (int i = 0; i < failuresCount; i++) {
            get(url).then().assertThat().body(containsString(expectedFallbackResponse));
        }

        // @CircuitBreaker.delay
        long circuitBreakerDelayMillis = 5000;
        long maxWaitTime = circuitBreakerDelayMillis + 1000;

        await().atMost(maxWaitTime, TimeUnit.MILLISECONDS).untilAsserted(() -> {
            get(url).then().assertThat().body(containsString(expectedOkResponse));
        });
    }

    /**
     * @tpTestDetails Deploy MP FT application with @BulkHead annotation on service method and call it.
     * @tpPassCrit Method returns expected output as no exception was thrown
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void bulkheadOk() throws InterruptedException {
        Map<String, Integer> expectedResponses = new HashMap<>();
        expectedResponses.put("Hello from @Bulkhead method, context = foobar", 10);

        // 10 allowed invocations
        // 11 invocations would already trigger fallback
        testBulkhead(10, baseApplicationUrl + "?operation=bulkhead&context=foobar", expectedResponses);
    }

    /**
     * @tpTestDetails Deploy MP FT application with @BulkHead and @Fallback annotation on service method. Bulkhead allows
     *                max 10 requests. In parallel call 30 requests
     * @tpPassCrit Verify 10 requests passed @BulkHead method and 20 went to @Fallback method
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void bulkheadFailure() throws InterruptedException {
        Map<String, Integer> expectedResponses = new HashMap<>();
        expectedResponses.put("Hello from @Bulkhead method, context = foobar", 10);
        expectedResponses.put("Fallback Hello, context = foobar", 20);

        // 30 = 10 allowed invocations + 20 not allowed invocations that lead to fallback
        // 31 invocations would already trigger fallback rejection
        testBulkhead(30, baseApplicationUrl + "?operation=bulkhead&context=foobar&fail=true", expectedResponses);
    }

    /**
     * @tpTestDetails Deploy MP FT application with @BulkHead and @Asynchronous annotation on service method and call it.
     * @tpPassCrit Method returns expected output as no exception was thrown
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void bulkheadOkAsync() throws InterruptedException {
        Map<String, Integer> expectedResponses = new HashMap<>();
        expectedResponses.put("Hello from @Bulkhead method", 20);

        // 20 = 10 allowed invocations + 10 queued invocations
        // 21 invocations would already trigger fallback
        testBulkhead(20, baseApplicationUrl + "async?operation=bulkhead", expectedResponses);
    }

    /**
     * @tpTestDetails Deploy MP FT application with @BulkHead, @Asynchronous and @Fallback annotation on service method.
     *                Bulkhead allows max 10 requests + 10 queued. In parallel call 40 requests.
     * @tpPassCrit Verify 20 requests passed @BulkHead method and 20 went to @Fallback method
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void bulkheadFailureAsync() throws InterruptedException {
        Map<String, Integer> expectedResponses = new HashMap<>();
        expectedResponses.put("Hello from @Bulkhead method", 20);
        expectedResponses.put("Fallback Hello", 20);

        // 40 = 10 allowed invocations + 10 queued invocations + 20 not allowed invocations that lead to fallback
        // 41 invocations would already trigger fallback rejection
        testBulkhead(40, baseApplicationUrl + "async?operation=bulkhead&fail=true", expectedResponses);
    }

    private static void testBulkhead(int parallelRequests, String url, Map<String, Integer> expectedResponses)
            throws InterruptedException {
        Set<String> violations = Collections.newSetFromMap(new ConcurrentHashMap<>());
        Queue<String> seenResponses = new ConcurrentLinkedQueue<>();

        ExecutorService executor = Executors.newFixedThreadPool(parallelRequests);
        for (int i = 0; i < parallelRequests; i++) {
            executor.submit(() -> {
                try {
                    String response = get(url).asString();
                    seenResponses.add(response);
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
                violations.add("Expected to see " + expectedResponse.getValue() + " occurence(s) but seen " + count
                        + ": " + expectedResponse.getKey());
            }
        }

        assertThat(violations, is(empty()));
    }

    @Before
    @After
    public void cleanUpInfiniteRetrySystemProperty() {
        try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
            executeCliCommand(client, "/system-property=" + HelloService.EXIT_INFINITE_RETRY_PROPERTY_NAME + ":remove()");
        } catch (Exception ignore) {
            // ignore if property not present
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        MicroProfileFaultToleranceServerConfiguration.disableFaultTolerance();
    }
}
