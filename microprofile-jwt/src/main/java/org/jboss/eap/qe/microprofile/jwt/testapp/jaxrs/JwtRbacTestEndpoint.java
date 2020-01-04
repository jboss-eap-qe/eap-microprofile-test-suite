package org.jboss.eap.qe.microprofile.jwt.testapp.jaxrs;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.jboss.eap.qe.microprofile.jwt.testapp.Roles;

/**
 * A servlet which uses RBAC to control who can execute its methods
 */
@Path("/rbac-endpoint")
@DeclareRoles({ Roles.MONITOR, Roles.DIRECTOR, Roles.ADMIN })
public class JwtRbacTestEndpoint {

    @RolesAllowed({ Roles.MONITOR })
    @GET
    @Path(Roles.MONITOR)
    public Response getResponseOnlyForMonitor() {
        return null;
    }

}
