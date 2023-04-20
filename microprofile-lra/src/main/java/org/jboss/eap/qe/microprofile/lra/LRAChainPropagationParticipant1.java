package org.jboss.eap.qe.microprofile.lra;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;

import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;

@Path(LRAChainPropagationParticipant1.PATH)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class LRAChainPropagationParticipant1 extends LRAParticipant {

    public static final String PATH = "lra-chain-participant-1";

    private Client client;

    @PostConstruct
    public void init() {
        client = ClientBuilder.newClient();
    }

    @PreDestroy
    public void destroy() {
        if (client != null) {
            client.close();
        }
    }

    @LRA(value = LRA.Type.MANDATORY, end = false)
    @GET
    public void doInLRA(@HeaderParam(LRA.LRA_HTTP_CONTEXT_HEADER) URI lraId,
            @HeaderParam(LRA.LRA_HTTP_RECOVERY_HEADER) URI recoveryId,
            @HeaderParam(LRAController.BASE_URI) URI baseURI,
            @QueryParam(LRAController.FAIL_LRA) boolean delayedFail) {
        lraResult.setLraId(lraId);
        lraResult.setRecoveryId(recoveryId);

        try (Response ignored = client.target(baseURI)
                .path(LRAChainPropagationParticipant2.PATH)
                .queryParam(LRAController.FAIL_LRA, delayedFail)
                .request()
                .header(LRA.LRA_HTTP_CONTEXT_HEADER, lraId)
                .get()) {
        }
    }
}
