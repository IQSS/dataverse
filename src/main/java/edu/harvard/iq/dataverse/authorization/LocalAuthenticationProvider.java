package edu.harvard.iq.dataverse.authorization;

import edu.harvard.iq.dataverse.DataverseUser;
import edu.harvard.iq.dataverse.DataverseUserServiceBean;
import edu.harvard.iq.dataverse.authorization.groups.Group;
import edu.harvard.iq.dataverse.authorization.groups.GroupCreator;
import java.util.List;
import javax.ejb.EJB;

public class LocalAuthenticationProvider implements AuthenticationProvider, UserLister, GroupCreator {

    @EJB
    DataverseUserServiceBean dataverseUserService;

    private RoleAssignee roleAssignee;

    @Override
    public RoleAssignee getRoleAssignee(String identifier) {
        DataverseUser user = dataverseUserService.findByUserName(identifier);
        return roleAssignee;
    }

    @Override
    public List<User> listUsers() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Group createGroup() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
