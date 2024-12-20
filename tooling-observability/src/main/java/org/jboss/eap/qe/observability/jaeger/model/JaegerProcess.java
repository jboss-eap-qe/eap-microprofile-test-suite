/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.eap.qe.observability.jaeger.model;

import java.util.List;

/**
 * Copied from org.wildfly.test.integration.observability.opentelemetry.jaeger
 */
public class JaegerProcess {
    private String serviceName;
    private List<JaegerTag> tags;

    public String getServiceName() {
        return serviceName;
    }

    public JaegerProcess setServiceName(String serviceName) {
        this.serviceName = serviceName;
        return this;
    }

    public List<JaegerTag> getTags() {
        return tags;
    }

    public JaegerProcess setTags(List<JaegerTag> tags) {
        this.tags = tags;
        return this;
    }
}
