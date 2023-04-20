package org.jboss.eap.qe.microprofile.lra;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;

import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;

@Path(LRAChainPropagationParticipant2.PATH)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class LRAChainPropagationParticipant2 extends LRAParticipant {

    public static final String PATH = "lra-chain-participant-2";

    @LRA(value = LRA.Type.MANDATORY, end = false)
    @GET
    public Response doInLRA(@HeaderParam(LRA.LRA_HTTP_CONTEXT_HEADER) URI lraId,
            @HeaderParam(LRA.LRA_HTTP_RECOVERY_HEADER) URI recoveryId,
            @QueryParam(LRAController.FAIL_LRA) boolean shouldFail) {
        lraResult.setLraId(lraId);
        lraResult.setRecoveryId(recoveryId);

        if (shouldFail) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        return Response.ok().build();
    }
}
