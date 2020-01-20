package org.jboss.eap.qe.microprofile.fault.tolerance.deployments.database;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet is used to work with MP FT service working with database.
 *
 * @author mnovak
 */
@WebServlet("/")
public class DatabaseServlet extends HttpServlet {

    @Inject
    DatabaseService databaseService;

    /**
     * @param request
     * @param response
     * @throws IOException
     * @see {@link HttpServlet#doGet(HttpServletRequest, HttpServletResponse)}
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        processRequest(request, response);
    }

    /**
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     * @see {@link HttpServlet#doPost(HttpServletRequest, HttpServletResponse)}
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    /**
     * Process requests
     *
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        PrintWriter out = response.getWriter();
        String op = request.getParameter("op");
        try {

            if (op != null) {
                if (op.equals("createTable")) {
                    databaseService.createTable(out);
                } else if (op.equals("dropTable")) {
                    databaseService.dropTable(out);
                } else if (op.equals("insertRecord")) {
                    databaseService.insertRecord(out);
                } else if (op.equals("insertRecordWithRetry")) {
                    databaseService.insertRecordWithRetry(out);
                } else if (op.equals("insertRecordWithCircuitBreaker")) {
                    databaseService.insertRecordWithCircuitBreaker(out);
                } else if (op.equals("getInsertRecordCount")) {
                    databaseService.getInsertRecordCount(out);
                } else {
                    response.getWriter().println("Operation: " + op + " is not supoported.");
                }
            }
        } catch (SQLException e) {
            out.println(e.getMessage());
            throw new ServletException(e);
        } catch (IOException e) {
            out.println(e.getMessage());
            throw e;
        } finally {
            response.getWriter().close();
        }
    }

}
