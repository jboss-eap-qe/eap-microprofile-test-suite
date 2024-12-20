/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.eap.qe.observability.jaeger.model;

import java.util.List;

/**
 * Copied from org.wildfly.test.integration.observability.opentelemetry.jaeger
 */
public class JaegerResponse {
    private List<JaegerTrace> data;
    private String errors;

    public List<JaegerTrace> getData() {
        return data;
    }

    public JaegerResponse setData(List<JaegerTrace> data) {
        this.data = data;
        return this;
    }

    public String getErrors() {
        return errors;
    }

    public JaegerResponse setErrors(String errors) {
        this.errors = errors;
        return this;
    }
}
