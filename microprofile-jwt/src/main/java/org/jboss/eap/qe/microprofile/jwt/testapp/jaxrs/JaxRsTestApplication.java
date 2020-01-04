package org.jboss.eap.qe.microprofile.jwt.testapp.jaxrs;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.eclipse.microprofile.auth.LoginConfig;

/**
 * Application which activates JWT subsystem using {@code org.eclipse.microprofile.auth.LoginConfig} annotation
 */
@LoginConfig(authMethod = "MP-JWT",
        //"Virtual" security domain
        realmName = "MP-JWT")
@ApplicationPath("/")
public class JaxRsTestApplication extends Application {

}
