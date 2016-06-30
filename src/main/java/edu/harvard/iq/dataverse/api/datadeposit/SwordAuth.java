package edu.harvard.iq.dataverse.api.datadeposit;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseRoleServiceBean;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.UserServiceBean;
import edu.harvard.iq.dataverse.api.AbstractApiBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import org.swordapp.server.AuthCredentials;
import org.swordapp.server.SwordAuthException;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;

/**
 * In early version of Dataverse 4 this class was responsible for both
 * instantiating an AuthenticatedUser and enforcing permissions but now
 * permission enforcement is done inside each of the methods in the "*Impl.java"
 * files for SWORD.
 */
public class SwordAuth extends AbstractApiBean {

    private static final Logger logger = Logger.getLogger(SwordAuth.class.getCanonicalName());

    @EJB
    PermissionServiceBean permissionService;
    @EJB
    DataverseRoleServiceBean roleService;
    @EJB
    UserServiceBean userService;
    public static boolean fixForIssue1070Enabled = true;

    public AuthenticatedUser auth(AuthCredentials authCredentials) throws SwordAuthException, SwordServerException {

        if (authCredentials == null) {
            /**
             * in DVN 3.x at least, it seems this was never reached... eaten
             * somewhere by way of ServiceDocumentServletDefault ->
             * ServiceDocumentAPI -> SwordAPIEndpoint
             */
            String msg = "No credentials provided.";
            throw new SwordAuthException(msg);
        }

        String username = authCredentials.getUsername();
        if (username == null) {
            String msg = "No API token/key (formerly username) provided.";
            logger.info(msg);
            throw new SwordAuthException(msg);
        }

        AuthenticatedUser authenticatedUserFromToken = findUserByApiToken(username);
        if (authenticatedUserFromToken == null) {
            String msg = "User not found based on API token.";
            logger.fine(msg);
            throw new SwordAuthException(msg);
        } else {
            return authenticatedUserFromToken;
        }
    }

    /**
     * @todo Delete this method. In DVN 3.x we required "admin only" at the
     * dataverse level. With the fancy new permission system in Dataverse 4 we
     * can now allow a lowly contributor to do most SWORD operations (except
     * publishing). See also https://github.com/IQSS/dataverse/issues/1070
     *
     * @todo When you delete this method, consider changing the error messages
     * that are returned in the new permissions check, which were copied from
     * when this method returns false. This copying was done for backward
     * compatibility but in some places, now that the check is not always done
     * on a dataverse, we could say something like "user not authorized to edit
     * dataset metadata" rather than "is not authorized to modify dataverse
     * [parent]".
     */
    boolean hasAccessToModifyDataverse(DataverseRequest dataverseRequest, Dataverse dataverse) throws SwordError {
        if (fixForIssue1070Enabled) {
            return true;
        }
        boolean authorized = false;

        /**
         * @todo use actual roles
         */
//        VDCRole role = vdcUser.getVDCRole(dv);
//        String roleString = null;
//        if (role != null) {
//            roleString = role.getRole().getName();
//            if ("admin".equals(roleString)) {
//                authorized = true;
//            } else if ("contributor".equals(roleString) || "curator".equals(roleString) || "privileged viewer".equals(roleString)) {
//                authorized = false;
//                 return early to avoid throwing exception when getting Service Document
//                return authorized;
//            } else {
//                authorized = false;
//            }
//        }
//
        for (RoleAssignment roleAssignment : roleService.assignmentsFor(dataverseRequest.getUser(), dataverse).getAssignments()) {
            /**
             * @todo do we want to hard code a check for the string "manager"
             * here? Probably not... for now let's just check for
             * Permission.DestructiveEdit which feels equivalent to the "admin"
             * role in DVN 3.x. We could also do a check for an admin-type
             * command like this: permissionService.userOn(dataverseUser,
             * dataverse).canIssue(DestroyDataverseCommand.class)
             *
             * @todo What about the root dataverse? With the GUI, any user can
             * create datasets in the root dataverse but users won't be "admin"
             * of the root dataverse. The "all or nothing" admin concept for all
             * SWORD operations will probably need to go away. Rather than a
             * single hasAccessToModifyDataverse method, we should have methods
             * per SWORD commands that map onto permissions like
             * canIssue(CreateDatasetCommand.class)
             */
            logger.log(Level.FINE, "{0}: {1} has role {2}", new Object[]{dataverse.getAlias(), dataverseRequest.getUser().getIdentifier(), roleAssignment.getRole().getAlias()});
        }
        if (permissionService.requestOn(dataverseRequest, dataverse).has(Permission.EditDataverse)) {
            authorized = true;
            return authorized;
        } else {
            authorized = false;
            return authorized;
        }

        /**
         * @todo: for backwards compatibility with DVN 3.x do we need to throw
         * this SWORD error?
         */
//        if (!authorized) {
//            throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "User " + dataverseUser.getUserName() + " with role of " + roleString + " is not authorized to modify dataverse " + dataverse.getAlias());
//        } else {
//            return authorized;
//        }
    }
}
