package org.jboss.eap.qe.microprofile.openapi.apps.routing.router.rest.routed;

import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.router.model.DistrictObject;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.router.services.DistrictServiceClient;

/**
 * Rest resource exposing Service Provider operations for districts management services - Local Service Router
 * "routed" services must abide to the Services Provider definition
 */
@Path("/districts")
public class RouterDistrictsResource {

    DistrictServiceClient serviceClient;

    public RouterDistrictsResource() throws URISyntaxException {
        serviceClient = RestClientBuilder.newBuilder()
                .baseUri(new URI("http://localhost:8080/serviceProviderDeployment"))
                .build(DistrictServiceClient.class);
    }

    /**
     * Returns all available districts
     *
     * @return Response containing the list of all available districts
     */
    @GET
    @Path("/all")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllDistricts() {
        return serviceClient.getAllDistricts();
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
    public Response getDistrictByCode(@PathParam("code") String code, @QueryParam("excludeObsolete") Boolean excludeObsolete) {
        return serviceClient.getDistrictByCode(code, excludeObsolete);
    }

    /***
     * Updates a district data
     *
     * @param {@link {@link DistrictObject}} instance carrying data to update the stored entity
     * @return District instance representing the updated stored entity
     */
    @PATCH
    @Path("/{code}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateDistrict(@RequestBody DistrictObject district) {
        return serviceClient.updateDistrict(district.getCode(), district);
    }
}
