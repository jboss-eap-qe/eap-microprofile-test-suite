package org.jboss.eap.qe.microprofile.openapi.apps.routing.router.model.another;

import java.io.Serializable;

/**
 * Represents <i>another</i>, intentionally different from
 * {@link org.jboss.eap.qe.microprofile.openapi.apps.routing.router.model.DistrictObject}, district entity.
 *
 * This is to verify what would happen when two separate - and conflicting in terms of simple name classes - POJOs are used
 * in OpenAPI annotations, e.g.: {@link org.eclipse.microprofile.openapi.annotations.parameters.RequestBodySchema}).
 *
 * If two Local Service Router applications are deployed to the same server/virtual host, they would contribute to
 * the same OpenAPI documentation, and classes like {@link DistrictObject} and
 * {@link org.jboss.eap.qe.microprofile.openapi.apps.routing.router.model.DistrictObject} should be documented
 * by the global {@code components} property.
 */
public class DistrictObject implements Serializable {

    private String code;
    private String name;
    private Boolean markedForRemoval;

    public DistrictObject() {
        this(null, null, Boolean.FALSE);
    }

    public DistrictObject(String code, String name) {
        this(code, name, Boolean.FALSE);

    }

    public DistrictObject(final String code, final String name, final Boolean markedForRemoval) {
        this.code = code;
        this.name = name;
        this.markedForRemoval = markedForRemoval;
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
     * Provide read access to the boolean value that allows tell whether a district has been marked for removal
     *
     * @return True if the district has been marked for removal, False otherwise
     */
    public Boolean getMarkedForRemoval() {
        return markedForRemoval;
    }

    /**
     * Provides write access to the boolean value that allows to mark a district for removal
     *
     * @param isMarkedForRemoval True if the district has to be marked for removal, False otherwise
     */
    public void setMarkedForRemoval(final Boolean isMarkedForRemoval) {
        this.markedForRemoval = isMarkedForRemoval;
    }
}
