package edu.harvard.iq.dataverse.authorization.providers;

import edu.harvard.iq.dataverse.authorization.AuthenticationProvider;
import java.util.Objects;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

/**
 * Database-storable form of an {@code AuthenticationProvider}.
 * An instance of this class is translated to an actual {@link AuthenticationProvider}
 * instance by the {@link AuthenticationProviderFactory} whose alias appears in 
 * {@link #factoryAlias}.
 * 
 * @author michael
 */
@NamedQueries({
    @NamedQuery( name="AuthenticationProviderRow.findAllEnabled",
                 query="SELECT r FROM AuthenticationProviderRow r WHERE r.enabled=true" ),
    @NamedQuery( name="AuthenticationProviderRow.findById",
                 query="SELECT r FROM AuthenticationProviderRow r WHERE r.id=:id" ),
    @NamedQuery( name="AuthenticationProviderRow.findAll",
                 query="SELECT r FROM AuthenticationProviderRow r" )
})
@Entity
@Table(indexes = {@Index(columnList="enabled")})
public class AuthenticationProviderRow implements java.io.Serializable {
    
    @Id
    private String id;

    /**
     * @todo Consider dropping this column since we override title in order to
     * internationalize it. Or add a translatableTitle field?
     */
    private String title;
    
    private String subtitle;
    
    private String factoryAlias;
    
    private boolean enabled;
    
    @Lob
    private String factoryData;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getFactoryAlias() {
        return factoryAlias;
    }

    public void setFactoryAlias(String factoryAlias) {
        this.factoryAlias = factoryAlias;
    }

    public String getFactoryData() {
        return factoryData;
    }

    public void setFactoryData(String factoryData) {
        this.factoryData = factoryData;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + Objects.hashCode(this.id);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if ( !(obj instanceof AuthenticationProviderRow)) {
            return false;
        }
        final AuthenticationProviderRow other = (AuthenticationProviderRow) obj;
        return Objects.equals(this.id, other.id);
    }
    
    
}
