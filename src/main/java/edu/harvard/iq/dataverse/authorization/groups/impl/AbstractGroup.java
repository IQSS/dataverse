package edu.harvard.iq.dataverse.authorization.groups.impl;

import edu.harvard.iq.dataverse.authorization.groups.Group;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import java.util.Collections;
import java.util.Set;
import javax.servlet.ServletRequest;

/**
 * Convenience class for implementing {@link Groups}.
 * @author michael
 */
public abstract class AbstractGroup implements Group {
    
    private String alias;
    private String name;
    private String description;

    protected void setAlias(String alias) {
        this.alias = alias;
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
    public Set<Group> getDirectSubGroups() {
        return Collections.<Group>emptySet();
    }
    
}
