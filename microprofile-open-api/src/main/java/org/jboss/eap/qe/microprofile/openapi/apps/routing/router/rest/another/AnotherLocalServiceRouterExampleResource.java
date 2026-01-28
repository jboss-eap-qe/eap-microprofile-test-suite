package org.jboss.eap.qe.microprofile.openapi.apps.routing.router.rest.another;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.links.Link;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBodySchema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirements;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.security.SecuritySchemes;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.router.model.another.AnotherPojoExample;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.router.model.another.DistrictObject;

/**
 * "Another" Local Service Router REST resource providing examples based on conflicting and non-conflicting resources.
 *
 * <p>
 * The class provides REST APIs that conflict with the other deployments by using different referenced class
 * as the {@link RequestBodySchema} value, but with the same {@link Class#getSimpleName()},
 * i.e. {@link DistrictObject}.
 * Additionally, the class is annotated with a {@link SecurityRequirement} instance with the same {@code name} of
 * the one in {@link org.jboss.eap.qe.microprofile.openapi.apps.routing.router.rest.LocalServiceRouterInfoResource},
 * but with conflicting {@code type}.
 * </p>
 */
@SecuritySchemes(value = {
        @SecurityScheme(securitySchemeName = "http_secured", type = SecuritySchemeType.OAUTH2)
})
@SecurityRequirements(value = {
        @SecurityRequirement(name = "http_secured") })
@Path("/another-example")
public class AnotherLocalServiceRouterExampleResource {
    /**
     * Prints a {@link DistrictObject} resource as a string
     *
     * @return Response containing the string representation of a {@link DistrictObject} resource
     */
    @GET
    @Path("/another-district-example")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponse(responseCode = "200", links = @Link(ref = "districtInformationLink"), content = @Content(mediaType = "application/json"))
    public Response getAnotherDistrictExample(@RequestBodySchema(DistrictObject.class) DistrictObject district) {
        return Response.ok(district.toString()).build();
    }

    /**
     * Prints a {@link AnotherPojoExample} resource as a string
     *
     * @return Response containing the string representation of a {@link AnotherPojoExample} resource
     */
    @GET
    @Path("/another-pojo-example")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponse(responseCode = "200", description = "Returns a single AnotherPojoExample.", content = @Content(mediaType = "application/json", examples = @ExampleObject(
            // Reference the named example in the static file, ad should be adjusted if it was given a
            // different name in the model tree, as it happens with multiple deployments
            ref = "pojoExample")))
    public Response getAnotherPojoExampleExample(
            @RequestBodySchema(AnotherPojoExample.class) AnotherPojoExample anotherPojoExample) {
        return Response.ok(anotherPojoExample.toString()).build();
    }
}
