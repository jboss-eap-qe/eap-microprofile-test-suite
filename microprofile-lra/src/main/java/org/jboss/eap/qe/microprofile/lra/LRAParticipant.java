package org.jboss.eap.qe.microprofile.lra;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import java.net.URI;

import org.eclipse.microprofile.lra.annotation.AfterLRA;
import org.eclipse.microprofile.lra.annotation.Compensate;
import org.eclipse.microprofile.lra.annotation.Complete;
import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;
import org.jboss.eap.qe.microprofile.lra.model.LRAResult;

public class LRAParticipant {

    public static final String RESULT_PATH = "/result";
    public static final String RESET_PATH = "/reset";

    protected LRAResult lraResult = new LRAResult();

    @Compensate
    @PUT
    @Path("/compensate")
    public Response compensate(@HeaderParam(LRA.LRA_HTTP_CONTEXT_HEADER) URI lraId,
            @HeaderParam(LRA.LRA_HTTP_RECOVERY_HEADER) URI recoveryId) {
        lraResult.setCompleted(false);
        lraResult.setFinishLraId(lraId);
        lraResult.setFinishRecoveryId(recoveryId);
        return Response.ok().build();
    }

    @Complete
    @PUT
    @Path("/complete")
    public Response complete(@HeaderParam(LRA.LRA_HTTP_CONTEXT_HEADER) URI lraId,
            @HeaderParam(LRA.LRA_HTTP_RECOVERY_HEADER) URI recoveryId) {
        lraResult.setCompleted(true);
        lraResult.setFinishLraId(lraId);
        lraResult.setFinishRecoveryId(recoveryId);
        return Response.ok().build();
    }

    @AfterLRA
    @PUT
    @Path("/afterLRA")
    public Response afterLRA(@HeaderParam(LRA.LRA_HTTP_ENDED_CONTEXT_HEADER) URI endedLRA) {
        lraResult.setAfterLraId(endedLRA);
        return Response.ok().build();
    }

    @GET
    @Path(RESULT_PATH)
    public LRAResult getLraResult() {
        return lraResult;
    }

    @PUT
    @Path(RESET_PATH)
    public void resetResult() {
        lraResult = new LRAResult();
    }

}
