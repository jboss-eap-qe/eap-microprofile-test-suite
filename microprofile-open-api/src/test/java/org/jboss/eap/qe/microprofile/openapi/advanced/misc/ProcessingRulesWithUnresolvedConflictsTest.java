package org.jboss.eap.qe.microprofile.openapi.advanced.misc;

import static org.hamcrest.Matchers.containsString;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.eap.qe.microprofile.openapi.OpenApiServerConfiguration;
import org.jboss.eap.qe.microprofile.openapi.model.OpenApiModelReader;
import org.jboss.eap.qe.microprofile.openapi.util.MicroProfileOpenApiTestUtils;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.ConfigurationException;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.MicroProfileServerSetupTask;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.jboss.eap.qe.microprofile.tooling.server.log.ModelNodeLogChecker;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;

import io.restassured.response.ValidatableResponse;

/**
 * Extends {@link ProcessingRulesTestBase} to validate the server behavior in MicroProfile OpenAPI
 * generation processing rules with multiple deployments, and specifically when discordant
 * element values are not resolved.
 */
@RunWith(Arquillian.class)
@ServerSetup({ ProcessingRulesWithUnresolvedConflictsTest.OpenApiExtensionSetup.class })
@RunAsClient
public class ProcessingRulesWithUnresolvedConflictsTest extends ProcessingRulesTestBase {

    static class OpenApiExtensionSetup implements MicroProfileServerSetupTask {
        @Override
        public void setup() throws Exception {
            try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
                OpenApiServerConfiguration.enableOpenApi(client);
            }
        }

