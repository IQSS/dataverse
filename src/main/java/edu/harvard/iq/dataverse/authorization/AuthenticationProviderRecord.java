package edu.harvard.iq.dataverse.authorization;

import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;

/**
 * Persistent view of an {@link AuthenticationProvider}. Used by {@link AuthenticationProviderFactory}s 
 * to actually generate the providers.
 * 
 * @author michael
 */
@Entity
public class AuthenticationProviderRecord implements java.io.Serializable {
    
    @Id
    private String alias;
    
    private boolean enabled;
    
    private String factoryName;
    
    @Lob
    private String data;
    
    @Column( length = 256 )
    private String info;

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getFactoryName() {
        return factoryName;
    }

    public void setFactoryName(String factoryName) {
        this.factoryName = factoryName;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 59 * hash + Objects.hashCode(this.alias);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if ( !(obj instanceof AuthenticationProviderRecord) ) {
            return false;
        }
        final AuthenticationProviderRecord other = (AuthenticationProviderRecord) obj;
        return Objects.equals(this.alias, other.alias);
    }

    @Override
    public String toString() {
        return "AuthenticationProviderRecord{" + "alias=" + alias + ", enabled=" + enabled + ", factoryName=" + factoryName + '}';
    }
    
}
