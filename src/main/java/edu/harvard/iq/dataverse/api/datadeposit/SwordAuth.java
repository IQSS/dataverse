package edu.harvard.iq.dataverse.api.datadeposit;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseRoleServiceBean;
import edu.harvard.iq.dataverse.DataverseUser;
import edu.harvard.iq.dataverse.DataverseUserServiceBean;
import edu.harvard.iq.dataverse.PasswordEncryption;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.authorization.Permission;
import java.util.logging.Logger;
import javax.ejb.EJB;
import org.swordapp.server.AuthCredentials;
import org.swordapp.server.SwordAuthException;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;

public class SwordAuth {

    private static final Logger logger = Logger.getLogger(SwordAuth.class.getCanonicalName());

    @EJB
    DataverseUserServiceBean dataverseUserService;
    @EJB
    PermissionServiceBean permissionService;
    @EJB
    DataverseRoleServiceBean roleService;

    public DataverseUser auth(AuthCredentials authCredentials) throws SwordAuthException, SwordServerException {

        if (authCredentials != null) {
            String username = authCredentials.getUsername();
            String password = authCredentials.getPassword();
            logger.fine("Checking username " + username + " ...");
            DataverseUser dataverseUser = dataverseUserService.findByUserName(username);
            if (dataverseUser != null) {
                String encryptedPassword = PasswordEncryption.getInstance().encrypt(password);
                if (encryptedPassword.equals(dataverseUser.getEncryptedPassword())) {
                    return dataverseUser;
                } else {
                    logger.fine("wrong password");
                    throw new SwordAuthException();
                }
            } else {
                logger.fine("could not find username: " + username);
                throw new SwordAuthException();
            }
        } else {
            // in DVN 3.x at least, it seems this was never reached... eaten somewhere by way of ServiceDocumentServletDefault -> ServiceDocumentAPI -> SwordAPIEndpoint
            logger.info("no credentials provided");
            throw new SwordAuthException();
        }
    }

    boolean hasAccessToModifyDataverse(DataverseUser dataverseUser, Dataverse dataverse) throws SwordError {
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
        for (RoleAssignment roleAssignment : roleService.assignmentsFor(dataverseUser, dataverse).getAssignments()) {
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
            logger.fine(dataverse.getAlias() + ": " + dataverseUser.getUserName() + " has role " + roleAssignment.getRole().getAlias());
        }
        if (permissionService.userOn(dataverseUser, dataverse).has(Permission.DestructiveEdit)) {
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
