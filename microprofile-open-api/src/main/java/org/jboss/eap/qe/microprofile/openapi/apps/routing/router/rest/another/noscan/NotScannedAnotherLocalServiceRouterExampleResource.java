package org.jboss.eap.qe.microprofile.openapi.apps.routing.router.rest.another.noscan;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

import org.jboss.eap.qe.microprofile.openapi.apps.routing.router.model.another.AnotherPojoExample;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.router.model.another.DistrictObject;

/**
 * "Another" Local Service Router REST resource providing examples, which is not scanned to reflect annotations into
 * the generated OpenAPI document
 */
@Path("/example")
public class NotScannedAnotherLocalServiceRouterExampleResource {

    /**
     * Prints all the example resources as a string
     *
     * @return Response containing the string representation of all the example resources
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response getExamples() {
        return Response.ok(List.of(new DistrictObject(), new AnotherPojoExample()).toString()).build();
    }
}
