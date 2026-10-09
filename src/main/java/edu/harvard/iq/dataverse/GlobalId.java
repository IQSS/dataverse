/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.util.BundleUtil;
import jakarta.json.bind.annotation.JsonbCreator;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.validation.constraints.NotBlank;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static edu.harvard.iq.dataverse.util.StringUtil.isEmpty;

/**
 * Represents a globally unique identifier composed of a protocol, authority, identifier, optional separator,
 * URL prefix, and managing provider identifier.
 * <p>
 * Instances of this class can be used to identify resources in a persistent, globally resolvable way.
 * A complete identifier contains non-empty protocol, authority, and identifier parts.
 * The class also supports conversion to different textual and URL representations for integration with identifier
 * systems, registries, and external services.
 * <p>
 * It is thread-safe and immutable, thus suitable for concurrent use, sharing across threads and keys in maps.
 * When used with Jakarta JSON-B, it can be serialized and deserialized efficiently.
 */
@Schema(
    name = "GlobalId",
    description = "Persistent identifier composed of a protocol, authority and local identifier."
)
public class GlobalId implements java.io.Serializable {
    
    private static final Logger logger = Logger.getLogger(GlobalId.class.getName());

    @JsonbCreator
    public GlobalId(@JsonbProperty("protocol") String protocol,
                    @JsonbProperty("authority") String authority,
                    @JsonbProperty("identifier") String identifier,
                    @JsonbProperty("separator") String separator,
                    @JsonbProperty("urlPrefix") String urlPrefix,
                    @JsonbProperty("providerId") String providerName) {
        this.protocol = protocol;
        this.authority = authority;
        this.identifier = identifier;
        if(separator!=null) {
          this.separator = separator;
        }
        this.urlPrefix = urlPrefix;
        this.managingProviderId = providerName;
    }
    
    // protocol the identifier system, e.g. "doi"
    // authority the namespace that the authority manages in the identifier system
    // identifier the local identifier part
    private String protocol;
    private String authority;
    private String identifier;
    private String managingProviderId;
    private String separator = "/";
    private String urlPrefix;

    /**
     * Tests whether {@code this} instance has all the data required for a 
     * global id.
     * @return {@code true} iff all the fields are non-empty; {@code false} otherwise.
     */
    @JsonbTransient
    @Schema(hidden = true)
    public boolean isComplete() {
        return !(isEmpty(protocol)||isEmpty(authority)||isEmpty(identifier));
    }
    
    @NotBlank
    @Schema(
        description = "Identifier protocol.",
        example = "doi",
        required = true
    )
    public String getProtocol() {
        return protocol;
    }
    
    @NotBlank
    @Schema(
        description = "Authority namespace within the identifier system.",
        example = "10.12345",
        required = true
    )
    public String getAuthority() {
        return authority;
    }
    
    @Schema(
        description = "Separator between the authority and local identifier; defaults to '/' when omitted or null.",
        example = "/",
        defaultValue = "/"
    )
    public String getSeparator() {
        return separator;
    }
    
    @NotBlank
    @Schema(
        description = "Local identifier within the authority namespace.",
        example = "ABC123",
        required = true
    )
    public String getIdentifier() {
        return identifier;
    }
    
    @Schema(
        description = "Identifier of the managing persistent-identifier provider.",
        nullable = true
    )
    public String getProviderId() {
        return managingProviderId;
    }
    
    @Schema(
        description = "Resolver URL prefix used to construct the identifier URL.",
        example = "https://doi.org/",
        nullable = true
    )
    public String getUrlPrefix() {
        return urlPrefix;
    }

    public String toString() {
        return asString();
    }
    
    /**
     * Concatenate the parts that make up a Global Identifier.
     * 
     * @return the Global Identifier, e.g. "doi:10.12345/67890"
     */
    public String asString() {
        if (protocol == null || authority == null || identifier == null) {
            return "";
        }
        return protocol + ":" + authority + separator + identifier;
    }
    
    public String asURL() {
        URL url = null;
        if (identifier == null){
            return null;
        }
        try {
               url = new URL(urlPrefix + authority + separator + identifier);
               return url.toExternalForm();
        } catch (MalformedURLException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    public String asRawIdentifier() {
        if (protocol == null || authority == null || identifier == null) {
            return "";
        }
        return authority + separator + identifier;
    }

    /**
     * Verifies that the pid only contains allowed characters.
     *
     * @param pidParam
     * @return true if pid only contains allowed characters false if pid
     * contains characters not specified in the allowed characters regex.
     */
    public static boolean verifyImportCharacters(String pidParam) {

        Pattern p = Pattern.compile(BundleUtil.getStringFromBundle("pid.allowedCharacters"));
        Matcher m = p.matcher(pidParam);

        return m.matches();
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(protocol, authority, identifier, managingProviderId);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof GlobalId that)) return false;
        return Objects.equals(this.protocol, that.protocol) &&
               Objects.equals(this.authority, that.authority) &&
               Objects.equals(this.identifier, that.identifier) &&
               Objects.equals(this.managingProviderId, that.managingProviderId);
    }
}
