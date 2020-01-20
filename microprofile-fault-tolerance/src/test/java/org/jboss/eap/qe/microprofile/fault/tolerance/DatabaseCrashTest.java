package org.jboss.eap.qe.microprofile.fault.tolerance;

import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.containsString;

import java.io.File;
import java.net.URL;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.eap.qe.microprofile.fault.tolerance.deployments.database.DatabaseService;
import org.jboss.eap.qe.microprofile.fault.tolerance.deployments.database.DatabaseServlet;
import org.jboss.eap.qe.microprofile.fault.tolerance.util.MicroProfileFaultToleranceServerConfiguration;
import org.jboss.eap.qe.ts.common.docker.Docker;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.io.Files;
import com.sun.security.auth.module.UnixSystem;

/**
 * Test MP Fault tolerance service with crashing database.
 */
@RunAsClient
@RunWith(Arquillian.class)
public class DatabaseCrashTest {

    private static final String APPLICATION_NAME = "DatabaseServlet";

    private static Docker postgresDB = null;
    private static File postgresDataDir = null;

    @ArquillianResource
    private URL baseUrl;

    @Deployment(testable = false)
    public static WebArchive createDatabaseService() {
        final WebArchive databaseService = ShrinkWrap.create(WebArchive.class, APPLICATION_NAME + ".war");
        databaseService.addClasses(DatabaseService.class, DatabaseServlet.class);
        databaseService.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");

        String postgresDS = "<datasources xmlns=\"http://www.jboss.org/ironjacamar/schema\">\n" +
                "    <datasource jndi-name=\"java:jboss/datasources/PostgresDS\" pool-name=\"exampleDS\">\n" +
                "        <driver>DatabaseServlet.war_org.postgresql.Driver_42_2</driver>\n" +
                "        <connection-url>jdbc:postgresql://127.0.0.1:5432/test-database</connection-url>\n" +
                "        <security>\n" +
                "            <user-name>postgres</user-name>\n" +
                "            <password>pass</password>\n" +
                "        </security>\n" +
                "           <validation>\n" +
                "                <valid-connection-checker class-name=\"org.jboss.jca.adapters.jdbc.extensions.postgres.PostgreSQLValidConnectionChecker\">\n"
                +
                "                </valid-connection-checker>\n" +
                "                <exception-sorter class-name=\"org.jboss.jca.adapters.jdbc.extensions.postgres.PostgreSQLExceptionSorter\">\n"
                +
                "                </exception-sorter>\n" +
                "</validation>\n" +
                "    </datasource>\n" +
                "</datasources>";
        databaseService.addAsWebInfResource(new StringAsset(postgresDS), "postgres-ds.xml");

        // Add postgresql JDBC driver
        JavaArchive[] postgresqlDriver = Maven.resolver().loadPomFromFile("pom.xml")
                .resolve("org.postgresql:postgresql").withoutTransitivity().as(JavaArchive.class);
        databaseService.addAsLibraries(postgresqlDriver);
        return databaseService;
    }

    @BeforeClass
    public static void startDatabase() throws Exception {
        // create data dir for postgres database
        postgresDataDir = Files.createTempDir();
        postgresDB = new Docker.Builder("postgres", "postgres:12")
                .setContainerReadyCondition(() -> {
                    // checking port 5432 is not an option as PostgreSQL opens it before it's ready when started for the 1st time
                    // thus try to create JDBC connection directly
                    Properties props = new Properties();
                    props.setProperty("user", "postgres");
                    props.setProperty("password", "pass");
                    try {
                        DriverManager.getConnection("jdbc:postgresql://127.0.0.1:5432/test-database", props).close();
                        return true;
                    } catch (SQLException ex) {
                        return false;
                    }
                })
                .setContainerReadyTimeout(3, TimeUnit.MINUTES)
                .withPortMapping("5432:5432")
                .withEnvVar("POSTGRES_DB", "test-database") // creates "test-database" database after 1st start
                .withEnvVar("POSTGRES_PASSWORD", "pass")
                .withCmdOption("-v")
                .withCmdOption(postgresDataDir.getAbsolutePath() + ":/var/lib/postgresql/data")
                .withCmdOption("-u")
                .withCmdOption(String.valueOf(new UnixSystem().getUid()))
                .build();
        postgresDB.start();
    }

    @BeforeClass
    public static void setup() throws Exception {
        MicroProfileFaultToleranceServerConfiguration.enableFaultTolerance();
    }

    @Before
    public void createDatabaseSchema() {
        get(baseUrl + "?op=createTable").then().assertThat().body(containsString("Table created."));
    }

    /**
     * @tpTestDetails Deploy application with @Retry annotation on method and kill DB in the moment when executing DB operation.
     *                Verify retry happens and once DB is restarted client gets successful response that operation was completed
     *                after retry.
     * @tpPassCrit Response that operation was completed successfully after retry.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testRetryWithDBKill() throws Exception {
        // call servlet to use DB, check that everything works
        get(baseUrl + "?op=insertRecordWithRetry").then().assertThat()
                .body(containsString("New record inserted."));

        postgresDB.kill();

        // call servlet to use DB, it will @Retry
        Future<String> retryCall = Executors.newSingleThreadExecutor()
                .submit(() -> get(baseUrl + "?op=insertRecordWithRetry").body().asString());

        // wait for 10 sec until @Retry method was called at least once
        Awaitility.await("@Retry is not working. Check that microprofile fault tolerance is working.")
                .atMost(Duration.TEN_SECONDS)
                .until(() -> Integer.valueOf(get(baseUrl + "?op=getInsertRecordCount")
                        .body().print()) > 3);

        postgresDB.start();

        // get returned correctly
        Awaitility.await().atMost(Duration.TEN_SECONDS)
                .until(() -> {
                    String response = retryCall.get();
                    return response.contains("New record inserted.");
                });
    }

    /**
     * @tpTestDetails Use @CircuitBreaker annotation and kill DB in the moment when micro service executing DB operation.
     *                Verify circuit breaker gets open, client gets exception. Restart database and verify circuit breaker is
     *                closed again.
     * @tpPassCrit Response that operation was completed successfully circuit breaker is closed.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void testCircuitBreakerWithDBKill() throws Exception {
        // check insert works
        get(baseUrl + "?op=insertRecord").then().assertThat()
                .body(containsString("New record inserted."));

        // kill DB
        postgresDB.kill();

        // call servlet a few times to open circuit - 2 of 10 fail will cause circuit will open
        for (int i = 0; i < 10; i++) {
            get(baseUrl + "?op=insertRecordWithCircuitBreaker");
        }

        // start DB again
        postgresDB.start();

        // circuit will close after 10 succesful calls
        for (int i = 0; i < 10; i++) {
            get(baseUrl + "?op=insertRecord").then().assertThat()
                    .body(containsString("New record inserted."));
        }

        // one more call to check circuit is closed 
        get(baseUrl + "?op=insertRecord").then().assertThat()
                .body(containsString("New record inserted."));
    }

    @After
    public void dropDatabaseSchema() {
        get(baseUrl + "?op=dropTable").then().assertThat().body(containsString("Table dropped."));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // stop DB if still running and delete data directory
        postgresDB.stop();
        postgresDataDir.delete();
        MicroProfileFaultToleranceServerConfiguration.disableFaultTolerance();
    }
}
