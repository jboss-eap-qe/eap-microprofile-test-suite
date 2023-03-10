package org.jboss.eap.qe.microprofile.jwt.testapp.jaxrs;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.jwt.Claims;
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

    @GET
    @Path("/protected")
    @RolesAllowed({ "NO_ONE" })
    public Response echoRawTokenValueProtected() {
        return Response.ok()
                .entity(callerPrincipal.getRawToken())
                .type(MediaType.TEXT_PLAIN)
                .build();
    }

    @GET
    @Path("/{claim}")
    public Response getClaimValue(@PathParam("claim") String claim) {
        return Response.ok()
                .entity(callerPrincipal.getClaim(Claims.valueOf(claim)))
                .type(MediaType.TEXT_PLAIN)
                .build();
    }
}
