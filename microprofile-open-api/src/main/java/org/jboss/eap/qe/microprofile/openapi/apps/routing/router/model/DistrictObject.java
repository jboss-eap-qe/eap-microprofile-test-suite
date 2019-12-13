package org.jboss.eap.qe.microprofile.openapi.apps.routing.router.model;

import java.io.Serializable;

/**
 * Represents a district entity as it is implemented by Local Service Router app - i.e. a POJO used to produce expected
 * JSON data to Services Provider endpoints or to represent incoming JSON data for exposed mirrored services.
 *
 * This is to show that the Local Service provider could be implementing mirrored services without having the District
 * interface available, while providing a compliant POJO, in fact Local Service Router could be written using different
 * languages/tools. So this class is not - by design - implementing District interface.
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

    /**
     * Provide read access to the string that uniquely identifies the district
     *
     * @return String that uniquely identifies the district
     */
    public String getCode() {
        return code;
    }

    /**
     * Provide read access to the string that represents the district name
     *
     * @return String that represents the district name
     */
    public String getName() {
        return name;
    }

    /**
     * Provides write access to the String value that represents the district name
     *
     * @param name String value to set the district name
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Provides write access to the String value that uniquely identifies the district
     *
     * @param code String to set the value that uniquely identifies the district
     */
    public void setCode(final String code) {
        this.code = code;
    }

    /**
     * Provide read access to the boolean value that allows tell whether a district has been marked as obsolete
     *
     * @return True if the district has been marked as obsolete, False otherwise
     */
    public Boolean getObsolete() {
        return obsolete;
    }

    /**
     * Provides write access to the boolean value that allows to mark a district as obsolete
     *
     * @param isObsolete True if the district has to be marked as obsolete, False otherwise
     */
    public void setObsolete(final Boolean isObsolete) {
        this.obsolete = isObsolete;
    }
}
