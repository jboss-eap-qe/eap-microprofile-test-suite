package org.jboss.eap.qe.micrometer.model.jaeger;

import java.util.List;

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
