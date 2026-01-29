package edu.harvard.iq.dataverse.license;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import edu.harvard.iq.dataverse.TermsOfUseAndAccess;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;

/**
 * @author Jing Ma
 */
 @NamedQueries({
    @NamedQuery( name="License.findAll",
            query="SELECT l FROM License l ORDER BY (case when l.isDefault then 0 else 1 end), l.sortOrder, l.id asc"),
    @NamedQuery( name="License.findAllActive",
            query="SELECT l FROM License l WHERE l.active='true' ORDER BY (case when l.isDefault then 0 else 1 end), l.sortOrder, l.id asc"),
    @NamedQuery( name="License.findById",
            query = "SELECT l FROM License l WHERE l.id=:id"),
    @NamedQuery( name="License.findDefault",
            query = "SELECT l FROM License l WHERE l.isDefault='true' "),
    @NamedQuery( name="License.findActiveByNameOrUri",
            query = "SELECT l FROM License l WHERE l.name=:name AND l.active='true' OR l.uri=:uri AND l.active='true'"),
    @NamedQuery( name="License.deleteById",
            query = "DELETE FROM License l WHERE l.id=:id"),
    @NamedQuery( name="License.deleteByName",
            query = "DELETE FROM License l WHERE l.name=:name"),
    @NamedQuery( name="License.setDefault",
            query = "UPDATE License l SET l.isDefault='true' WHERE l.id=:id"),
    @NamedQuery( name="License.clearDefault",
                query = "UPDATE License l SET l.isDefault='false'"),
    @NamedQuery( name="License.setActiveState",
    query = "UPDATE License l SET l.active=:state WHERE l.id=:id"),
    @NamedQuery( name="License.setSortOrder",
    query = "UPDATE License l SET l.sortOrder=:sortOrder WHERE l.id=:id"),

})
@Entity
@Table(uniqueConstraints = {
      @UniqueConstraint(columnNames = "name"),
      @UniqueConstraint(columnNames = "uri")}
)
public class License {
    public static String CC0 = "http://creativecommons.org/publicdomain/zero/1.0";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = false, unique = true)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String shortDescription;

    @Column(columnDefinition = "TEXT", nullable = false, unique = true)
    private String uri;

    @Column(columnDefinition = "TEXT")
    private String iconUrl;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private boolean isDefault;

    @Column(nullable = false, columnDefinition = "BIGINT NOT NULL DEFAULT 0")
    private Long sortOrder;

    @Column(name = "rights_identifier")
    private String rightsIdentifier;

    @Column(name = "rights_identifier_scheme")
    private String rightsIdentifierScheme;

    @Column(name = "scheme_uri")
    private String schemeUri;
    
    @Column(name = "language_code")
    private String languageCode;

    @OneToMany(mappedBy = "license")
    private List<TermsOfUseAndAccess> termsOfUseAndAccess;

    public License() {
    }

    public License(String name, String shortDescription, URI uri, URI iconUrl, boolean active, Long sortOrder) {
        this.name = name;
        this.shortDescription = shortDescription;
        this.uri = uri.toASCIIString();
        if (iconUrl != null) {
            this.iconUrl = iconUrl.toASCIIString();
        } else {
            this.iconUrl = null;
        }
        this.active = active;
        isDefault = false;
        this.sortOrder = sortOrder;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public URI getUri() {
        try {
            return new URI(uri);
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Incorrect URI in JSON");
        }
    }

    public void setUri(URI uri) {
        this.uri = uri.toASCIIString();
    }

    public URI getIconUrl() {
        if (iconUrl == null) {
            return null;
        }
        try {
            return new URI(iconUrl);
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Incorrect URI in JSON");
        }
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public void setIconUrl(URI iconUrl) {
        if (iconUrl != null) {
            this.iconUrl = iconUrl.toASCIIString();
        } else {
            this.iconUrl = null;
        }
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    public List<TermsOfUseAndAccess> getTermsOfUseAndAccess() {
        return termsOfUseAndAccess;
    }

    public void setTermsOfUseAndAccess(List<TermsOfUseAndAccess> termsOfUseAndAccess) {
        this.termsOfUseAndAccess = termsOfUseAndAccess;
    }

    public Long getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Long sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getRightsIdentifier() {
        return rightsIdentifier;
    }

    public void setRightsIdentifier(String rightsIdentifier) {
        this.rightsIdentifier = rightsIdentifier;
    }

    public String getRightsIdentifierScheme() {
        return rightsIdentifierScheme;
    }

    public void setRightsIdentifierScheme(String rightsIdentifierScheme) {
        this.rightsIdentifierScheme = rightsIdentifierScheme;
    }

    public String getSchemeUri() {
        return schemeUri;
    }

    public void setSchemeUri(String schemeUri) {
        this.schemeUri = schemeUri;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        License license = (License) o;
        return active == license.active && id.equals(license.id) && name.equals(license.name)
                && shortDescription.equals(license.shortDescription) && uri.equals(license.uri)
                && Objects.equals(iconUrl, license.iconUrl) && Objects.equals(sortOrder, license.sortOrder)
                && Objects.equals(rightsIdentifier, license.rightsIdentifier)
                && Objects.equals(rightsIdentifierScheme, license.rightsIdentifierScheme)
                && Objects.equals(schemeUri, license.schemeUri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, shortDescription, uri, iconUrl, active, sortOrder, rightsIdentifier,
                rightsIdentifierScheme, schemeUri);
    }

    @Override
    public String toString() {
        return "License{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", shortDescription='" + shortDescription + '\'' +
                ", uri=" + uri +
                ", iconUrl=" + iconUrl +
                ", active=" + active +
                ", isDefault=" + isDefault +
                ", sortOrder=" + sortOrder +
                ", rightsIdentifier='" + rightsIdentifier + '\'' +
                ", rightsIdentifierScheme='" + rightsIdentifierScheme + '\'' +
                ", schemeUri='" + schemeUri + '\'' +
                '}';
    }

}
