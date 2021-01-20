package org.jboss.eap.qe.microprofile.jwt.deployment;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.eap.qe.microprofile.jwt.EnableJwtSubsystemSetupTask;
import org.jboss.eap.qe.microprofile.jwt.testapp.jaxrs.JaxRsTestApplication;
import org.jboss.eap.qe.microprofile.jwt.testapp.jaxrs.SessionScopedJaxRsEndpoint;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * MP-JWT implementations are required to generate a {@code javax.enterprise.inject.spi.DeploymentException} for a claim value
 * injection into Passivation capable beans, for example, @SessionScoped
 * (see https://github.com/eclipse/microprofile-jwt-auth/issues/183)
 *
 * @author tborgato
 */
@RunAsClient
@RunWith(Arquillian.class)
@ServerSetup(EnableJwtSubsystemSetupTask.class)
public class SessionScopedClaimInjectionTest {

    private static final String DEPLOYMENT_NAME = "FAULTY_DEPLOYMENT";

    @Deployment(name = DEPLOYMENT_NAME, managed = false)
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, SessionScopedClaimInjectionTest.class.getSimpleName() + ".war")
                .addClass(SessionScopedJaxRsEndpoint.class)
                .addClass(JaxRsTestApplication.class)
                .addAsManifestResource(
                        SessionScopedClaimInjectionTest.class.getClassLoader().getResource("mp-config-jwe-RS256.properties"),
                        "microprofile-config.properties")
                .addAsManifestResource(
                        SessionScopedClaimInjectionTest.class.getClassLoader().getResource("pki/RS256/key.public.pem"),
                        "key.public.pem");
    }

    /**
     * @tpTestDetails Deploys and app containing a @SessionScoped bean with a claim value injection
     * @tpPassCrit Expect deployment to fail with javax.enterprise.inject.spi.DeploymentException
     * @tpSince EAP 7.4.0.CD23
     */
    @Test(expected = org.jboss.arquillian.container.spi.client.container.DeploymentException.class)
    public void testDeploymentException(@ArquillianResource Deployer deployer) {
        deployer.deploy(DEPLOYMENT_NAME);
    }
}
