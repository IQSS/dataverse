package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataverseRoleServiceBean;
import edu.harvard.iq.dataverse.engine.TestCommandContext;
import edu.harvard.iq.dataverse.engine.TestDataverseEngine;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.persistence.MocksFactory;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.group.IpAddress;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.DataverseRole;
import edu.harvard.iq.dataverse.persistence.user.GuestUser;
import edu.harvard.iq.dataverse.persistence.user.Permission;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author michael
 */
public class CreateRoleCommandTest {

    boolean saveCalled = false;

    TestDataverseEngine engine = new TestDataverseEngine(new TestCommandContext() {
        @Override
        public DataverseRoleServiceBean roles() {
            return new DataverseRoleServiceBean() {
                @Override
                public DataverseRole save(DataverseRole aRole) {
                    saveCalled = true;
                    return aRole;
                }
            };
        }
    });

    @Before
    public void before() {
        saveCalled = false;
    }

    @Test(expected = IllegalCommandException.class)
    public void testNonSuperUsersCantAddRoles() throws CommandException {
        DataverseRole dvr = new DataverseRole();
        dvr.setAlias("roleTest");
        dvr.setName("Tester Role");
        dvr.addPermission(Permission.AddDataset);

        Dataverse dv = MocksFactory.makeDataverse();
        dvr.setOwner(dv);

        AuthenticatedUser normalUser = new AuthenticatedUser();
        normalUser.setSuperuser(false);

        CreateRoleCommand sut = new CreateRoleCommand(dvr, new DataverseRequest(normalUser, IpAddress.valueOf("89.17.33.33")), dv);
        engine.submit(sut);

    }

    @Test
    public void testSuperUsersAddRoles() throws CommandException {
        DataverseRole dvr = new DataverseRole();
        dvr.setAlias("roleTest");
        dvr.setName("Tester Role");
        dvr.addPermission(Permission.AddDataset);

        Dataverse dv = MocksFactory.makeDataverse();
        dvr.setOwner(dv);

        AuthenticatedUser normalUser = new AuthenticatedUser();
        normalUser.setSuperuser(true);

        CreateRoleCommand sut = new CreateRoleCommand(dvr, new DataverseRequest(normalUser, IpAddress.valueOf("89.17.33.33")), dv);
        engine.submit(sut);
        assertTrue("CreateRoleCommand did not call save on the created role.", saveCalled);

    }

    @Test(expected = IllegalCommandException.class)
    public void testGuestUsersCantAddRoles() throws CommandException {
        DataverseRole dvr = new DataverseRole();
        dvr.setAlias("roleTest");
        dvr.setName("Tester Role");
        dvr.addPermission(Permission.AddDataset);

        Dataverse dv = MocksFactory.makeDataverse();
        dvr.setOwner(dv);

        CreateRoleCommand sut = new CreateRoleCommand(dvr, new DataverseRequest(GuestUser.get(), IpAddress.valueOf("89.17.33.33")), dv);
        engine.submit(sut);
    }

}
