package org.jboss.eap.qe.microprofile.openapi.apps.routing.router.rest;

import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;

/**
 * Rest resource exposing operations which provide information about Local Service Router, this is a "non-routed"
 * service - i.e. only related to the Local Service Router domain
 */
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
