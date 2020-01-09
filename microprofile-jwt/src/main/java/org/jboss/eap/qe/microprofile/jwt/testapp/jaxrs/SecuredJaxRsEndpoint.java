package org.jboss.eap.qe.microprofile.jwt.testapp.jaxrs;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.eap.qe.microprofile.jwt.testapp.Endpoints;

@Path("/" + Endpoints.SECURED_ENDPOINT)
@ApplicationScoped
public class SecuredJaxRsEndpoint {

    @Inject
    private JsonWebToken callerPrincipal;

    @GET
    public Response echoRawTokenValue() {
        return Response.ok()
                .entity(callerPrincipal.getRawToken())
                .type(MediaType.TEXT_PLAIN)
                .build();
    }
}
