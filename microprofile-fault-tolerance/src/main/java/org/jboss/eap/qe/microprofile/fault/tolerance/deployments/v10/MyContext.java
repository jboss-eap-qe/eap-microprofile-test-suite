package org.jboss.eap.qe.microprofile.fault.tolerance.deployments.v10;

import javax.enterprise.context.RequestScoped;

/**
 * Simple context class injected into MP FT services to check that medhods are invoked in given CDI request scope context
 */
@RequestScoped
public class MyContext {
    private String value = "default";

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        if (value == null || value.isEmpty()) {
            return;
        }

        this.value = value;
    }
}
