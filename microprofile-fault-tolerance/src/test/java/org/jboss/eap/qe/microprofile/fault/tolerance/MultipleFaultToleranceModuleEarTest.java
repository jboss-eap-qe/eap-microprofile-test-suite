package org.jboss.eap.qe.microprofile.fault.tolerance;

import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.containsString;

import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.eap.qe.microprofile.fault.tolerance.deployments.v10.HelloFallback;
import org.jboss.eap.qe.microprofile.fault.tolerance.deployments.v10.HelloService;
import org.jboss.eap.qe.microprofile.fault.tolerance.deployments.v10.HelloServlet;
import org.jboss.eap.qe.microprofile.fault.tolerance.deployments.v10.MyContext;
import org.jboss.eap.qe.microprofile.fault.tolerance.util.MicroProfileFaultToleranceServerConfiguration;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.ConfigurationException;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.ArquillianContainerProperties;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.ArquillianDescriptorWrapper;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests deploy/undeploy of MP FT applications in EAR archive
 * Note that this is test for multiple deployments which is currently unsupported feature in Wildfly/EAP.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class MultipleFaultToleranceModuleEarTest {

    private static final String DEPLOYMENT_EAR = "MultipleFaultToleranceModuleEarTest";
    private static final String FIRST_MODULE_NAME = "first-module";
    private static final String SECOND_MODULE_NAME = "second-module";

    @Deployment(name = DEPLOYMENT_EAR, testable = false)
    public static Archive<?> createDeploymentPackage() {
        final WebArchive firstModule = createModule(FIRST_MODULE_NAME, true);
        final WebArchive secondModule = createModule(SECOND_MODULE_NAME, false);

        String applicationXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<application xmlns=\"http://java.sun.com/xml/ns/javaee\"\n" +
                "             xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "             xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/application_6.xsd\"\n"
                +
                "             version=\"6\">\n" +
                "    <initialize-in-order>true</initialize-in-order>\n" +
                "    <module>\n" +
                "        <web>\n" +
                "            <web-uri>" + FIRST_MODULE_NAME + ".war</web-uri>\n" +
                "            <context-root>" + FIRST_MODULE_NAME + "</context-root>\n" +
                "        </web>\n" +
                "    </module>\n" +
                "    <module>\n" +
                "        <web>\n" +
                "            <web-uri>" + SECOND_MODULE_NAME + ".war</web-uri>\n" +
                "            <context-root>" + SECOND_MODULE_NAME + "</context-root>\n" +
                "        </web>\n" +
                "    </module>\n" +
                "</application>";

        return ShrinkWrap.create(EnterpriseArchive.class)
                .addAsApplicationResource(new StringAsset(applicationXml), "application.xml")
                .addAsModule(firstModule)
                .addAsModule(secondModule);
    }

    public static WebArchive createModule(String moduleName, boolean faultToleranceTimeoutEnabled) {
        String mpConfig = "Timeout/enabled=" + faultToleranceTimeoutEnabled;
        return ShrinkWrap.create(WebArchive.class, moduleName + ".war")
                .addClasses(HelloService.class, HelloServlet.class, HelloFallback.class, MyContext.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsManifestResource(new StringAsset(mpConfig), "microprofile-config.properties");
    }

    @BeforeClass
    public static void setup() throws Exception {
        MicroProfileFaultToleranceServerConfiguration.enableFaultTolerance();
    }

    /**
     * @tpTestDetails Deploy EAR with two MP FT modules. Both of them are the same (same classes/methods).
     *                MicroProfile specs are not currently supporting multiple deployments, which makes sense in most common
     *                microservices/cloud-native scenarios, so the behavior of such a configuration depends on the vendor
     *                implementation. This topic has also been discussed in a JIRA involving the MP Health subsystem, see
     *                https://issues.redhat.com/browse/WFLY-12835?focusedCommentId=14115173&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-14115173
     *                So WildFly still doesn't support the above mentioned configuration and the @Ignore clause references
     *                WFLY-12835
     *                accordingly.
     *                It could be enabled in the future depending on whether WildFly will support MP FT for sub-deployments.
     * @tpPassCrit Verify that MP FT Metrics are the same for both (as there are same classes/methods)
     *             and that they are summed.
     * @tpSince EAP 7.4.0.CD19
     */
    @Ignore("https://issues.redhat.com/browse/WFLY-12835")
    @Test
    public void testFaultToleranceMetricsAreSummedWithSameDeployments(
            @ArquillianResource @OperateOnDeployment(DEPLOYMENT_EAR) URL baseUrl) throws ConfigurationException {
        get(baseUrl + "/" + FIRST_MODULE_NAME + "/?operation=timeout&context=foobar&fail=true").then()
                .assertThat()
                .body(containsString("Fallback Hello, context = foobar"));

        get(baseUrl + "/" + SECOND_MODULE_NAME + "/?operation=timeout&context=foobar&fail=true").then()
                .assertThat()
                .body(containsString("Fallback Hello, context = foobar"));
        ArquillianContainerProperties arqProperties = new ArquillianContainerProperties(
                ArquillianDescriptorWrapper.getArquillianDescriptor());
        get("http://" + arqProperties.getDefaultManagementAddress() + ":" + arqProperties.getDefaultManagementPort()
                + "/metrics").then()
                        .assertThat()
                        .body(containsString(
                                "base_ft_timeout_calls_total{method=\"org.jboss.eap.qe.microprofile.fault.tolerance.deployments.v10.HelloService.timeout\",timedOut=\"true\"} 2.0"));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        MicroProfileFaultToleranceServerConfiguration.disableFaultTolerance();
    }
}
