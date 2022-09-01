package org.jboss.eap.qe.microprofile.config.testapp.jaxrs;

import static org.jboss.eap.qe.microprofile.config.testapp.ResolverConfigSource.PROPERTY_NAME;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.eap.qe.microprofile.config.testapp.ResolverConfigSource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

/**
 * This EndPoint class tests ConfigProviderResolver#registerConfig and ConfigProviderResolver#releaseConfig methods
 */
@Path("/resolver")
public class ResolverEndPoint {

    public final static String ILLEGAL_STATE_EXCEPTION_ERROR_MESSAGE = "Expected IllegalStateException was not thrown";

    /**
     * End point use default Config object, release it, create and use new one and release it again.
     * New default Config should be created again.
     *
     * This is just an example that demonstrate, that releaseConfig call is possible.
     * It is not full use-case (where some synchronization would be necessary, etc.)
     */
    @GET
    @Path("/oneConfigSource")
    public String configSourceInOneConfig() {
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
        try {
            Config customConfig = resolver.getConfig();
            response.append(customConfig.getValue(PROPERTY_NAME, String.class));
        } finally {
            // we need to make sure that the newly created config is cleaned up when we're done with it
            resolver.releaseConfig(specificConfig);
        }

        // get default config
        originalConfig = resolver.getConfig();
        response.append(originalConfig.getValue(PROPERTY_NAME, String.class));

        return response.toString();
    }

    /**
     * This end point checks behaviour of two config
     *
     * This behaviour may be changed in future spec release. See https://github.com/eclipse/microprofile-config/issues/522
     */
    @GET
    @Path("/twoConfigSources")
    public String configSourceInTwoConfig() {

        // prepare response
        StringBuilder response = new StringBuilder();

        // get resolver
        ConfigProviderResolver resolver = ConfigProviderResolver.instance();

        // handle original config
        Config originalConfig = resolver.getConfig();
        resolver.releaseConfig(originalConfig);

        // prepare two configs
        ConfigBuilder builder = resolver.getBuilder();
        ConfigSource customConfigSource = new ResolverConfigSource();
        Config specificConfig1 = builder.withSources(customConfigSource).build();
        Config specificConfig2 = builder.withSources(customConfigSource).build();

        // register first config
        resolver.registerConfig(specificConfig1, ResolverEndPoint.class.getClassLoader());
        // try to register second config, exception is expected
        try {
            resolver.registerConfig(specificConfig2, ResolverEndPoint.class.getClassLoader());
            return ILLEGAL_STATE_EXCEPTION_ERROR_MESSAGE;
        } catch (IllegalStateException ise) {
            if (!ise.getMessage().equals("SRCFG00017: Configuration already registered for the given class loader")) {
                return ILLEGAL_STATE_EXCEPTION_ERROR_MESSAGE;
            }
            // get value from first config
            Config customConfig1 = resolver.getConfig();
            response.append(customConfig1.getValue(PROPERTY_NAME, String.class));
        } finally {
            // but anyway we need to make sure that the newly created config is cleaned up when we're done with it
            resolver.releaseConfig(specificConfig1);
        }

        // get value from second config
        response.append(specificConfig2.getValue(PROPERTY_NAME, String.class));

        // return response
        return response.toString();
    }
}
