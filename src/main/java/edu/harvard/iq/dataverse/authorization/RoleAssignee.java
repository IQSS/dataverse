package edu.harvard.iq.dataverse.authorization;

public interface RoleAssignee {

    /**
     * 
     * @return the unique identifier of the role assignee within the system. 
     */
    public String getIdentifier();

    public RoleAssigneeDisplayInfo getDisplayInfo();

}
