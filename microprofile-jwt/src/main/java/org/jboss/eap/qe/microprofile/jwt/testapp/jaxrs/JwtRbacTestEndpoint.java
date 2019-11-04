package org.jboss.eap.qe.microprofile.jwt.testapp.jaxrs;

import org.jboss.eap.qe.microprofile.jwt.testapp.Roles;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

/**
 * A servlet which uses RBAC to control who can execute its methods
 */
@Path("/rbac-endpoint")
@DeclareRoles({Roles.USER1, Roles.USER2, Roles.ADMIN})
public class JwtRbacTestEndpoint {

    @RolesAllowed({Roles.USER1})
    @GET
    @Path("/user1")
    public Response getResponseOnlyForUser1() {
        return null;
    }

}
