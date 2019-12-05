package org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.data;

import java.io.Serializable;

import org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.model.District;

/**
 * Represents a concrete {@link District} entity
 */
public class DistrictEntity implements District, Serializable {

    private String code;
    private String name;
    private Boolean obsolete;

    public DistrictEntity() {
        this(null, null, Boolean.FALSE);
    }

    public DistrictEntity(String code, String name) {
        this(code, name, Boolean.FALSE);

    }

    public DistrictEntity(final String code, final String name, final Boolean obsolete) {
        this.code = code;
        this.name = name;
        this.obsolete = obsolete;
    }

    @Override
    public Boolean getObsolete() {
        return obsolete;
    }

    @Override
    public void setObsolete(final Boolean isObsolete) {
        this.obsolete = isObsolete;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public void setCode(final String code) {
        this.code = code;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(final String name) {
        this.name = name;
    }
}
