package edu.harvard.iq.dataverse.api.datadeposit;

import edu.harvard.iq.dataverse.api.AbstractApiBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import java.util.logging.Logger;
import org.swordapp.server.AuthCredentials;
import org.swordapp.server.SwordAuthException;
import org.swordapp.server.SwordServerException;

/**
 * In early version of Dataverse 4 this class was responsible for both
 * instantiating an AuthenticatedUser and enforcing permissions but now
 * permission enforcement is done inside each of the methods in the "*Impl.java"
 * files for SWORD.
 */
public class SwordAuth extends AbstractApiBean {

    private static final Logger logger = Logger.getLogger(SwordAuth.class.getCanonicalName());

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

        // Checking if the user is deactivated is done inside findUserByApiToken.
        AuthenticatedUser authenticatedUserFromToken = findUserByApiToken(username);
        if (authenticatedUserFromToken == null) {
            String msg = "User not found based on API token.";
            logger.fine(msg);
            throw new SwordAuthException(msg);
        } else {

            authenticatedUserFromToken = userSvc.updateLastApiUseTime(authenticatedUserFromToken);
            return authenticatedUserFromToken;
        }
    }

}
