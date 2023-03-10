package org.jboss.eap.qe.microprofile.fault.tolerance.deployments.nofaulttolerance;

import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Simple deployment without MP FT stuff. Used to test that MP FT subsystem does not get activated.
 */
@WebServlet("/")
public class HelloServlet extends HttpServlet {

    @Inject
    private HelloService hello;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String operation = req.getParameter("operation");
        switch (operation) {
            case "ping":
                resp.getWriter().print(hello.ping());
                break;
            default:
                resp.getWriter().println("Unsupported operation: " + operation);
        }
    }
}
