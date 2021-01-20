package org.jboss.eap.qe.microprofile.jwt.testapp.jaxrs;

import javax.annotation.security.PermitAll;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.jwt.Claim;
import org.eclipse.microprofile.jwt.ClaimValue;
import org.jboss.eap.qe.microprofile.jwt.testapp.Endpoints;

/**
 * MP-JWT implementations are required to generate a {@code javax.enterprise.inject.spi.DeploymentException} for a claim value
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
