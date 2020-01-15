package org.jboss.eap.qe.microprofile.fault.tolerance.v20;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
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
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.collect.Range;

import io.restassured.RestAssured;

@RunWith(Arquillian.class)
public class FaultTolerance20AsyncPartialTest {

    @ArquillianResource
    protected URL baseApplicationUrl;

    @Deployment
    public static Archive<?> deployment() {
        String mpConfig = "hystrix.command.default.execution.isolation.thread.timeoutInMilliseconds=6000\n" +
                "hystrix.command.default.execution.isolation.semaphore.maxConcurrentRequests=20\n" +
                "hystrix.threadpool.default.maximumSize=40\n" +
                "hystrix.threadpool.default.allowMaximumSizeToDivergeFromCoreSize=true\n";
        return ShrinkWrap.create(WebArchive.class, FaultTolerance20AsyncPartialTest.class.getSimpleName() + ".war")
                .addPackages(true, AsyncHelloService.class.getPackage())
                .addClasses(TimeoutException.class, FaultToleranceException.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsManifestResource(new StringAsset(mpConfig), "microprofile-config.properties");
    }

    @BeforeClass
    public static void setup() throws Exception {
        MicroProfileFaultToleranceServerConfiguration.enableFaultTolerance();
    }

    /**
     * @tpTestDetails Test sends 40 parallel requests. There are annotations on service:
     *                timeout 1s and bulkhead (of maximum number of concurrent calls = 15 and 5 queued).
     *                Every even call is made to timeout. Calls above 20 fall-back.
     * @tpPassCrit Tests expects maximal success of 20 messages (those not timeout-ed + not fallback-ed).
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @RunAsClient
    public void bulkhead15q5Timeout() throws InterruptedException {
        Map<String, Range<Integer>> expectedResponses = new HashMap<>();
        expectedResponses.put("Hello from @Bulkhead @Timeout method", Range.closed(0, 20));
        expectedResponses.put("Fallback Hello", Range.closed(20, 40));
        testPartial(40, baseApplicationUrl + "partial?operation=bulkhead15q5-timeout&counter=", expectedResponses);
    }

    /**
     * @tpTestDetails Test sends 40 parallel requests. There are annotations on service:
     *                timeout 1s and bulkhead (of maximum number of concurrent calls = 5 and 5 queued).
     *                Every even call is made to timeout. Calls above 20 fall-back.
     * @tpPassCrit Tests expects maximal success of 20 messages (those not timeout-ed + not fallback-ed).
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @RunAsClient
    public void bulkhead5q5Timeout() throws InterruptedException {
        Map<String, Range<Integer>> expectedResponses = new HashMap<>();
        expectedResponses.put("Hello from @Bulkhead @Timeout method", Range.closed(0, 20));
        expectedResponses.put("Fallback Hello", Range.closed(20, 40));
        testPartial(40, baseApplicationUrl + "partial?operation=bulkhead5q5-timeout&counter=", expectedResponses);
    }

    /**
     * @tpTestDetails Test sends 20 parallel requests. There are annotations on service:
     *                timeout 1s, bulkhead (of maximum number of concurrent calls = 5 and 5 queued), retry
     *                5 requests invoke timeout exception immediately, 5 requests time-out. Calls above 10 fall-back.
     * @tpPassCrit Tests expects maximal success of 10 messages (those without exception, not timeout-ed + not fallback-ed).
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @RunAsClient
    public void bulkhead5q5TimeoutRetry() throws InterruptedException {
        Map<String, Range<Integer>> expectedResponses = new HashMap<>();
        expectedResponses.put("Hello from @Bulkhead @Timeout @Retry method", Range.closed(0, 10));
        expectedResponses.put("Fallback Hello", Range.closed(10, 20));
        testPartial(20, baseApplicationUrl + "partial?operation=bulkhead5q5-timeout-retry&counter=", expectedResponses);
    }

    /**
     * @tpTestDetails Test sends 20 parallel requests. There are annotations on service:
     *                timeout 1s, bulkhead (of maximum number of concurrent calls = 15 and 5 queued), retry
     *                5 requests invoke timeout exception immediately, 5 requests time-out.
     * @tpPassCrit Tests expects maximal success of 10 messages (those without exception, not timeout-ed).
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @RunAsClient
    public void bulkhead15q5TimeoutRetry() throws InterruptedException {
        Map<String, Range<Integer>> expectedResponses = new HashMap<>();
        expectedResponses.put("Hello from @Bulkhead @Timeout @Retry method", Range.closed(0, 10));
        expectedResponses.put("Fallback Hello", Range.closed(10, 20));
        testPartial(20, baseApplicationUrl + "partial?operation=bulkhead15q5-timeout-retry&counter=", expectedResponses);
    }

    private static void testPartial(int parallelRequests, String url, Map<String, Range<Integer>> expectedResponses)
            throws InterruptedException {
        Set<String> violations = Collections.newSetFromMap(new ConcurrentHashMap<>());
        Queue<String> seenResponses = new ConcurrentLinkedQueue<>();

        ExecutorService executor = Executors.newFixedThreadPool(parallelRequests);
        for (int i = 0; i < parallelRequests; i++) {
            final int finalI = i;
            executor.submit(() -> {
                try {
                    seenResponses.add(RestAssured.when().get(url + finalI).asString());
                } catch (Exception e) {
                    violations.add("Unexpected exception: " + e.getMessage());
                }
            });
        }
        executor.shutdown();
        boolean finished = executor.awaitTermination(15, TimeUnit.SECONDS);
        assertThat(finished, is(true));

        for (String seenResponse : seenResponses) {
            if (!expectedResponses.containsKey(seenResponse.replaceAll("[0-9]", ""))) {
                violations.add("Unexpected response: " + seenResponse);
            }
        }
        for (Map.Entry<String, Range<Integer>> expectedResponse : expectedResponses.entrySet()) {
            int count = 0;
            for (String seenResponse : seenResponses) {
                if (expectedResponse.getKey().equals(seenResponse.replaceAll("[0-9]", ""))) {
                    count++;
                }
            }
            if (!expectedResponse.getValue().contains(count)) {
                violations.add("Expected to see " + expectedResponse.getValue() + " occurrence(s) but seen " + count
                        + ": " + expectedResponse.getKey());
            }
        }
        assertThat(violations, is(empty()));
    }

    /**
     * @tpTestDetails Test sends 16 parallel requests. There are annotations on service:
     *                Retry(retryOn = IOException.class), CircuitBreaker(failOn = IOException.class,
     *                requestVolumeThreshold = 5, successThreshold = 3, delay = 2, delayUnit = ChronoUnit.SECONDS, failureRatio
     *                = 0.75)
     *                4 requests pass, 12 invoke IOException.
     * @tpPassCrit After that the circuit is open, after at most 3 seconds it is closed.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @RunAsClient
    public void retryCircuitBreaker() throws InterruptedException {
        Map<String, Range<Integer>> expectedResponses = new HashMap<>();
        expectedResponses.put("Hello from @Retry @CircuitBreaker method", Range.closed(0, 4));
        expectedResponses.put("Fallback Hello", Range.closed(12, 16));
        testPartial(16, baseApplicationUrl + "partial?operation=retry-circuitbreaker&counter=", expectedResponses);

        // ensure circuit is opened (note number 88 does request correct behavior!)
        String response2 = RestAssured.when().get(baseApplicationUrl + "partial?operation=retry-circuitbreaker&counter=88")
                .asString();
        assertThat(response2, is("Fallback Hello88"));
        // ensure circuit is closed
        await().atMost(3, TimeUnit.SECONDS).untilAsserted(() -> {
            String response = RestAssured.when()
                    .get(baseApplicationUrl + "partial?operation=retry-circuitbreaker&counter=88").asString();
            assertThat(response, is("Hello from @Retry @CircuitBreaker method88"));
        });
    }

    /**
     * @tpTestDetails Test sends 20 parallel requests. There are annotations on service:
     *                Retry(maxRetries = 2), CircuitBreaker(failOn = TimeoutException.class), Timeout
     *                5 requests pass, 10 invoke TimeoutException, 5 requests time-out.
     * @tpPassCrit After that the circuit is open, after at most 6 seconds it is closed.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    @RunAsClient
    public void retryCircuitBreakerTimeout() throws InterruptedException, IOException {
        Map<String, Range<Integer>> expectedResponses = new HashMap<>();
        expectedResponses.put("Hello from @Retry @CircuitBreaker @Timeout method", Range.closed(0, 5));
        expectedResponses.put("Fallback Hello", Range.closed(15, 20));
        testPartial(20, baseApplicationUrl + "partial?operation=retry-circuitbreaker-timeout&counter=", expectedResponses);

        // ensure circuit is opened (note number 99 does request correct behavior!)
        String response2 = RestAssured.when()
                .get(baseApplicationUrl + "partial?operation=retry-circuitbreaker-timeout&counter=99").asString();
        assertThat(response2, is("Fallback Hello99"));
        // ensure circuit is closed
        await().atMost(6000, TimeUnit.MILLISECONDS).untilAsserted(() -> {
            String response = RestAssured.when()
                    .get(baseApplicationUrl + "partial?operation=retry-circuitbreaker-timeout&counter=99").asString();
            assertThat(response, is("Hello from @Retry @CircuitBreaker @Timeout method99"));
        });
    }

    @AfterClass
    public static void tearDown() throws Exception {
        MicroProfileFaultToleranceServerConfiguration.disableFaultTolerance();
    }
}
