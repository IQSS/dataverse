package edu.harvard.iq.dataverse.authorization;

public interface RoleAssignee {

    String identifier = new String();
    boolean showInLists = false;

    public String displayInfo();

}
