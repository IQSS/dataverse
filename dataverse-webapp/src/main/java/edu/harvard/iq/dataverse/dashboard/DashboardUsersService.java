package edu.harvard.iq.dataverse.dashboard;

import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.UserServiceBean;
import edu.harvard.iq.dataverse.engine.command.impl.GrantSuperuserStatusCommand;
import edu.harvard.iq.dataverse.engine.command.impl.RevokeAllRolesCommand;
import edu.harvard.iq.dataverse.engine.command.impl.RevokeSuperuserStatusCommand;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;

import javax.ejb.Stateless;
import javax.inject.Inject;

import static edu.harvard.iq.dataverse.GlobalIdServiceBean.logger;

@Stateless
public class DashboardUsersService {

    private EjbDataverseEngine commandEngine;
    private DataverseRequestServiceBean dvRequestService;
    private UserServiceBean userServiceBean;

    // -------------------- CONSTRUCTORS --------------------

    @Deprecated
    public DashboardUsersService() {
    }

    @Inject
    public DashboardUsersService(EjbDataverseEngine commandEngine, DataverseRequestServiceBean dvRequestService,
                                 UserServiceBean userServiceBean) {
        this.commandEngine = commandEngine;
        this.dvRequestService = dvRequestService;
        this.userServiceBean = userServiceBean;
    }

    // -------------------- LOGIC --------------------

    public AuthenticatedUser changeSuperuserStatus(AuthenticatedUser user) {
        logger.fine("Toggling user's " + user.getIdentifier() + " superuser status; (current status: " + user.isSuperuser() + ")");
        logger.fine("Attempting to save user " + user.getIdentifier());
        logger.fine("selectedUserPersistent info: " + user.getId() + " set to: " + user.isSuperuser());

        AuthenticatedUser dbUser = userServiceBean.find(user.getId());

        if (dbUser.isSuperuser()) {
            return revokeSuperuserStatus(dbUser);
        } else {
            return grantSuperuserStatus(dbUser);
        }
    }

    public AuthenticatedUser revokeAllRolesForUser(AuthenticatedUser user) {
        return commandEngine.submit(new RevokeAllRolesCommand(user, dvRequestService.getDataverseRequest()));
    }

    // -------------------- PRIVATE ---------------------

    private AuthenticatedUser grantSuperuserStatus(AuthenticatedUser user) {
        return commandEngine.submit(new GrantSuperuserStatusCommand(user, dvRequestService.getDataverseRequest()));
    }

    private AuthenticatedUser revokeSuperuserStatus(AuthenticatedUser user) {
        return commandEngine.submit(new RevokeSuperuserStatusCommand(user, dvRequestService.getDataverseRequest()));
    }
}
