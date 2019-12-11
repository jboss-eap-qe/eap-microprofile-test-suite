package org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.rest;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.extensions.Extensions;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.RoutingServiceConstants;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.api.DistrictService;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.data.DistrictEntity;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.model.District;

/**
 * Rest resource exposing operations which provide access to districts management services
 */
@Path("/districts")
public class DistrictsResource {

    @Inject
    DistrictService districtService;

    /**
     * Returns all available districts
     *
     * @return Response containing the list of all available districts
     */
    @GET
    @Path("/all")
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "All available districts", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(type = SchemaType.ARRAY, implementation = District.class))) })
    @Extensions({
            @Extension(name = RoutingServiceConstants.OPENAPI_OPERATION_EXTENSION_PROXY_FQDN_NAME, value = RoutingServiceConstants.OPENAPI_OPERATION_EXTENSION_PROXY_FQDN_DEFAULT_VALUE),
            @Extension(name = "x-string-property", value = "string-value"),
            @Extension(name = "x-boolean-property", value = "true", parseValue = true),
            @Extension(name = "x-number-property", value = "42", parseValue = true),
            @Extension(name = "x-object-property", value = "{ \"property-1\" : \"value-1\", \"property-2\" : \"value-2\", \"property-3\" : { \"prop-3-1\" : 42, \"prop-3-2\" : true } }", parseValue = true),
            @Extension(name = "x-string-array-property", value = "[ \"one\", \"two\", \"three\" ]", parseValue = true),
            @Extension(name = "x-object-array-property", value = "[ { \"name\": \"item-1\" }, { \"name\" : \"item-2\" } ]", parseValue = true)
    })
    @Operation(summary = "Get all districts", description = "Retrieves and returns the available districts", operationId = "getAllDistricts")
    public Response getAllDistricts() {
        return Response.ok().entity(districtService.getAll()).build();
    }

    /**
     * Returns the district identified by the given code
     *
     * @param code String that uniquely identifies a District
     * @param excludeObsolete Whether the found entity has to be returned when marked as obsolete
     * @return Response containing the requested district data. HTTP 204 is returned when no item was found for the
     *         given code or - in case the given "excludeObsolete" parameter is set to True - an element was found but is marked
     *         as obsolete.
     */
    @GET
    @Path("/{code}")
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Requested district", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = District.class))),
            @APIResponse(responseCode = "204", description = "Requested district was not found") })
    @Extensions({
            @Extension(name = RoutingServiceConstants.OPENAPI_OPERATION_EXTENSION_PROXY_FQDN_NAME, value = RoutingServiceConstants.OPENAPI_OPERATION_EXTENSION_PROXY_FQDN_DEFAULT_VALUE)
    })
    @Operation(summary = "Get district by code", description = "Retrieves and returns the requested district", operationId = "getDistrictByCOde")
    public Response getDistrictByCode(@PathParam("code") String code, @QueryParam("excludeObsolete") Boolean excludeObsolete) {
        Boolean doExcludeObsolete = excludeObsolete != null ? excludeObsolete : Boolean.FALSE;
        District result = districtService.getByCode(code);
        if ((result == null) || (doExcludeObsolete && result.getObsolete())) {
            return Response.noContent().build();
        } else
            return Response.ok().entity(result).build();
    }

    /***
     * Updates a district data
     *
     * @param {@link District} instance carrying data to update the stored entity
     * @return District instance representing the updated stored entoty
     */
    @PATCH
    @Path("/{code}")
    @Consumes(MediaType.APPLICATION_JSON)
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Updated district", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = District.class), example = "{'code': 'DW', 'name': 'Western district', 'obsolete': false}")),
            @APIResponse(responseCode = "404", description = "Requested district was not found") })
    @Extensions({
            @Extension(name = "x-string-property", value = "string-value"),
            @Extension(name = "x-boolean-property", value = "true", parseValue = true),
            @Extension(name = "x-number-property", value = "42", parseValue = true),
            @Extension(name = "x-object-property", value = "{ \"property-1\" : \"value-1\", \"property-2\" : \"value-2\", \"property-3\" : { \"prop-3-1\" : 42, \"prop-3-2\" : true } }", parseValue = true),
            @Extension(name = "x-string-array-property", value = "[ \"one\", \"two\", \"three\" ]", parseValue = true),
            @Extension(name = "x-object-array-property", value = "[ { \"name\": \"item-1\" }, { \"name\" : \"item-2\" } ]", parseValue = true)
    })
    public Response updateDistrict(@RequestBody DistrictEntity district) {
        District result = districtService.getByCode(district.getCode());
        if (result == null)
            return Response.status(Response.Status.NOT_FOUND).build();
        else {

            return Response.ok().entity(districtService.update(district.getCode(), district)).build();
        }
    }
}