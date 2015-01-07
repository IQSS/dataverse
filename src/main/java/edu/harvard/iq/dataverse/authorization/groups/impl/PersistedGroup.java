package edu.harvard.iq.dataverse.authorization.groups.impl;

import edu.harvard.iq.dataverse.api.Groups;
import edu.harvard.iq.dataverse.authorization.groups.Group;
import edu.harvard.iq.dataverse.authorization.RoleAssigneeDisplayInfo;
import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

/**
 * Convenience class for implementing {@link Groups}.
 * @author michael
 */
@NamedQueries({
    @NamedQuery( name="PersistedGroup.findByAlias",
                query="SELECT g FROM PersistedGroup g WHERE g.alias=:alias" )
})
@Entity
public abstract class PersistedGroup implements Group, Serializable {
    
    @Id
    @GeneratedValue
    private Long id;
    
    @Column(unique = true)
    private String alias;
    private String name;
    private String description;

    protected void setAlias(String alias) {
        this.alias = alias;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    
    @Override
    public String getAlias() {
        return alias;
    }

    protected void setName(String name) {
        this.name = name;
    }
    
    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    protected void setDescription(String description) {
        this.description = description;
    }

    @Override
    public RoleAssigneeDisplayInfo getDisplayInfo() {
        return new RoleAssigneeDisplayInfo(name, null);
    }

    @Override
    public String getIdentifier() {
        return Group.IDENTIFIER_PREFIX + getAlias();
    }
    
    
}
