package org.jboss.eap.qe.microprofile.jwt.testapp.jaxrs;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.DenyAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
