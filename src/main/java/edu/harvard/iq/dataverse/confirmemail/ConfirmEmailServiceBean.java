package edu.harvard.iq.dataverse.confirmemail;

import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.MailServiceBean;
import edu.harvard.iq.dataverse.UserNotification;
import edu.harvard.iq.dataverse.api.AbstractApiBean;
import edu.harvard.iq.dataverse.authorization.providers.shib.ShibAuthenticationProvider;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.MailUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

/**
 *
 * @author bsilverstein
 */
@Stateless
public class ConfirmEmailServiceBean {

    private static final Logger logger = Logger.getLogger(ConfirmEmailServiceBean.class.getCanonicalName());

    @EJB
    AuthenticationServiceBean authenticationService;

    @EJB
    MailServiceBean mailService;

    @EJB
    SystemConfig systemConfig;

    @EJB DataverseServiceBean dataverseService;

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    
    /**
     * A simple interface to check if a user email has been verified or not.
     * @param user
     * @return true if verified, false otherwise
     */
    public boolean hasVerifiedEmail(AuthenticatedUser user) {
        // Look up the user again in case the "verify email" link was clicked in another browser.
        user = authenticationService.findByID(user.getId());
        boolean hasTimestamp = user.getEmailConfirmed() != null;
        boolean isVerifiedByAuthProvider = authenticationService.lookupProvider(user).isEmailVerified();
        // Note: In practice, we are relying on hasTimestamp to know if an email
        // has been confirmed/verified or not. We have switched the Shib code to automatically
        // overwrite the "confirm email" timestamp on login. So hasTimeStamp will be enough.
        // If we ever want to get away from using "confirmed email" timestamps for Shib users
        // we can make use of the isVerifiedByAuthProvider boolean. Currently,
        // isVerifiedByAuthProvider is set to false in the super class and nothing
        // is overridden in the shib auth provider (or any auth provider) but we could override
        // isVerifiedByAuthProvider in the Shib auth provider and have it return true.
        return hasTimestamp || isVerifiedByAuthProvider;
    }

    /**
     * Initiate the email confirmation process.
     *
     * @param user
     * @return {@link ConfirmEmailInitResponse}
     */
    public ConfirmEmailInitResponse beginConfirm(AuthenticatedUser user) throws ConfirmEmailException {
        deleteAllExpiredTokens();
        if (user != null) {
            return sendConfirm(user, true);
        } else {
            return new ConfirmEmailInitResponse(false);
        }
    }

    private ConfirmEmailInitResponse sendConfirm(AuthenticatedUser aUser, boolean sendEmail) throws ConfirmEmailException {
        // delete old tokens for the user
        ConfirmEmailData oldToken = findSingleConfirmEmailDataByUser(aUser);
        if (oldToken != null) {
            em.remove(oldToken);
        }

        aUser.setEmailConfirmed(null);
        aUser = em.merge(aUser);
        // create a fresh token for the user iff they don't have an existing token
        ConfirmEmailData confirmEmailData = new ConfirmEmailData(aUser, systemConfig.getMinutesUntilConfirmEmailTokenExpires());
        try {
            /**
             * @todo This "persist" is causing lots of noise in Glassfish's
             * server.log if a token already exists (i.e. it isn't expired and
             * wasn't deleted above). Exercise this bug by running
             * ConfirmEmailIT.
             */
            em.persist(confirmEmailData);
            // TODO: don't hard-code SystemConfig.UI.JSF to JSF. When the SPA is in use, what should the email say?
            ConfirmEmailInitResponse confirmEmailInitResponse = new ConfirmEmailInitResponse(true, confirmEmailData, optionalConfirmEmailAddonMsg(aUser, SystemConfig.UI.JSF));
            if (sendEmail) {
                sendLinkOnEmailChange(aUser, confirmEmailInitResponse.getConfirmUrl());
            }

            return confirmEmailInitResponse;
        } catch (Exception ex) {
            String msg = "Unable to save token for " + aUser.getEmail();
            throw new ConfirmEmailException(msg, ex);
        }

    }

