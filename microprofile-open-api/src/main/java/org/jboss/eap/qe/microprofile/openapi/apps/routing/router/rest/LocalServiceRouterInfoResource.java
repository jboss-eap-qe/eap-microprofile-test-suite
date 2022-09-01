package org.jboss.eap.qe.microprofile.openapi.apps.routing.router.rest;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirements;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.security.SecuritySchemes;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Rest resource exposing operations which provide information about Local Service Router, this is a "non-routed"
 * service - i.e. only related to the Local Service Router domain.
 */
@SecuritySchemes(value = {
        @SecurityScheme(securitySchemeName = "http_secured", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
})
@SecurityRequirements(value = {
        @SecurityRequirement(name = "http_secured") })
@Path("/info")
public class LocalServiceRouterInfoResource {

    /**
     * Returns local service router FQDN
     *
     * @return Response containing the local service router FQDN
     */
    @GET
    @Path("/fqdn")
    @Produces(MediaType.TEXT_PLAIN)
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Local Service Provider FQDN", content = @Content(mediaType = MediaType.TEXT_PLAIN)),
            @APIResponse(responseCode = "500", description = "An error occurred while retrieving Local Service Provider FQDN") })
    @Operation(summary = "Get local service router FQDN", description = "Retrieves and returns the local service router FQDN", operationId = "getFullyQualifiedDomainName")
    public Response getFullyQualifiedDomainName() throws UnknownHostException {
        return Response.ok(InetAddress.getLocalHost().getHostName()).build();
    }
}
