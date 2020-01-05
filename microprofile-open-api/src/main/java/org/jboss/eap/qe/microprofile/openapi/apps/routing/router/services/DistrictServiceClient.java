package org.jboss.eap.qe.microprofile.openapi.apps.routing.router.services;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.router.model.DistrictObject;

/**
 * Rest Client implementation to access Service Provider endpoints
 */
@Path("/districts")
@RegisterRestClient
public interface DistrictServiceClient {

    /**
     * Returns all available districts
     *
     * @return Response containing the list of all available districts
     */
    @GET
    @Path("/all")
    @Produces(MediaType.APPLICATION_JSON)
    Response getAllDistricts();

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
    Response getDistrictByCode(@PathParam("code") String code, @QueryParam("excludeObsolete") Boolean excludeObsolete);

    /***
     * Updates a district data
     *
     * @param {@link District} instance carrying data to update the stored entity
     * @return District instance representing the updated stored entity
     */
    @PATCH
    @Path("/{code}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Response updateDistrict(@PathParam("code") String code, @RequestBody DistrictObject district);
}
