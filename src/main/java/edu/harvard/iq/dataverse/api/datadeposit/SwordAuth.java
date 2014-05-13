package edu.harvard.iq.dataverse.api.datadeposit;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseUser;
import edu.harvard.iq.dataverse.DataverseUserServiceBean;
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

    public DataverseUser auth(AuthCredentials authCredentials) throws SwordAuthException, SwordServerException {

        if (authCredentials != null) {
            String username = authCredentials.getUsername();
            String password = authCredentials.getPassword();
            logger.fine("Checking username " + username + " ...");
            DataverseUser dataverseUser = dataverseUserService.findByUserName(username);
            if (dataverseUser != null) {
                /**
                 * @todo actually check the user's password! Can't use
                 * validateMethod on LoginPage.java since it's a backing bean...
                 */
//                if (dataverseUserService.validatePassword(username, password)) {
                return dataverseUser;
//                } else {
//                    logger.info("wrong password");
//                    throw new SwordAuthException();
//                }
            } else {
                logger.info("could not find username: " + username);
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
        if (dataverse.getCreator().equals(dataverseUser)) {
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
