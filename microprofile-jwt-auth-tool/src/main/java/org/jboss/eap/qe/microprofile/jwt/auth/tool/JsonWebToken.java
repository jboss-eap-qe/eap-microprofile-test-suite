package org.jboss.eap.qe.microprofile.jwt.auth.tool;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Base64;

/**
 * A class representing JsonWebToken
 */
public class JsonWebToken {

    private final String rawTokenValue;

    private JsonWebToken(Builder builder) {
        this.rawTokenValue = this.composeSignedRawValue(builder.joseHeader, builder.jwtClaims, builder.signature, builder.privateKey);
    }

    /**
     * Generates a base64-encoded signed JWT
     *
     * @return a base64-encoded signed JWT.
     */
    private String composeSignedRawValue(final JoseHeader joseHeader, final JwtClaims jwtClaims, final Signature signature, final PrivateKey privateKey) {
        try {
            final byte[] joseBytes = joseHeader.toJson().toString().getBytes("UTF-8");
            final byte[] claimBytes = jwtClaims.toJson().toString().getBytes("UTF-8");

            final String joseAndClaims = Base64.getUrlEncoder().encodeToString(joseBytes) + "." +
                    Base64.getUrlEncoder().encodeToString(claimBytes);

            signature.initSign(privateKey);
            signature.update(joseAndClaims.getBytes("UTF-8"));

            return joseAndClaims + "." + Base64.getUrlEncoder().encodeToString(signature.sign());
        } catch (final GeneralSecurityException e) {
            throw new IllegalStateException("Could not sign JWT using provided signature.", e);
        } catch (final UnsupportedEncodingException e) {
            throw new IllegalStateException("JVM does not support UTF-8, by spec this can not happen.", e);
        }
    }

    interface JwtJoseHeader {
        JwtJwtClaims joseHeader(JoseHeader joseHeader);
    }

    interface JwtJwtClaims {
        JwtSignature jwtClaims(JwtClaims jwtClaims);
    }

    interface JwtSignature {
        JwtPrivateKey signature(Signature signature);
    }

    interface JwtPrivateKey {
        JwtBuilder privateKey(PrivateKey privateKey);
    }

    interface JwtBuilder {
        JsonWebToken build();
    }

    /**
     * Return raw token value (header.payload.signature)
     * @return Raw token value
     */
    public String getRawValue() {
        return this.rawTokenValue;
    }

    /**
     * A builder for {@link JsonWebToken}
     */
    public static final class Builder implements JwtJoseHeader, JwtJwtClaims, JwtSignature, JwtPrivateKey, JwtBuilder {

        private JoseHeader joseHeader;
        private JwtClaims jwtClaims;
        private Signature signature;
        private PrivateKey privateKey;

        private Builder() {
            //prevent no-argument initialization
        }

        /**
         * Create new instance of JWT builder to utilize fluent API.
         * @return new instance of {@link JwtJoseHeader}.
         */
        public static JwtJoseHeader newInstance() {
            return new Builder();
        }

        /**
         * Token header
         * @param joseHeader token header specifying information about key and algorithm used to sign this token
         * @return instance of {@link JwtJwtClaims}
         */
        public JwtJwtClaims joseHeader(JoseHeader joseHeader) {
            this.joseHeader = joseHeader;
            return this;
        }

        /**
         * Set claims values
         * @param jwtClaims claim values
         * @return instance of {@link JwtSignature}
         */
        public JwtSignature jwtClaims(JwtClaims jwtClaims) {
            this.jwtClaims = jwtClaims;
            return this;
        }

        /**
         * Set signature algorithm implementation which will be used to sign the token
         * @param signature an algorithm implementation - fresh instance with no initialized key or set payload
         * @return instance of {@link JwtPrivateKey}
         */
        public JwtPrivateKey signature(Signature signature) {
            this.signature = signature;
            return this;
        }

        /**
         * Set private key which will be used for signing the JWT
         * @param privateKey private key instance used for signing
         * @return instance of {@link JwtBuilder}
         */
        public JwtBuilder privateKey(PrivateKey privateKey) {
            this.privateKey = privateKey;
            return this;
        }

        /**
         * Build a Java object representing a JWT using values set in this builder
         * @return new instance of {@link JsonWebToken}
         */
        public JsonWebToken build() {
            return new JsonWebToken(this);
        }

    }

}
