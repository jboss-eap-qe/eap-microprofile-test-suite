package org.jboss.eap.qe.microprofile.openapi.apps.routing.router.rest.legacy;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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