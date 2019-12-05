package org.jboss.eap.qe.microprofile.openapi.apps.routing.provider.model;

/**
 * Defines the contract for implementing a district entity
 */
public interface District {

    /**
     * Provide read access to the string that uniquely identifies the district
     *
     * @return String that uniquely identifies the district
     */
    String getCode();

    /**
     * Provides write access to the String value that uniquely identifies the district
     *
     * @param code String to set the value that uniquely identifies the district
     */
    void setCode(String code);

    /**
     * Provide read access to the string that represents the district name
     *
     * @return String that represents the district name
     */
    String getName();

    /**
     * Provides write access to the String value that represents the district name
     *
     * @param name String value to set the district name
     */
    void setName(String name);

    /**
     * Provide read access to the boolean value that allows tell whether a district has been marked as obsolete
     *
     * @return True if the district has been marked as obsolete, False otherwise
     */
    Boolean getObsolete();

    /**
     * Provides write access to the boolean value that allows to mark a district as obsolete
     *
     * @param isObsolete True if the district has to be marked as obsolete, False otherwise
     */
    void setObsolete(Boolean isObsolete);
}
