package org.jboss.eap.qe.microprofile.openapi.apps.routing.router.rest;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.links.Link;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBodySchema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.router.model.DistrictObject;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.router.model.PojoExample;

/**
 * Rest resource exposing operations which provide examples about Local Service Router, this is a "non-routed"
 * service - i.e. only related to the Local Service Router domain.
 */
@Path("/examples")
public class LocalServiceRouterExampleResource {
    /**
     * Prints a {@link DistrictObject} resource as a string
     *
     * @return Response containing the string representation of a {@link DistrictObject} resource
     */
    @GET
    @Path("/district-example")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponse(responseCode = "200", links = @Link(ref = "districtInformationLink"), content = @Content(mediaType = "application/json"))
    public Response getDistrictExample(@RequestBodySchema(DistrictObject.class) DistrictObject district) {
        return Response.ok(district.toString()).build();
    }

    /**
     * Prints a {@link PojoExample} resource as a string
     *
     * @return Response containing the string representation of a {@link PojoExample} resource
     */
    @GET
    @Path("/pojo-example")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponse(responseCode = "200", description = "Returns a single PojoExample.", links = @Link(ref = "districtInformationLink"), content = @Content(mediaType = "application/json", examples = @ExampleObject(
            // Reference the named example in the static file, ad should be adjusted if it was given a
            // different name in the model tree, as it happens with multiple deployments
            ref = "pojoExample")))
    public Response getPojoExampleExample(@RequestBodySchema(PojoExample.class) PojoExample pojoExample) {
        return Response.ok(pojoExample.toString()).build();
    }
}
