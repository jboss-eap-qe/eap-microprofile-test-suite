package org.jboss.eap.qe.microprofile.fault.tolerance.deployments.v10;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet using {@link HelloService} for testing MP FT annotations
 */
@WebServlet("/")
public class HelloServlet extends HttpServlet {
    @Inject
    private MyContext context;

    @Inject
    private HelloService hello;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        String operation = req.getParameter("operation");
        boolean fail = Boolean.parseBoolean(req.getParameter("fail"));
        String context = req.getParameter("context");

        this.context.setValue(context);

        try {
            switch (operation) {
                case "timeout":
                    resp.getWriter().print(hello.timeout(fail));
                    break;
                case "retry":
                    resp.getWriter().print(hello.retry(fail));
                    break;
                case "infiniteRetry":
                    resp.getWriter().print(hello.infiniteRetry());
                    break;
                case "isInfiniteRetryInProgress":
                    resp.getWriter().print(hello.isInfiniteRetryInProgress());
                    break;
                case "circuit-breaker":
                    resp.getWriter().print(hello.circuitBreaker(fail));
                    break;
                case "bulkhead":
                    resp.getWriter().print(hello.bulkhead(fail));
                    break;
            }

        } catch (InterruptedException e) {
            throw new ServletException(e);
        }
    }
}
