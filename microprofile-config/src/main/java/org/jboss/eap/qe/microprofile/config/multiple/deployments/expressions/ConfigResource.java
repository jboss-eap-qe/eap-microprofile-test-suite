/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.eap.qe.microprofile.config.multiple.deployments.expressions;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.config.Config;

/**
 * A REST resource that exposes an endpoint to retrieve the value of a MicroProfile Config property.
 */
@Path("/config")
@ApplicationScoped
public class ConfigResource {

    @Inject
    Config config;

    @GET
    @Path("/{key}")
    public Response getValue(@PathParam("key") String key) {
        String value = config.getOptionalValue(key, String.class).orElse(null);
        if (value == null) {
            return Response.status(404).build();
        }
        return Response.ok(value).build();
    }
}
