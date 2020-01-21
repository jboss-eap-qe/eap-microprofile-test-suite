package org.jboss.eap.qe.microprofile.fault.tolerance.deployments.nofaulttolerance;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
