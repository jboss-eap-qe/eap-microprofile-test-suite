package org.jboss.eap.qe.microprofile.openapi.apps.routing.router.model;

public class PojoExample {
    private String code;
    private String name;

    public PojoExample() {
        this(null, null);
    }

    public PojoExample(String code, String name) {
        this.code = code;
        this.name = name;

    }

    /**
     * Provide read access to the string that uniquely identifies the instance
     *
     * @return String that uniquely identifies the instance
     */
    public String getCode() {
        return code;
    }

    /**
     * Provide read access to the string that represents the instance name
     *
     * @return String that represents the instance name
     */
    public String getName() {
        return name;
    }

    /**
     * Provides write access to the String value that represents the instance name
     *
     * @param name String value to set the instance name
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Provides write access to the String value that uniquely identifies the instance
     *
     * @param code String to set the value that uniquely identifies the instance
     */
    public void setCode(final String code) {
        this.code = code;
    }
}