    /**
     * @todo: We expect to send two messages. One at signup and another at email
     * change.
     */
    private void sendLinkOnEmailChange(AuthenticatedUser aUser, String confirmationUrl) throws ConfirmEmailException {
        String messageBody = BundleUtil.getStringFromBundle("notification.email.changeEmail", Arrays.asList(
                aUser.getFirstName(),
                confirmationUrl,
                ConfirmEmailUtil.friendlyExpirationTime(systemConfig.getMinutesUntilConfirmEmailTokenExpires())
        ));
        logger.log(Level.FINE, "messageBody:{0}", messageBody);

        try {
            String toAddress = aUser.getEmail();

            // FIXME: consider refactoring this into MailServiceBean.sendNotificationEmail.
            // CONFIRMEMAIL may be the only type where we don't want an in-app notification.
            UserNotification userNotification = new UserNotification();
            userNotification.setType(UserNotification.Type.CONFIRMEMAIL);
            String subject = MailUtil.getSubjectTextBasedOnNotification(userNotification, null);
            logger.fine("sending email to " + toAddress + " with this subject: " + subject);
            if (ShibAuthenticationProvider.PROVIDER_ID.equals(aUser.getAuthenticatedUserLookup().getAuthenticationProviderId())) {
                // Shib users have "emailconfirmed" timestamp set on login.
                logger.info("Returning early to prevent an email confirmation link from being sent to Shib user " + aUser.getUserIdentifier() + ".");
                return;
            }
            mailService.sendSystemEmail(toAddress, subject, messageBody);
        } catch (Exception ex) {
            /**
             * @todo get more specific about the exception that's thrown when
             * `asadmin create-javamail-resource` (or equivalent) hasn't been
             * run.
             */
            throw new ConfirmEmailException("Problem sending email confirmation link possibily due to mail server not being configured.");
        }
        logger.log(Level.FINE, "attempted to send mail to {0}", aUser.getEmail());
    }

    /**
     * Process the email confirmation token. If all looks good, set the
     * timestamp and delete the token/confirmEmailData.
     *
     * @param tokenQueried
     * @return ConfirmEmailExecResponse
     * @throws Exception with details of the problem we can show the user.
     */
    public ConfirmEmailExecResponse processToken(String tokenQueried) throws Exception {
        deleteAllExpiredTokens();
        ConfirmEmailData confirmEmailData;
        try {
            confirmEmailData = findSingleConfirmEmailDataByToken(tokenQueried);
        } catch (ConfirmEmailException ex) {
            logger.info("processToken: could not find single ConfirmEmailData row using token " + tokenQueried);
            throw new Exception(BundleUtil.getStringFromBundle("confirmEmail.details.failure.invalidToken"));
        }
        if (confirmEmailData == null) {
            // shouldn't reach here because "invalid token" exception should have already been thrown.
            logger.info("processToken: ConfirmEmailData is null using token " + tokenQueried);
            throw new Exception(BundleUtil.getStringFromBundle("confirmEmail.details.failure.lookupFailed"));
        }
        if (confirmEmailData.isExpired()) {
            // shouldn't reach here since tokens are being expired above
            logger.info("processToken: Token is expired: " + tokenQueried);
            throw new Exception(BundleUtil.getStringFromBundle("confirmEmail.details.failure.tokenExpired"));
        }
        // No need for null check because confirmEmailData always has a user (a foreign key).
        AuthenticatedUser authenticatedUser = confirmEmailData.getAuthenticatedUser();
        if (authenticatedUser.isDeactivated()) {
            logger.info("processToken: User is deactivated. Token was " + tokenQueried);
            throw new Exception(BundleUtil.getStringFromBundle("confirmEmail.details.failure.userDeactivated"));
        }
        ConfirmEmailExecResponse response = new ConfirmEmailExecResponse(tokenQueried, confirmEmailData);
        long nowInMilliseconds = new Date().getTime();
        Timestamp emailConfirmed = new Timestamp(nowInMilliseconds);
        authenticatedUser.setEmailConfirmed(emailConfirmed);
        em.remove(confirmEmailData);
        return response;
    }

