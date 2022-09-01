package org.jboss.eap.qe.microprofile.opentracing.v10;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

/**
 * A REST resource exposing single operation with providing access to the traced service.
 */
@Path("/")
public class RestSimpleResource {
    @Inject
    private MyService myService;

    @GET
    public Response tracedOperation() {
        return Response.ok().entity(myService.hello()).build();
    }
}
