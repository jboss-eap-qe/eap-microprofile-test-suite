package org.jboss.eap.qe.microprofile.jwt.tools;

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
     * Generates a base64-encoded signed JWT that expires after one hour and has the claims "sub" and
     * "preferred_username" set to the provided subject string.
     *
     * @return a base64-encoded signed JWT token.
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
    public static final class Builder {

        private JoseHeader joseHeader;
        private JwtClaims jwtClaims;
        private Signature signature;
        private PrivateKey privateKey;

        /**
         * Token header
         * @param joseHeader token header specifying information about key and algorithm used to sign this token
         * @return instance of this builder
         */
        public Builder joseHeader(JoseHeader joseHeader) {
            this.joseHeader = joseHeader;
            return this;
        }

        /**
         * Set claims values
         * @param jwtClaims claim values
         * @return instance of this builder
         */
        public Builder jwtClaims(JwtClaims jwtClaims) {
            this.jwtClaims = jwtClaims;
            return this;
        }

        /**
         * Set signature algorithm implementation which will be used to sign the token
         * @param signature an algorithm implementation - fresh instance with no initialized key or set payload
         * @return instance of this builder
         */
        public Builder signature(Signature signature) {
            this.signature = signature;
            return this;
        }

        /**
         * Set private key which will be used for signing the JWT
         * @param privateKey private key instance used for signing
         * @return instance of this builder
         */
        public Builder privateKey(PrivateKey privateKey) {
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
