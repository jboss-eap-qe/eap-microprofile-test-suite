package org.jboss.eap.qe.microprofile.fault.tolerance.deployments.v20;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.eap.qe.microprofile.fault.tolerance.deployments.v20.priority.InterceptorsContext;

/**
 * Servlet using {@link PriorityService} with MP FT annotations for testing that annotation are triggered in given order
 */
@WebServlet("/priority")
public class PriorityServlet extends HttpServlet {
    @Inject
    private PriorityService priorityService;

    @Inject
    private InterceptorsContext context;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String operation = req.getParameter("operation");
        boolean fail = Boolean.parseBoolean(req.getParameter("fail"));

        String response = null;
        context.getOrderQueue().add(this.getClass().getSimpleName());

        switch (operation) {
            case "retry":
                response = priorityService.retryFallback(fail);
                break;
        }
        resp.getWriter().print(response != null ? response : "response is null");
    }
}
