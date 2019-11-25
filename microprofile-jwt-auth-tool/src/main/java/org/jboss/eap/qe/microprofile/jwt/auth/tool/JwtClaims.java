package org.jboss.eap.qe.microprofile.jwt.auth.tool;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * JWT claims as described in specification. See https://github.com/eclipse/microprofile-jwt-auth/releases
 */
public class JwtClaims {

    private String issuer;
    private String subject;
    private Long expirationTime;
    private Long issuedAtTime;
    private String jwtId;
    private String userPrincipalName;
    private Set<String> groups;
    private String audience;

    private final Map<String, Object> customClaims;


    private JwtClaims(Builder builder) {
        this.issuer = builder.issuer;
        this.subject = builder.subject;
        this.expirationTime = builder.expirationTime;
        this.issuedAtTime = builder.issuedAtTime;
        this.jwtId = builder.jwtId;
        this.userPrincipalName = builder.userPrincipalName;
        this.groups = builder.groups;
        this.audience = builder.audience;
        this.customClaims = builder.customClaims;
    }

    public JsonObject toJson() {
        final JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder()
                .add("jti", this.jwtId)
                .add("sub", this.subject)
                .add("groups", Json.createArrayBuilder(this.groups))
                .add("aud", this.audience)
                .add("iss", this.issuer)
                .add("iat", this.issuedAtTime)
                .add("exp", this.expirationTime)
                .add("upn", this.userPrincipalName);

        for (Map.Entry<String, Object> entry : this.customClaims.entrySet()) {
            jsonObjectBuilder.add(entry.getKey(), entry.getValue().toString());
        }

        return jsonObjectBuilder.build();
    }

    public static final class Builder {

        private String issuer;
        private String subject;
        private Long expirationTime;
        private Long issuedAtTime;
        private String jwtId = UUID.randomUUID().toString();
        private String userPrincipalName;
        private Set<String> groups;
        private String audience;

        private Map<String, Object> customClaims;

        public Builder issuer(String issuer) {
            this.issuer = issuer;
            return this;
        }

        public Builder subject(String subject) {
            this.subject = subject;
            return this;
        }

        public Builder expirationTime(Long expirationTime) {
            this.expirationTime = expirationTime;
            return this;
        }

        public Builder issuedAtTime(Long issuedAtTime) {
            this.issuedAtTime = issuedAtTime;
            return this;
        }

        public Builder jwtId(String jwtId) {
            this.jwtId = jwtId;
            return this;
        }

        public Builder userPrincipalName(String userPrincipalName) {
            this.userPrincipalName = userPrincipalName;
            return this;
        }

        public Builder groups(Set<String> groups) {
            this.groups = groups;
            return this;
        }

        public Builder audience(String audience) {
            this.audience = audience;
            return this;
        }

        public Builder customClaim(final String name, final Object value) {
            if (this.customClaims == null) {
                this.customClaims = new HashMap<>();
            }
            this.customClaims.put(name, value);
            return this;
        }

        public JwtClaims build() {
            return new JwtClaims(this);
        }
    }

}
