package org.jboss.eap.qe.microprofile.jwt.tools;

import javax.json.Json;
import javax.json.JsonObject;

/**
 * A class representing a header of JWT
 */
public class JoseHeader {

    private final String keyId;
    private final String type;
    private final String algorithm;

    private JoseHeader(Builder builder) {
        this.keyId = builder.keyId;
        this.type = builder.type;
        this.algorithm = builder.algorithm;
    }

    /**
     * Shortcut for creating valid instance with custom key ID. Useful when you just need a header with valid values.
     * @param keyId ID of key which will be propagated in this header.
     * @return new instance of valid header
     */
    public static JoseHeader validWithKeyId(final String keyId) {
        return new JoseHeader.Builder()
                .keyId(keyId)
                .algorithm("RS256")
                .type("JWT")
                .build();
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

    public static final class Builder {
        private String keyId;
        private String type;
        private String algorithm;

        /**
         * Set ID of key which identifies key used to verify the signature. This ID is used by server to select correct
         * key among several.
         * @param keyId Key ID
         * @return instance of this builder
         */
        public Builder keyId(String keyId) {
            this.keyId = keyId;
            return this;
        }

        /**
         * Identifies the token as an RFC7519 and must be "JWT" RFC7519, Section 5.1 to satisfy the spec.
         * @param type a type
         * @return instance of this builder
         */
        public Builder type(String type) {
            this.type = type;
            return this;
        }

        /**
         * Identifies the cryptographic algorithm used to secure the JWT.
         * @param algorithm Cryptographic algorithm - e.g. RS256
         * @return instance of this builder
         */
        public Builder algorithm(String algorithm) {
            this.algorithm = algorithm;
            return this;
        }

        /**
         * Create a new instance of {@link JoseHeader} using values in this builder.
         * @return new instance of {@link JoseHeader}
         */
        public JoseHeader build() {
            return new JoseHeader(this);
        }
    }

}
