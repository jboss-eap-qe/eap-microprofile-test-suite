package org.jboss.eap.qe.microprofile.lra;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;

import org.eclipse.microprofile.lra.annotation.AfterLRA;
import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;
import org.jboss.logging.Logger;

@ApplicationScoped
@Path(LRAController.LRA_PATH)
@Produces(MediaType.TEXT_PLAIN)
public class LRAController {
    public static final String LRA_PATH = "lra";
    public static final String END_LRA_PATH = "end-lra";
    public static final String PROPAGATE_MANUAL = "propagate-manual";
    public static final String PROPAGATE_CHAIN = "propagate-chain";
    public static final String BASE_URI = "Base-URL";
    public static final String FAIL_LRA = "fail-lra";
    public static final String DELAYED_FAIL = "delayed-fail";

    private static final Logger LOG = Logger.getLogger(LRAController.class);

    private Client client;
    private URI baseURI;

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

    @LRA(end = false)
    @GET
    public Response start(@HeaderParam(LRA.LRA_HTTP_CONTEXT_HEADER) URI lraId,
            @HeaderParam(BASE_URI) URI baseURI) {
        this.baseURI = baseURI;
        return Response.ok(lraId).build();
    }

    @LRA(value = LRA.Type.MANDATORY, end = false)
    @Path(PROPAGATE_MANUAL)
    @GET
    public void propagateManual(@HeaderParam(LRA.LRA_HTTP_CONTEXT_HEADER) URI lraId,
            @QueryParam(FAIL_LRA) boolean failParticipant) {
        try (Response ignored = client.target(baseURI)
                .path(LRAManualPropagationParticipant.PATH)
                .queryParam(FAIL_LRA, failParticipant)
                .request()
                .header(LRA.LRA_HTTP_CONTEXT_HEADER, lraId)
                .get()) {
        }
    }

    @LRA(value = LRA.Type.MANDATORY, end = false)
    @Path(PROPAGATE_CHAIN)
    @GET
    public void propagateChain(@HeaderParam(LRA.LRA_HTTP_CONTEXT_HEADER) URI lraId,
            @QueryParam(FAIL_LRA) boolean failParticipant) {
        try (Response ignored = client.target(baseURI)
                .path(LRAChainPropagationParticipant1.PATH)
                .queryParam(FAIL_LRA, failParticipant)
                .request()
                .header(LRAController.BASE_URI, baseURI)
                .header(LRA.LRA_HTTP_CONTEXT_HEADER, lraId)
                .get()) {
        }
    }

    @LRA(value = LRA.Type.MANDATORY)
    @Path(END_LRA_PATH)
    @GET
    public Response endLRA(@HeaderParam(LRA.LRA_HTTP_CONTEXT_HEADER) URI lraId,
            @QueryParam(FAIL_LRA) boolean failLRA) {
        return failLRA ? Response.status(Response.Status.BAD_REQUEST).entity(lraId).build() : Response.ok(lraId).build();
    }

    // not tested, only required by the specification
    @AfterLRA
    @PUT
    public Response afterLRA() {
        return Response.ok().build();
    }

}