        @Override
        public void tearDown() throws Exception {
            try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
                OpenApiServerConfiguration.disableOpenApi(client);
            }
        }
    }

    /**
     * @tpTestDetails Verifies that the global {@code externalDocs} element exists in the generated
     *                OpenAPI documentation, as provided by the {@code openapi-with-external-docs.yaml} static file definition
     * @tpPassCrit The generated OpenAPI documentation {@code externalDocs} element exists in the generated
     *             OpenAPI documentation, and it contains the value defined by the {@code openapi-with-external-docs.yaml}
     *             static file.
     * @tpSince JBoss EAP XP 6
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testExternalDocumentationExistsInStaticallyGeneratedOpenApi()
            throws MalformedURLException, URISyntaxException {
        deployer.deploy(STATICALLY_GENERATED_ROUTER_DEPLOYMENT_NAME);
        try {
            final String element = "externalDocs";
            final ValidatableResponse openApiResponse = MicroProfileOpenApiTestUtils.getGeneratedOpenApi(
                    new URL("http://localhost:8080/openapi"))
                    .body(containsString(element + ":"));
            final String responseContent = openApiResponse.extract().asString();
            Map<String, Object> externalDocs = MicroProfileOpenApiTestUtils.getGeneratedRootElement(responseContent,
                    element);

            Assert.assertEquals("Unexpected number of \"externalDocs\" items", 2, externalDocs.size());
            String url = (String) externalDocs.get("url");
            String description = (String) externalDocs.get("description");

            Assert.assertEquals("The generated \"externalDoc.url\" value is not correct",
                    "https://static-file-based-external-docs.org",
                    url);
            Assert.assertEquals("The generated \"externalDoc.description\" value is not correct",
                    "Could be overridden by annotations",
                    description);
        } finally {
            deployer.undeploy(STATICALLY_GENERATED_ROUTER_DEPLOYMENT_NAME);
        }
    }

    /**
     * @tpTestDetails Verifies that the global {@code externalDocs} element that is created by {@link OpenApiModelReader}
     *                is overridden by the value in the {@code openapi-with-external-docs.yaml} static file definition
     * @tpPassCrit The generated OpenAPI documentation {@code externalDocs} element contains the value defined
     *             by the {@code openapi-with-external-docs.yaml} static file, which overrides the one set by
     *             {@link OpenApiModelReader}.
     * @tpSince JBoss EAP XP 6
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testExternalDocumentationIsOverriddenInProgrammaticallyModeledOpenApi()
            throws MalformedURLException, URISyntaxException {
        deployer.deploy(PROGRAMMATICALLY_MODELED_ROUTER_DEPLOYMENT_NAME);
        try {
            final String element = "externalDocs";
            final ValidatableResponse openApiResponse = MicroProfileOpenApiTestUtils.getGeneratedOpenApi(
                    new URL("http://localhost:8080/openapi"))
                    .body(containsString(element + ":"));
            final String responseContent = openApiResponse.extract().asString();
            Map<String, Object> externalDocs = MicroProfileOpenApiTestUtils.getGeneratedRootElement(responseContent,
                    element);

            Assert.assertEquals("Unexpected number of \"externalDocs\" items", 2, externalDocs.size());
            String url = (String) externalDocs.get("url");
            String description = (String) externalDocs.get("description");
            Assert.assertEquals("The generated \"externalDoc.url\" value is not correct",
                    "http://oas-filter-based-external-docs.org",
                    url);
            Assert.assertEquals("The generated \"externalDoc.description\" value is not correct",
                    "Could be overridden only by other filters in the chain",
                    description);
        } finally {
            deployer.undeploy(PROGRAMMATICALLY_MODELED_ROUTER_DEPLOYMENT_NAME);
        }
    }

    /**
     * @tpTestDetails Verify that the conflict between {@code externalDocs} discordant values configured by the two
     *                deployments results in the final OpenAPI documentation to contain an empty {@code externalDocs} element.
     * @tpPassCrit The {@code externalDocs} element is empty, and {@code WFLYMPOAI0009} is logged.
     * @tpSince JBoss EAP XP 6
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testExternalDocsElementGenerationWithMultipleDeployments()
            throws ConfigurationException, IOException, URISyntaxException {
        final String element = "externalDocs";
        deployer.deploy(STATICALLY_GENERATED_ROUTER_DEPLOYMENT_NAME);
        try {
            deployer.deploy(PROGRAMMATICALLY_MODELED_ROUTER_DEPLOYMENT_NAME);
            try {
                final ValidatableResponse openApiResponse = MicroProfileOpenApiTestUtils.getGeneratedOpenApi(
                        new URL("http://localhost:8080/openapi"))
                        .body(containsString(element + ":"));
                final String responseContent = openApiResponse.extract().asString();
                Map<String, Object> externalDocs = MicroProfileOpenApiTestUtils.getGeneratedRootElement(responseContent,
                        element);

                Assert.assertTrue("\"externalDocs\" should be empty as there are conflicting values", externalDocs.isEmpty());

                // let's also check that the WFLYMPOAI0009 waring is logged too.
                try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
                    ModelNodeLogChecker modelNodeLogChecker = new ModelNodeLogChecker(client, 100);
                    Assert.assertTrue(modelNodeLogChecker.logContains("WFLYMPOAI0009"));
                }
            } finally {
                deployer.undeploy(PROGRAMMATICALLY_MODELED_ROUTER_DEPLOYMENT_NAME);
                final ValidatableResponse openApiResponse = MicroProfileOpenApiTestUtils.getGeneratedOpenApi(
                        new URL("http://localhost:8080/openapi"))
                        .body(containsString(element + ":"));
                final String responseContent = openApiResponse.extract().asString();
                Map<String, Object> externalDocs = MicroProfileOpenApiTestUtils.getGeneratedRootElement(responseContent,
                        element);

                Assert.assertFalse("\"externalDocs\" should be no longer empty as there no conflicting values",
                        externalDocs.isEmpty());
                Assert.assertEquals("Unexpected number of \"externalDocs\" items", 2, externalDocs.size());

                String url = (String) externalDocs.get("url");
                String description = (String) externalDocs.get("description");
                Assert.assertEquals("The generated \"externalDoc.url\" value is not correct",
                        "https://static-file-based-external-docs.org",
                        url);
                Assert.assertEquals("The generated \"externalDoc.description\" value is not correct",
                        "Could be overridden by annotations",
                        description);
            }
        } finally {
            deployer.undeploy(STATICALLY_GENERATED_ROUTER_DEPLOYMENT_NAME);
        }
    }
}