    /**
     * @param token
     * @return Null or a single row of email confirmation data.
     */
    private ConfirmEmailData findSingleConfirmEmailDataByToken(String token) throws ConfirmEmailException {
        TypedQuery<ConfirmEmailData> typedQuery = em.createNamedQuery("ConfirmEmailData.findByToken", ConfirmEmailData.class);
        typedQuery.setParameter("token", token);
        try {
            return typedQuery.getSingleResult();
        } catch (NoResultException | NonUniqueResultException ex) {
            logger.info("findSingleConfirmEmailDataByToken: When looking up " + token + " caught an exception:" + ex);
            throw new ConfirmEmailException("");
        }
    }

    public ConfirmEmailData findSingleConfirmEmailDataByUser(AuthenticatedUser user) {
        ConfirmEmailData confirmEmailData = null;
        TypedQuery<ConfirmEmailData> typedQuery = em.createNamedQuery("ConfirmEmailData.findByUser", ConfirmEmailData.class);
        typedQuery.setParameter("user", user);
        try {
            confirmEmailData = typedQuery.getSingleResult();
        } catch (NoResultException | NonUniqueResultException ex) {
            logger.fine("When looking up user " + user + " caught " + ex);
        }
        return confirmEmailData;
    }
    
    public boolean hasActiveVerificationToken(AuthenticatedUser au) {
        if (findSingleConfirmEmailDataByUser(au) == null){
            return false;
        }
        return !findSingleConfirmEmailDataByUser(au).isExpired();
    }


    public List<ConfirmEmailData> findAllConfirmEmailData() {
        TypedQuery<ConfirmEmailData> typedQuery = em.createNamedQuery("ConfirmEmailData.findAll", ConfirmEmailData.class);
        List<ConfirmEmailData> confirmEmailDatas = typedQuery.getResultList();
        return confirmEmailDatas;
    }

    /**
     * @return The number of tokens deleted.
     */
    private long deleteAllExpiredTokens() {
        long numDeleted = 0;
        List<ConfirmEmailData> allData = findAllConfirmEmailData();
        for (ConfirmEmailData data : allData) {
            if (data.isExpired()) {
                em.remove(data);
                numDeleted++;
            }
        }
        return numDeleted;
    }

    /**
     * @param authenticatedUser
     * @return True if token is deleted. False otherwise.
     */
    public boolean deleteTokenForUser(AuthenticatedUser authenticatedUser) {
        ConfirmEmailData confirmEmailData = findSingleConfirmEmailDataByUser(authenticatedUser);
        if (confirmEmailData != null) {
            em.remove(confirmEmailData);
            return true;
        }
        return false;
    }

    public ConfirmEmailData createToken(AuthenticatedUser au) {
        ConfirmEmailData confirmEmailData = new ConfirmEmailData(au, systemConfig.getMinutesUntilConfirmEmailTokenExpires());
        em.persist(confirmEmailData);
        return confirmEmailData;
    }

    public String optionalConfirmEmailAddonMsg(AuthenticatedUser user, SystemConfig.UI ui) {
        final String emptyString = "";
        if (SystemConfig.UI.SPA.equals(ui)) {
            // Return early to avoid giving the SPA a JSF/xhtml URL.
            // TODO: when "confirm email" is implemented by the SPA, return the proper SPA URL.
            return emptyString;
        }
        if (user == null) {
            logger.info("Can't return confirm email message. AuthenticatedUser was null!");
            return emptyString;
        }
        if (ShibAuthenticationProvider.PROVIDER_ID.equals(user.getAuthenticatedUserLookup().getAuthenticationProviderId())) {
            // Shib users don't have to confirm their email address.
            return emptyString;
        }
        ConfirmEmailData confirmEmailData = findSingleConfirmEmailDataByUser(user);
        if (confirmEmailData == null) {
            logger.info("Can't return confirm email message. No ConfirmEmailData for user id " + user.getId());
            return emptyString;
        }
        String expTime = ConfirmEmailUtil.friendlyExpirationTime(systemConfig.getMinutesUntilConfirmEmailTokenExpires());
        String confirmEmailUrl = systemConfig.getDataverseSiteUrl() + "/confirmemail.xhtml?token=" + confirmEmailData.getToken();
        List<String> args = Arrays.asList(confirmEmailUrl, expTime);
        String optionalConfirmEmailMsg = BundleUtil.getStringFromBundle("notification.email.welcomeConfirmEmailAddOn", args);
        return optionalConfirmEmailMsg;
    }

}
