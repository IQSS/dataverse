package edu.harvard.iq.dataverse.api.dto;

import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroup;

/**
 *
 * @author michael
 */
public class ExplicitGroupDTO  {
    
    private String description;
    private String displayName;
    private String aliasInOwner;
    
    /**
     * Applies the information in the DTO to the passed group.
     * The the passed group's {@link ExplicitGroup#setGroupAliasInOwner(java.lang.String)} is
     * updated only if it is {@code null}.
     * 
     * @param eg the group to be updated.
     * @return {@code eg}, for call chaining.
     */
    
    public ExplicitGroup apply( ExplicitGroup eg) {
        
        eg.setDescription(description);
        eg.setDisplayName(displayName);
        if ( eg.getGroupAliasInOwner() == null ) {
            eg.setGroupAliasInOwner(aliasInOwner);
        }
        
        return eg;
    }
    
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getAliasInOwner() {
        return aliasInOwner;
    }

    public void setAliasInOwner(String aliasInOwner) {
        this.aliasInOwner = aliasInOwner;
    }
    
    
}
