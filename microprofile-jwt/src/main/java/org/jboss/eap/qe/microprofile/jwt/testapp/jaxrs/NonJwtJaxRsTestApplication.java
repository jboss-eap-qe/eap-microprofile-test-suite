package org.jboss.eap.qe.microprofile.jwt.testapp.jaxrs;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * Empty JAX-RS application
 */
@ApplicationPath("/")
public class NonJwtJaxRsTestApplication extends Application {
}
