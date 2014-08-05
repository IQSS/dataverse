package edu.harvard.iq.dataverse.authorization;

public interface RoleAssignee {

    String identifier = "";
    boolean showInLists = false;

    public String displayInfo();

}
