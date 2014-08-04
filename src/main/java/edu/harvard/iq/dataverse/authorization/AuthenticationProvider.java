package edu.harvard.iq.dataverse.authorization;

public interface AuthenticationProvider {

    abstract RoleAssignee getRoleAssignee(String identifier);

}
