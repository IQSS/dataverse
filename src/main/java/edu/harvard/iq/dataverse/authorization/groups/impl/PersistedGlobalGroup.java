package edu.harvard.iq.dataverse.authorization.groups.impl;

import edu.harvard.iq.dataverse.authorization.groups.Group;
import edu.harvard.iq.dataverse.authorization.RoleAssigneeDisplayInfo;
import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

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
public abstract class PersistedGlobalGroup implements Group, Serializable {
    
    @Id
    @GeneratedValue
    private Long id;
    
    /**
     * A unique alias within the Dataverse system installation.
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
    

}
