package org.jboss.eap.qe.microprofile.openapi.model;

import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.Operation;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.RoutingServiceConstants;

/**
 * A filter to make final configuration changes to the produced OpenAPI document.
 */
public class OpenApiFilter implements OASFilter {

    public static final String LOCAL_TEST_ROUTER_FQDN = "local.test.district.unknown";

    /**
     * Replaces {@link RoutingServiceConstants#OPENAPI_OPERATION_EXTENSION_PROXY_FQDN_NAME} extension value with
     * local router FQDN
     */
    @Override
    public Operation filterOperation(Operation operation) {
        if ((operation.getExtensions() != null)
                && operation.getExtensions().containsKey(RoutingServiceConstants.OPENAPI_OPERATION_EXTENSION_PROXY_FQDN_NAME)) {
            operation.getExtensions().replace(RoutingServiceConstants.OPENAPI_OPERATION_EXTENSION_PROXY_FQDN_NAME,
                    LOCAL_TEST_ROUTER_FQDN);
        }
        return operation;
    }
}
