package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.DataverseRole;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignment;
import edu.harvard.iq.dataverse.persistence.user.User;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PermissionServiceBeanTest {

    @Mock
    private DataverseRoleServiceBean dataverseRoleServiceBean;

    @InjectMocks
    private PermissionServiceBean permissionServiceBean;

    @Test
    public void verifyIfDataverseAdminIsVerifiedAsSuch() {
        //given
        User user = new AuthenticatedUser();
        Dataverse dataverse = new Dataverse();

        //when
        when(dataverseRoleServiceBean.directRoleAssignments(user, dataverse))
                .thenReturn(assignRoleForUserInDataverse(user, DataverseRole.ADMIN));
        boolean isUserAdminForDataverse = permissionServiceBean.isUserAdminForDataverse(user, dataverse);
        //then
        assertTrue(isUserAdminForDataverse);
    }

    @Test
    public void verifyIfDataverseMemberIsVerifiedAsSuch() {
        //given
        User user = new AuthenticatedUser();
        Dataverse dataverse = new Dataverse();

        //when
        when(dataverseRoleServiceBean.directRoleAssignments(user, dataverse))
                .thenReturn(assignRoleForUserInDataverse(user, DataverseRole.MEMBER));
        boolean isUserAdminForDataverse = permissionServiceBean.isUserAdminForDataverse(user, dataverse);
        //then
        assertTrue(!isUserAdminForDataverse);
    }

    private List<RoleAssignment> assignRoleForUserInDataverse(User user, String dataverseRole) {
        RoleAssignment roleAssignment = new RoleAssignment();

        DataverseRole adminRole = new DataverseRole();
        adminRole.setAlias(dataverseRole);

        roleAssignment.setRole(adminRole);
        roleAssignment.setAssigneeIdentifier(user.getIdentifier());
        return Collections.singletonList(roleAssignment);
    }
}
