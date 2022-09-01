package org.jboss.eap.qe.microprofile.fault.tolerance.deployments.v20;

import java.io.IOException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Servlet using {@link AsyncHelloService} with MP FT annotations for testing @Asynchronous calls
 */
@WebServlet("/async")
public class AsyncHelloServlet extends HttpServlet {
    @Inject
    private AsyncHelloService asyncHello;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        String operation = req.getParameter("operation");
        boolean fail = Boolean.parseBoolean(req.getParameter("fail"));

        CompletionStage<MyConnection> completionStage = null;
        try {
            switch (operation) {
                case "timeout":
                    completionStage = asyncHello.timeout(fail);
                    break;
                case "bulkhead-timeout-retry":
                    completionStage = asyncHello.bulkheadTimeoutRetry(fail);
                    break;
                case "retry-circuit-breaker":
                    completionStage = asyncHello.retryCircuitBreaker(fail);
                    break;
                case "retry-circuit-breaker-timeout":
                    completionStage = asyncHello.retryCircuitBreakerTimeout(fail);
                    break;
                case "retry-timeout":
                    completionStage = asyncHello.retryTimeout(fail);
                    break;
                case "timeout-circuit-breaker":
                    completionStage = asyncHello.timeoutCircuitBreaker(fail);
                    break;

            }
            resp.getWriter().print(completionStage != null ? completionStage.toCompletableFuture().get().getData()
                    : "completion stage is null");
        } catch (InterruptedException | ExecutionException e) {
            throw new ServletException(e);
        }
    }
}
