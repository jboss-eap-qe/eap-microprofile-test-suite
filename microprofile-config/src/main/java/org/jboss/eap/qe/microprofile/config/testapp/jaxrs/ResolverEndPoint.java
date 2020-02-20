package org.jboss.eap.qe.microprofile.config.testapp.jaxrs;

import static org.jboss.eap.qe.microprofile.config.testapp.ResolverConfigSource.PROPERTY_NAME;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.jboss.eap.qe.microprofile.config.testapp.ResolverConfigSource;

/**
 * End point use default Config object, release it, create and use new one and release it again.
 * New default Config should be created again.
 *
 * This is just an example that demonstrate, that releaseConfig call is possible.
 * It is not full use-case (where some synchronization would be necessary, etc.)
 */
@Path("/resolver")
public class ResolverEndPoint {

    @GET
    public String doGet() {
        // prepare response
        StringBuilder response = new StringBuilder();

        // get resolver
        ConfigProviderResolver resolver = ConfigProviderResolver.instance();

        // handle original config
        Config originalConfig = resolver.getConfig();
        response.append(originalConfig.getValue(PROPERTY_NAME, String.class));
        resolver.releaseConfig(originalConfig);

        // prepare, register and release new config
        ConfigBuilder builder = resolver.getBuilder();
        Config specificConfig = builder.addDefaultSources().withSources(new ResolverConfigSource()).build();
        resolver.registerConfig(specificConfig, ResolverEndPoint.class.getClassLoader());
        Config customConfig = resolver.getConfig();
        response.append(customConfig.getValue(PROPERTY_NAME, String.class));
        resolver.releaseConfig(customConfig);

        // get default config
        originalConfig = resolver.getConfig();
        response.append(originalConfig.getValue(PROPERTY_NAME, String.class));

        return response.toString();
    }
}
