package edu.harvard.iq.dataverse;

import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * @author Jing Ma
 */
 @NamedQueries({
    @NamedQuery( name="License.findAll",
                query="SELECT l FROM License l"),
    @NamedQuery( name="License.findById",
            query = "SELECT l FROM License l WHERE l.id=:id"),
    @NamedQuery( name="License.deleteById",
                query="DELETE FROM License l WHERE l.id=:id")

})
@Entity
@Table(uniqueConstraints = {
      @UniqueConstraint(columnNames = "name"),
      @UniqueConstraint(columnNames = "uri")}
)
public class License {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition="TEXT", nullable = false)
    private String name;

    @Column(columnDefinition="TEXT")
    private String shortDescription;

    @Column(columnDefinition="TEXT", nullable = false)
    private String uri;

    @Column(columnDefinition="TEXT")
    private String iconUrl;

    @Column(nullable = false)
    private boolean active;

    public License() {
    }

    public License(String name, String shortDescription, String uri, String iconUrl, boolean active) {
        this.name = name;
        this.shortDescription = shortDescription;
        this.uri = uri;
        this.iconUrl = iconUrl;
        this.active = active;
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

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        License license = (License) o;
        return active == license.active &&
                Objects.equals(id, license.id) &&
                Objects.equals(name, license.name) &&
                Objects.equals(shortDescription, license.shortDescription) &&
                Objects.equals(uri, license.uri) &&
                Objects.equals(iconUrl, license.iconUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, shortDescription, uri, iconUrl, active);
    }

    @Override
    public String toString() {
        return "License{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", shortDescription='" + shortDescription + '\'' +
                ", uri='" + uri + '\'' +
                ", iconUrl='" + iconUrl + '\'' +
                ", active=" + active +
                '}';
    }
    
}
