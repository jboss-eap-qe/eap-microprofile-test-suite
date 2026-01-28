package org.jboss.eap.qe.microprofile.openapi.apps.routing.router.rest.noscan;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

import org.jboss.eap.qe.microprofile.openapi.apps.routing.router.model.DistrictObject;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.router.model.PojoExample;

/**
 * Rest resource exposing operations which provide examples, which is not scanned to reflect annotations into
 * the generated OpenAPI document
 */
@Path("/examples")
public class NotScannedLocalServiceRouterExampleResource {
    /**
     * Prints all the example resources as a string
     *
     * @return Response containing the string representation of all the example resources
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response getExamples() {
        return Response.ok(List.of(new DistrictObject(), new PojoExample()).toString()).build();
    }
}
