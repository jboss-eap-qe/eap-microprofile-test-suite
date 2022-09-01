package org.jboss.eap.qe.microprofile.openapi.apps.routing.router.rest.legacy;

import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/contact/{id}")
@RequestScoped
public class Contact {
    private final String id;

    public Contact(@PathParam("id") String id) {
        this.id = id;
    }

    @Path("details")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getDetails() {
        return String.format("ID: %s", id);
    }
}