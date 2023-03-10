package org.jboss.eap.qe.microprofile.jwt.testapp.jaxrs;

import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.eap.qe.microprofile.jwt.testapp.Endpoints;

@Path("/" + Endpoints.UNSECURED_ENDPOINT)
@ApplicationScoped
public class UnsecuredJaxRsEndpoint {

    @PermitAll
    @GET
    public Response echoHello() {
        return Response.ok()
                .entity("hello")
                .type(MediaType.TEXT_PLAIN)
                .build();
    }

}
