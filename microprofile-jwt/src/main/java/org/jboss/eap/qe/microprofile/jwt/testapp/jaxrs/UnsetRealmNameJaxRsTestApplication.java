package org.jboss.eap.qe.microprofile.jwt.testapp.jaxrs;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.eclipse.microprofile.auth.LoginConfig;

/**
 * Application which normally activates JWT subsystem using {@code org.eclipse.microprofile.auth.LoginConfig}
 * annotation. This is however annotated by an annotation which is missing {@code realmName};
 */
@LoginConfig(authMethod = "MP-JWT")
@ApplicationPath("/")
public class UnsetRealmNameJaxRsTestApplication extends Application {

}
