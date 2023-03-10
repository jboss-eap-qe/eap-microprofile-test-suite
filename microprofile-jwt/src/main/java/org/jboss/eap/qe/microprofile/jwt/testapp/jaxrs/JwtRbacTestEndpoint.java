package org.jboss.eap.qe.microprofile.jwt.testapp.jaxrs;

import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.eap.qe.microprofile.jwt.testapp.Endpoints;
import org.jboss.eap.qe.microprofile.jwt.testapp.Roles;

/**
 * A servlet which uses RBAC to control who can execute its methods
 */
@Path("/" + Endpoints.RBAC_ENDPOINT)
@DeclareRoles({ Roles.MONITOR, Roles.DIRECTOR, Roles.ADMIN })
public class JwtRbacTestEndpoint {

    @RolesAllowed({ Roles.MONITOR })
    @GET
    @Path(Roles.MONITOR)
    public Response getResponseOnlyForMonitor() {
        return Response.ok()
                .entity(Roles.MONITOR)
                .type(MediaType.TEXT_PLAIN)
                .build();
    }

    @RolesAllowed({ Roles.DIRECTOR })
    @GET
    @Path(Roles.DIRECTOR)
    public Response getResponseOnlyForDirector() {
        return Response.ok()
                .entity(Roles.DIRECTOR)
                .type(MediaType.TEXT_PLAIN)
                .build();
    }

    @GET
    @Path(Roles.ADMIN)
    @RolesAllowed({ Roles.ADMIN })
    public Response getResponseOnlyForAdmin() {
        return Response.ok()
                .entity(Roles.ADMIN)
                .type(MediaType.TEXT_PLAIN)
                .build();
    }

    @RolesAllowed({ Roles.ADMIN, Roles.DIRECTOR })
    @GET
    @Path(Roles.ADMIN + Roles.DIRECTOR)
    public Response getResponseForAdminDirector() {
        return Response.ok()
                .entity(Roles.ADMIN + Roles.DIRECTOR)
                .type(MediaType.TEXT_PLAIN)
                .build();
    }

    @DenyAll
    @GET
    @Path("deny-all")
    public Response getResponseForNone() {
        return Response.ok().build();
    }

}
