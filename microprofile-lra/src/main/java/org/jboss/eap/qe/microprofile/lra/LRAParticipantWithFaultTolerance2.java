package org.jboss.eap.qe.microprofile.lra;

import static org.jboss.eap.qe.microprofile.lra.LRAParticipantWithFaultTolerance2.PATH;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;

import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;
import org.jboss.logging.Logger;

@Path(PATH)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
public class LRAParticipantWithFaultTolerance2 extends LRAParticipant {

    private static final Logger LOGGER = Logger.getLogger(LRAParticipantWithFaultTolerance2.class);

    public static final String PATH = "lra-participant-with-fault-tolerance-2";

    @LRA(value = LRA.Type.MANDATORY, end = false)
    @GET
    public Response doLRA(@HeaderParam(LRA.LRA_HTTP_CONTEXT_HEADER) URI lraId,
            @HeaderParam(LRA.LRA_HTTP_RECOVERY_HEADER) URI recoveryId) {
        LOGGER.infof("Executing action of Participant 2 enlisted in LRA %s " +
                "that was assigned %s participant Id.", lraId, recoveryId);

        lraResult.setLraId(lraId);
        lraResult.setRecoveryId(recoveryId);

        return Response.ok().build();
    }
}
