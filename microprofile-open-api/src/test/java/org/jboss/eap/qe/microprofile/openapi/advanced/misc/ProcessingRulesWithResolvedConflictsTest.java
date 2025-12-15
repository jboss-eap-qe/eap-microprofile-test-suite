package org.jboss.eap.qe.microprofile.openapi.advanced.misc;

import static org.hamcrest.Matchers.containsString;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.eap.qe.microprofile.openapi.OpenApiServerConfiguration;
import org.jboss.eap.qe.microprofile.openapi.util.MicroProfileOpenApiTestUtils;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.arquillian.MicroProfileServerSetupTask;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;

import io.restassured.response.ValidatableResponse;

/**
 * Extends {@link ProcessingRulesTestBase} to validate processing rules which should be enforced when generating OpenAPI
 * documentation, and employs MP Config properties at subsystem level to resolve conflicts
 */
@RunWith(Arquillian.class)
@ServerSetup({ ProcessingRulesWithResolvedConflictsTest.OpenApiExtensionSetup.class })
@RunAsClient
public class ProcessingRulesWithResolvedConflictsTest extends ProcessingRulesTestBase {

    static class OpenApiExtensionSetup implements MicroProfileServerSetupTask {
        @Override
        public void setup() throws Exception {
            try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
                OpenApiServerConfiguration.enableOpenApi(client);
                // configure a MicroProfile config property that will let the OpenAPI generation process resolve conflicts
                client.execute(
                        "/subsystem=microprofile-config-smallrye/config-source=props:add(properties={" +
                                "\"mp.openapi.extensions.server.default-server.host.default-host.externalDocs.url\" = \"http://system-property-based-local-service-router-external-docs.org\","
                                +
                                "\"mp.openapi.extensions.server.default-server.host.default-host.externalDocs.description\" = \"System property based Local Service Router external documentation\""
                                +
                                "})");
                new Administration(client).reload();
            }
        }

        @Override
        public void tearDown() throws Exception {
            try (OnlineManagementClient client = ManagementClientProvider.onlineStandalone()) {
                OpenApiServerConfiguration.disableOpenApi(client);
                // remove the MicroProfile config property that will resolve discordant values
                client.execute("/subsystem=microprofile-config-smallrye/config-source=props:remove");
            }
        }
    }

    /**
     * @tpTestDetails Verify that the conflict between {@code externalDocs} discordant values configured by the two
     *                deployments are resolved by sourcing the final value from a
     *                {@link org.eclipse.microprofile.config.spi.ConfigSource}
     *                defined by the {@code microprofile-config-smallrye} subsystem.
     * @tpPassCrit The {@code externalDocs} element is not empty
     * @tpSince JBoss EAP XP 6
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testExternalDocsElementGenerationWithMultipleDeployments() throws IOException, URISyntaxException {
        deployer.deploy(STATICALLY_GENERATED_ROUTER_DEPLOYMENT_NAME);
        try {
            deployer.deploy(PROGRAMMATICALLY_MODELED_ROUTER_DEPLOYMENT_NAME);
            try {
                final String element = "externalDocs";
                final ValidatableResponse openApiResponse = MicroProfileOpenApiTestUtils.getGeneratedOpenApi(
                        new URL("http://localhost:8080/openapi"))
                        .body(containsString(element + ":"));
                final String responseContent = openApiResponse.extract().asString();
                Map<String, Object> externalDocs = MicroProfileOpenApiTestUtils.getGeneratedRootElement(responseContent,
                        element);

                Assert.assertFalse("\"externalDocs\" should NOT be empty, since conflicting values should be resolved",
                        externalDocs.isEmpty());
                Assert.assertEquals("Unexpected number of \"externalDocs\" items", 2, externalDocs.size());
                String url = (String) externalDocs.get("url");
                String description = (String) externalDocs.get("description");

                Assert.assertEquals("The generated \"externalDoc.url\" value is not correct",
                        "http://system-property-based-local-service-router-external-docs.org",
                        url);
                Assert.assertEquals("The generated \"externalDoc.description\" value is not correct",
                        "System property based Local Service Router external documentation",
                        description);
            } finally {
                deployer.undeploy(PROGRAMMATICALLY_MODELED_ROUTER_DEPLOYMENT_NAME);
            }
        } finally {
            deployer.undeploy(STATICALLY_GENERATED_ROUTER_DEPLOYMENT_NAME);
        }
    }
}
