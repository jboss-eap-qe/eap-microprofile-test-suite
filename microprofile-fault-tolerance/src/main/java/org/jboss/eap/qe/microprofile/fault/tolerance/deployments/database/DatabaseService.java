package org.jboss.eap.qe.microprofile.fault.tolerance.deployments.database;

import java.io.*;
import java.sql.*;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;

/**
 * MicroProfile MP FT service used for testing MP FT annotations with database.
 */
public class DatabaseService {

    private static final String TEST_TABLE = "TEST_TABLE";

    private static AtomicInteger insertRecordCount = new AtomicInteger();

    public void dropTable(PrintWriter out) throws SQLException {
        try (Connection connection = getConnection()) {
            String deleteTableSql = "drop table " + TEST_TABLE;
            PreparedStatement deleteTable = connection.prepareStatement(deleteTableSql);
            deleteTable.execute();
            deleteTable.close();
            out.print("Table dropped.");
        }
    }

    public void createTable(PrintWriter out) throws SQLException {
        try (Connection connection = getConnection()) {
            String sql = "create table " + TEST_TABLE
                    + " (ID VARCHAR(50) primary key not null, NAME VARCHAR(50), ADDRESS VARCHAR(50))";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.executeUpdate();
            ps.close();
            out.print("Table created.");
        }
    }

    @Retry(maxRetries = -1, delay = 100)
    public void insertRecordWithRetry(PrintWriter out) throws SQLException {
        insertRecord(out);
    }

    /**
     * If 2 or more calls from last 10 calls fail then circuit is open.
     * If circuit is open and 1 call is successful (after 100 ms) delay then circuit is closed again.
     */
    @CircuitBreaker(requestVolumeThreshold = 10, failureRatio = 0.2, delay = 100, successThreshold = 10)
    public void insertRecordWithCircuitBreaker(PrintWriter out) throws SQLException {
        insertRecord(out);
    }

    public void insertRecord(PrintWriter out) throws SQLException {
        String id = String.valueOf(insertRecordCount.incrementAndGet());
        try (Connection connection = getConnection()) {
            PreparedStatement ps = connection.prepareStatement("INSERT INTO " + TEST_TABLE
                    + "(ID, NAME, ADDRESS) VALUES  (?, ?, ?)");
            ps.setString(1, id);
            ps.setString(2, "name" + id);
            ps.setString(3, "address" + id);
            ps.executeUpdate();
            ps.close();
            out.print("New record inserted.");
        }
    }

    public void getInsertRecordCount(PrintWriter out) {
        out.print(insertRecordCount.get());
    }

    private Connection getConnection() throws SQLException {
        Context context = null;
        DataSource dataSource;
        try {
            context = new InitialContext();
            dataSource = (DataSource) context.lookup("java:jboss/datasources/PostgresDS");
        } catch (NamingException ex) {
            throw new RuntimeException(ex); // there is now way to recover
        } finally {
            try {
                if (context != null)
                    context.close();
            } catch (NamingException ignore) {
                // ignore
            }
        }
        return dataSource.getConnection();
    }
}
