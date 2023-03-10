package org.jboss.eap.qe.microprofile.fault.tolerance.deployments.v10;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

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

        try {
            switch (operation) {
                case "timeout":
                    resp.getWriter().print(asyncHello.timeout(fail).get());
                    break;
                case "retry":
                    resp.getWriter().print(asyncHello.retry(fail).get());
                    break;
                case "circuit-breaker":
                    resp.getWriter().print(asyncHello.circuitBreaker(fail).get());
                    break;
                case "bulkhead":
                    resp.getWriter().print(asyncHello.bulkhead(fail).get());
                    break;
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new ServletException(e);
        }
    }
}
