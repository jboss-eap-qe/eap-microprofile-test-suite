package org.jboss.eap.qe.microprofile.jwt.security.keyselection;

import jakarta.json.JsonObject;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.jboss.eap.qe.microprofile.jwt.testapp.jaxrs.JaxRsTestApplication;
import org.jboss.eap.qe.microprofile.jwt.testapp.jaxrs.SecuredJaxRsEndpoint;
import org.jboss.eap.qe.microprofile.tooling.server.configuration.deployment.ConfigurationUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 * Deployment builder for org.jboss.eap.qe.microprofile.jwt.security.keyselection package tests. It can handle JWKS key
 * encoding and publickey properties configuration.
 */
final class DeploymentBuilder {

    private static final String MP_PUBLIC_KEY_PROPERTY = "mp.jwt.verify.publickey";
    private static final String MP_PUBLIC_KEY_PROPERTY_LOCATION = "mp.jwt.verify.publickey.location";
    private static final String MP_ISSUER_PROPERTY = "mp.jwt.verify.issuer";

    private final String name;

    private JsonObject jwksObject;
    private boolean base64encodedJwks;
    private boolean passAsInlineValue;

    public DeploymentBuilder(final String name) {
        this.name = name;
    }

    public DeploymentBuilder passAsInlineValue(boolean passAsInlineValue) {
        this.passAsInlineValue = passAsInlineValue;
        return this;
    }

    public DeploymentBuilder jwksObject(final JsonObject jwksObject) {
        this.jwksObject = jwksObject;
        return this;
    }

    public DeploymentBuilder base64encodedJwks(boolean base64encodedJwks) {
        this.base64encodedJwks = base64encodedJwks;
        return this;
    }

    private void validate() {
        if (jwksObject == null) {
            throw new IllegalArgumentException("JWKS must be set!");
        }
    }

    public WebArchive build() {
        validate();

        final StringBuilder stringBuilder = new StringBuilder();

        String jwksString;
        if (this.base64encodedJwks) {
            jwksString = Base64.getUrlEncoder().encodeToString(this.jwksObject.toString().getBytes(StandardCharsets.UTF_8));
        } else {
            jwksString = this.jwksObject.toString();
        }

        final WebArchive archive = ShrinkWrap.create(WebArchive.class, this.name)
                .addClass(SecuredJaxRsEndpoint.class)
                .addClass(JaxRsTestApplication.class);
        archive.addAsWebInfResource(ConfigurationUtil.BEANS_XML_FILE_LOCATION, "beans.xml");

        if (this.passAsInlineValue) {
            stringBuilder.append(MP_PUBLIC_KEY_PROPERTY).append(":").append(jwksString).append(String.format("%n"));
        } else {
            stringBuilder.append(MP_PUBLIC_KEY_PROPERTY_LOCATION).append(":").append("META-INF/keys.jwks")
                    .append(String.format("%n"));
            archive.addAsManifestResource(new StringAsset(jwksString), "keys.jwks");
        }

        stringBuilder.append(MP_ISSUER_PROPERTY).append(":issuer");

        return archive.addAsManifestResource(new StringAsset(stringBuilder.toString()), "microprofile-config.properties");
    }
}
