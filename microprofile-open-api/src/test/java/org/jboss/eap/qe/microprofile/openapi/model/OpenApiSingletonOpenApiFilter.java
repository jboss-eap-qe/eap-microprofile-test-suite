package org.jboss.eap.qe.microprofile.openapi.model;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.ExternalDocumentation;
import org.eclipse.microprofile.openapi.models.OpenAPI;

/**
 * A filter to make final configuration changes to the produced OpenAPI document.
 *
 * <p>
 * This specific class alters the singleton {@code OpenAPI element}
 * </p>
 */
public class OpenApiSingletonOpenApiFilter implements OASFilter {

    /**
     * Adds a custom {@link ExternalDocumentation} value.
     */
    @Override
    public void filterOpenAPI(OpenAPI openAPI) {
        ExternalDocumentation externalDocumentation = OASFactory.createExternalDocumentation();
        externalDocumentation.setUrl("http://oas-filter-based-external-docs.org");
        externalDocumentation.setDescription("Could be overridden only by other filters in the chain");
        openAPI.setExternalDocs(externalDocumentation);
    }
}
