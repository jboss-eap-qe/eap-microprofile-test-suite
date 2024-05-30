package org.jboss.eap.qe.micrometer.model.jaeger;

import java.util.List;

/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
