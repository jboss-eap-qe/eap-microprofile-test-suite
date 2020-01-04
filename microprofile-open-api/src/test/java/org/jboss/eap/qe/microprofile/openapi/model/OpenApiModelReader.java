package org.jboss.eap.qe.microprofile.openapi.model;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.OASModelReader;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.info.Info;

/**
 * Generates a base model to be used by the OpenAPI.
 */
public class OpenApiModelReader implements OASModelReader {
    /**
     * Creates a new {@link Info} instance, modifying the output OpenAPI document in order to add local router
     * information
     *
     * @return An new {@link OpenAPI} instance that will serve as base for generation
     */
    @Override
    public OpenAPI buildModel() {
        OpenAPI api = OASFactory.createObject(OpenAPI.class);
        Info info = OASFactory.createInfo();
        String newDescription = ((info.getDescription() == null) || (info.getDescription().isEmpty()) ? ""
                : info.getDescription()) +
                String.format("[Generated by router: %s]", OpenApiFilter.LOCAL_TEST_ROUTER_FQDN);
        info.setDescription(newDescription);
        api.setInfo(info);
        return api;
    }
}
