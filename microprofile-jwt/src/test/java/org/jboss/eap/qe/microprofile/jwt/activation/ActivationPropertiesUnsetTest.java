package org.jboss.eap.qe.microprofile.jwt.activation;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.eap.qe.microprofile.jwt.EnableJwtSubsystemSetupTask;
import org.jboss.eap.qe.microprofile.jwt.testapp.jaxrs.NonJwtJaxRsTestApplication;
import org.jboss.eap.qe.microprofile.jwt.testapp.jaxrs.SecuredJaxRsEndpoint;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Set of negative tests for subsystem activation.
 */
@RunWith(Arquillian.class)
@ServerSetup(EnableJwtSubsystemSetupTask.class)
public class ActivationPropertiesUnsetTest {

    private static final String UNSET_AUTH_ACTIVATION_PROPERTY_DEPLOYMENT_NAME = "PROPERTIES_UNSET_DEPLOYMENT";
    private static final String NON_JWT_AUTH_ACTIVATION_PROPERTY_DEPLOYMENT_NAME = "OTHER_VALUE_DEPLOYMENT";

    @Deployment(name = UNSET_AUTH_ACTIVATION_PROPERTY_DEPLOYMENT_NAME, managed = false)
    public static Archive<?> createDeploymentNoValue() {
        return ShrinkWrap
                .create(WebArchive.class,
                        ActivationPropertiesUnsetTest.class.getSimpleName() + UNSET_AUTH_ACTIVATION_PROPERTY_DEPLOYMENT_NAME
                                + ".war")
                .addClass(SecuredJaxRsEndpoint.class)
                .addClass(NonJwtJaxRsTestApplication.class)
                .addAsManifestResource(
                        ActivationPropertiesUnsetTest.class.getClassLoader().getResource("mp-config-basic-RS256.properties"),
                        "microprofile-config.properties")
                .addAsManifestResource(
                        ActivationPropertiesUnsetTest.class.getClassLoader().getResource("pki/RS256/key.public.pem"),
                        "key.public.pem");
    }

    @Deployment(name = NON_JWT_AUTH_ACTIVATION_PROPERTY_DEPLOYMENT_NAME, managed = false)
    public static Archive<?> createDeploymentOtherValue() {
        return ShrinkWrap
                .create(WebArchive.class,
                        ActivationPropertiesUnsetTest.class.getSimpleName() + NON_JWT_AUTH_ACTIVATION_PROPERTY_DEPLOYMENT_NAME
                                + ".war")
                .addClass(SecuredJaxRsEndpoint.class)
                .addClass(NonJwtJaxRsTestApplication.class)
                .setWebXML(ActivationPropertiesUnsetTest.class.getClassLoader().getResource("basic-auth-web.xml"))
                .addAsManifestResource(
                        ActivationPropertiesUnsetTest.class.getClassLoader().getResource("mp-config-basic-RS256.properties"),
                        "microprofile-config.properties")
                .addAsManifestResource(
                        ActivationPropertiesUnsetTest.class.getClassLoader().getResource("pki/RS256/key.public.pem"),
                        "key.public.pem");
    }

    /**
     * @tpTestDetails Test that subsystem is not activated when there is not {@code MP-JWT} set as an
     *                {@code auth-method} anywhere (both web.xml and {@code @LoginConfig} annotation).
     * @tpPassCrit Deployment fails because classes required by deployment (MP-JWT API) are not loaded due to the fact
     *             that the subsystem was not activated.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test(expected = org.jboss.arquillian.container.spi.client.container.DeploymentException.class)
    public void testDeploymentFailsWithUnsetAuth(@ArquillianResource Deployer deployer) {
        deployer.deploy(UNSET_AUTH_ACTIVATION_PROPERTY_DEPLOYMENT_NAME);
    }

    /**
     * @tpTestDetails Test that subsystem is not activated when the {@code auth-method} is set to other value than
     *                {@code MP-JWT}.
     * @tpPassCrit Deployment fails because classes required by deployment (MP-JWT API) are not loaded due to the fact
     *             that the subsystem was not activated.
     * @tpSince EAP 7.4.0.CD19
     */
    @Test(expected = org.jboss.arquillian.container.spi.client.container.DeploymentException.class)
    public void testDeploymentFailsAuthOtherValue(@ArquillianResource Deployer deployer) {
        deployer.deploy(NON_JWT_AUTH_ACTIVATION_PROPERTY_DEPLOYMENT_NAME);
    }
}
