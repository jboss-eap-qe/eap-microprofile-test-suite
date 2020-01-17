package org.jboss.eap.qe.microprofile.fault.tolerance.deployments.load;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet calling {@link LoadService} with MP FT annotation to verify their correct work under high CPU load
 */
@WebServlet("/")
public class LoadServlet extends HttpServlet {

    @Inject
    private LoadService loadService;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        String operation = req.getParameter("operation");

        try {
            switch (operation) {
                case "timeout":
                    resp.getWriter().print(loadService.timeout());
                    break;
                case "retry":
                    resp.getWriter().print(loadService.retry());
                    break;
                case "timeoutWithFallback":
                    resp.getWriter().print(loadService.timeoutWithFallback());
            }
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}
