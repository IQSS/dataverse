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
import java.util.logging.Logger;

@Stateless
public class DashboardUsersService {
    private static final Logger logger = Logger.getLogger(DashboardUsersService.class.getCanonicalName());

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

    public AuthenticatedUser changeSuperuserStatus(Long userId) {
        AuthenticatedUser dbUser = userServiceBean.find(userId);

        logger.fine("Toggling superuser status for user with id: " + userId + " superuser status; (current status: " + dbUser.isSuperuser() + ")");

        if (dbUser.isSuperuser()) {
            return revokeSuperuserStatus(dbUser);
        } else {
            return grantSuperuserStatus(dbUser);
        }
    }

    public AuthenticatedUser revokeAllRolesForUser(Long userId) {
        AuthenticatedUser dbUser = userServiceBean.find(userId);

        logger.fine("Revoking all roles for user: " + dbUser.getIdentifier());

        return commandEngine.submit(new RevokeAllRolesCommand(dbUser, dvRequestService.getDataverseRequest()));
    }

    // -------------------- PRIVATE ---------------------

    private AuthenticatedUser grantSuperuserStatus(AuthenticatedUser user) {
        return commandEngine.submit(new GrantSuperuserStatusCommand(user, dvRequestService.getDataverseRequest()));
    }

    private AuthenticatedUser revokeSuperuserStatus(AuthenticatedUser user) {
        return commandEngine.submit(new RevokeSuperuserStatusCommand(user, dvRequestService.getDataverseRequest()));
    }
}
