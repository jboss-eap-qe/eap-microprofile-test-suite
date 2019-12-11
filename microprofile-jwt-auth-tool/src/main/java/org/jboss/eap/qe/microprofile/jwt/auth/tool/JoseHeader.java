package org.jboss.eap.qe.microprofile.jwt.auth.tool;

import javax.json.Json;
import javax.json.JsonObject;

/**
 * A class representing a header of JWT
 */
public class JoseHeader {

    private final String keyId;
    private final String type;
    private final String algorithm;

    /**
     * Create a new instance of {@link JoseHeader} using provided values.
     * @param keyId ID of key which identifies key used to verify the signature. This ID is used by server to select
     *              correct key among several.
     * @param type Identifies the token as an RFC7519 and must be "JWT" RFC7519, Section 5.1 to satisfy the spec.
     * @param algorithm Identifies the cryptographic algorithm used to secure the JWT. E.g. RS256
     */
    public JoseHeader(final String keyId, final String type, final String algorithm) {
        this.keyId = keyId;
        this.type = type;
        this.algorithm = algorithm;
    }

    /**
     * Shortcut for creating valid instance with custom key ID. Useful when you just need a header with valid values.
     * @param keyId ID of key which will be propagated in this header.
     * @return new instance of valid header
     */
    public JoseHeader(final String keyId) {
        this(keyId, "JWT", "RS256");
    }

    /**
     * Format values to JSON
     * @return JSON representation
     */
    public JsonObject toJson() {
        return Json.createObjectBuilder()
                .add("kid", this.keyId)
                .add("typ", this.type)
                .add("alg", this.algorithm)
                .build();
    }

}
