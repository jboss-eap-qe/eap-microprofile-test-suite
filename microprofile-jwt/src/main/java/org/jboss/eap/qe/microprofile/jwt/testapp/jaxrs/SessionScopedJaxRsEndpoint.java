package org.jboss.eap.qe.microprofile.jwt.testapp.jaxrs;

import org.eclipse.microprofile.jwt.Claim;
import org.eclipse.microprofile.jwt.ClaimValue;
import org.jboss.eap.qe.microprofile.jwt.testapp.Endpoints;

import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * MP-JWT implementations are required to generate a {@code jakarta.enterprise.inject.spi.DeploymentException} for a claim value
 * injection into Passivation capable beans, for example, @SessionScoped
 * (see https://github.com/eclipse/microprofile-jwt-auth/issues/183)
 */
@Path("/" + Endpoints.SESSION_SCOPED_ENDPOINT)
@SessionScoped
public class SessionScopedJaxRsEndpoint {

    @Inject
    @Claim("raw_token")
    private ClaimValue<String> rawToken;

    @PermitAll
    @GET
    public Response echoHello() {
        return Response.ok()
                .entity("hello")
                .type(MediaType.TEXT_PLAIN)
                .build();
    }
}
