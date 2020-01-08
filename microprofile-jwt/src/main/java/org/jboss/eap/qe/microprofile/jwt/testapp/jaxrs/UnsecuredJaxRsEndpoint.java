package org.jboss.eap.qe.microprofile.jwt.testapp.jaxrs;

import javax.annotation.security.PermitAll;
import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/unsecured-endpoint")
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
