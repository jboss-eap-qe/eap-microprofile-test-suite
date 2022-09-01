package org.jboss.eap.qe.microprofile.openapi.apps.routing.router.rest.routed;

import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.router.model.DistrictObject;
import org.jboss.eap.qe.microprofile.openapi.apps.routing.router.services.DistrictServiceClient;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Rest resource exposing Service Provider operations for districts management services - Local Service Router
 * "routed" services must abide to the Services Provider definition
 */
@Path("/districts")
public class RouterDistrictsResource {

    DistrictServiceClient serviceClient;

    @Inject
    @ConfigProperty(name = "services.provider.host")
    String servicesProviderHost;
    @Inject
    @ConfigProperty(name = "services.provider.port")
    int servicesProviderPort;

    @PostConstruct
    private void initializeRestClient() throws URISyntaxException {
        serviceClient = RestClientBuilder.newBuilder()
                .baseUri(new URI(
                        String.format(
                                "http://%s:%d/serviceProviderDeployment", servicesProviderHost, servicesProviderPort)))
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
     *
     * @param code String that uniquely identifies a District
     * @param district instance carrying data to update the stored entity
     * @return District instance representing the updated stored entity
     */
    @PATCH
    @Path("/{code}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateDistrict(@PathParam("code") String code, @RequestBody DistrictObject district) {
        return serviceClient.updateDistrict(code, district);
    }
}
