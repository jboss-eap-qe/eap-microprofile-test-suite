package org.jboss.eap.qe.microprofile.jwt;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.ConfigurationException;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.creaper.ManagementClientProvider;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.operations.Address;
import org.wildfly.extras.creaper.core.online.operations.OperationException;
import org.wildfly.extras.creaper.core.online.operations.Operations;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;

/**
 * Class aggregating all subsystem related tests. If there ever will be configuration options for subsystem, this is the
 * place to add tests for them.
 */
@RunAsClient
@RunWith(Arquillian.class)
public class MicroProfileJwtSubsystemTestCase {

    private static final String MP_JWT_SUBSYSTEM_NAME = "microprofile-jwt-smallrye";

    private static final Address MP_JWT_SUBSYSTEM_ADDRESS = Address.subsystem(MP_JWT_SUBSYSTEM_NAME);

    private OnlineManagementClient client;
    private boolean wasSubsystemRemovedInPrepare = false;

    @Before
    public void before() throws ConfigurationException, IOException, OperationException, TimeoutException,
            InterruptedException {
        client = ManagementClientProvider.onlineStandalone();
        final Operations ops = new Operations(client);

        if (ops.exists(MP_JWT_SUBSYSTEM_ADDRESS)) {
            ops.remove(MP_JWT_SUBSYSTEM_ADDRESS).assertSuccess();
            new Administration(client).reloadIfRequired();
            wasSubsystemRemovedInPrepare = true;
        }
    }

    @After
    public void after() throws IOException, TimeoutException, InterruptedException {
        if (wasSubsystemRemovedInPrepare) {
            new Operations(client).add(MP_JWT_SUBSYSTEM_ADDRESS).assertSuccess();
            new Administration(client).reloadIfRequired();
        }

        client.close();
    }

    /**
     * @tpTestDetails Add and remove MicroProfile JWT subsystem.
     * @tpPassCrit Subsystem is successfully added and removed.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test
    public void addRemoveSubsystemTest() throws IOException, OperationException, TimeoutException,
            InterruptedException {
        final Operations ops = new Operations(client);
        final Administration administration = new Administration(client);

        ops.add(MP_JWT_SUBSYSTEM_ADDRESS).assertSuccess();
        administration.reloadIfRequired();
        Assert.assertTrue(ops.exists(MP_JWT_SUBSYSTEM_ADDRESS));
        ops.remove(MP_JWT_SUBSYSTEM_ADDRESS).assertSuccess();
        administration.reloadIfRequired();
        Assert.assertFalse(ops.exists(MP_JWT_SUBSYSTEM_ADDRESS));
    }

}
