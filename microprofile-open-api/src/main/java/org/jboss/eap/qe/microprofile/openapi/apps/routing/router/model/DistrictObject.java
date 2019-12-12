package org.jboss.eap.qe.microprofile.openapi.apps.routing.router.model;

import java.io.Serializable;

/**
 * Represents a concrete entity
 */
public class DistrictObject implements Serializable {

    private String code;
    private String name;
    private Boolean obsolete;

    public DistrictObject() {
        this(null, null, Boolean.FALSE);
    }

    public DistrictObject(String code, String name) {
        this(code, name, Boolean.FALSE);

    }

    public DistrictObject(final String code, final String name, final Boolean obsolete) {
        this.code = code;
        this.name = name;
        this.obsolete = obsolete;
    }

    public Boolean getObsolete() {
        return obsolete;
    }

    public void setObsolete(final Boolean isObsolete) {
        this.obsolete = isObsolete;
    }

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }
}
