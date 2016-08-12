package edu.harvard.iq.dataverse.authorization.groups.impl;

import edu.harvard.iq.dataverse.authorization.groups.Group;
import edu.harvard.iq.dataverse.authorization.RoleAssigneeDisplayInfo;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * Convenience base class for implementing groups that apply to the entire Dataverse
 * installation, and are persisted to the DB.
 * 
 * @author michael
 */
@NamedQueries({
    @NamedQuery( name="PersistedGlobalGroup.persistedGroupAlias",
                query="SELECT g FROM PersistedGlobalGroup g WHERE g.persistedGroupAlias=:persistedGroupAlias" )
})
@Entity
@Table(indexes = {@Index(columnList="dtype")})
public abstract class PersistedGlobalGroup implements Group, Serializable, Comparable {
    
    @Id
    @GeneratedValue
    private Long id;
    
    /**
     * A unique alias within the persisted group table. 
     */
    @Column(unique = true)
    private String persistedGroupAlias;
    private String displayName;
    private String description;

    public void setPersistedGroupAlias(String alias) {
        this.persistedGroupAlias = alias;
    }

    public String getPersistedGroupAlias() {
        return persistedGroupAlias;
    }
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    
    @Override
    public String getAlias() {
        return getGroupProvider().getGroupProviderAlias() + Group.PATH_SEPARATOR + persistedGroupAlias;
    }

    public void setDisplayName(String name) {
        this.displayName = name;
    }
    
    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public RoleAssigneeDisplayInfo getDisplayInfo() {
        return new RoleAssigneeDisplayInfo(displayName, null);
    }

    @Override
    public String getIdentifier() {
        return Group.IDENTIFIER_PREFIX + getAlias();
    }
    
    
    @Override
    public String toString() {
        return "[PersistedGlobalGroup " + getIdentifier() + "]";
    }
    
    @Override
    public int compareTo(Object o) {
        if (o instanceof AuthenticatedUser) {
            AuthenticatedUser other = (AuthenticatedUser) o;
            return this.getDisplayName().toUpperCase().compareTo(other.getLastName().toUpperCase());
        }

        if (o instanceof Group) {
            Group other = (Group) o;
            return this.getDisplayName().toUpperCase().compareTo(other.getDisplayName().toUpperCase());
        }
        
        return 0;
    }
}
