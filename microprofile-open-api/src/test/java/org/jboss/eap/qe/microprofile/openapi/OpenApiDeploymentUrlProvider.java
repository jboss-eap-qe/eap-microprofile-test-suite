package org.jboss.eap.qe.microprofile.openapi;

/**
 * Provides useful URLs for MP OpenAPI spec tests
 */
public class OpenApiDeploymentUrlProvider {

    /**
     * Given the deployment name, builds base URL for it
     *
     * @param deploymentName String representing the deployment name
     * @return String representing the base URL for given deployment name
     */
    public static String composeDefaultDeploymentBaseUrl(String deploymentName) {
        return String.format(
                "http://%s:%s/%s",
                OpenApiTestConstants.DEFAULT_HOST_NAME,
                OpenApiTestConstants.DEFAULT_ENDPOINT_PORT, deploymentName);
    }

    /**
     * Builds default OpenAPI endpoint URL, as per MP OpenAPI 1.1 specs
     * 
     * @return
     */
    public static String composeDefaultOpenApiUrl() {
        return String.format(
                "http://%s:%s/openapi",
                OpenApiTestConstants.DEFAULT_HOST_NAME,
                OpenApiTestConstants.DEFAULT_ENDPOINT_PORT);
    }
}
