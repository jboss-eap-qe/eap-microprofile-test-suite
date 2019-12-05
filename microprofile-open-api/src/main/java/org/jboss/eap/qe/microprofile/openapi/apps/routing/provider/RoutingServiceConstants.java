package org.jboss.eap.qe.microprofile.openapi.apps.routing.provider;

/**
 * Constants needed for service routing
 */
public class RoutingServiceConstants {
    /**
     * Name of the operation extension that carries the name of local service router
     */
    public final static String OPENAPI_OPERATION_EXTENSION_PROXY_FQDN_NAME = "x-routing-fqdn";

    /**
     * Default value of the operation extension that carries the name of local service router
     */
    public final static String OPENAPI_OPERATION_EXTENSION_PROXY_FQDN_DEFAULT_VALUE = "provider.acme.unknown";
}
