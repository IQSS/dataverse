package edu.harvard.iq.dataverse.authorization;

public interface RoleAssignee {

    /**
     * This is a JDBC-style identifier.
     *
     * User examples:
     *
     * - u:shib:idp.testshib.org:0109C89C-4BA2-42A5-969D-BB43D47DB409
     *
     * - u:local:jsmith
     *
     * Group examples:
     *
     * - g:shib:123
     *
     * - g:internal:all
     *
     * @return the unique identifier of the role assignee within the system. 
     */
    public String getIdentifier();

    public RoleAssigneeDisplayInfo getDisplayInfo();

}
