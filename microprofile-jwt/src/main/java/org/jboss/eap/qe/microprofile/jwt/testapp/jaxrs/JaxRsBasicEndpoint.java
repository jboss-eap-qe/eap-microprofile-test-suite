package org.jboss.eap.qe.microprofile.jwt.testapp.jaxrs;

import org.eclipse.microprofile.jwt.Claim;
import org.eclipse.microprofile.jwt.ClaimValue;
import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/basic-endpoint")
@ApplicationScoped
public class JaxRsBasicEndpoint {

    @Inject
    private JsonWebToken callerPrincipal;

    @GET
    public Response echoRawTokenValue(@HeaderParam("Authorization") String userAgent) {
        return Response.ok()
                .entity(callerPrincipal.getRawToken())
                .type(MediaType.TEXT_PLAIN)
                .build();
    }
}
