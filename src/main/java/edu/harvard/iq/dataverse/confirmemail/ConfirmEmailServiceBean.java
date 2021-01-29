package edu.harvard.iq.dataverse.confirmemail;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.MailServiceBean;
import edu.harvard.iq.dataverse.UserNotification;
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
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

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
        boolean hasTimestamp = user.getEmailConfirmed() != null;
        boolean hasNoStaleVerificationTokens = this.findSingleConfirmEmailDataByUser(user) == null;
        boolean isVerifiedByAuthProvider = authenticationService.lookupProvider(user).isEmailVerified();
        
        return (hasTimestamp && hasNoStaleVerificationTokens) || isVerifiedByAuthProvider;
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
            ConfirmEmailInitResponse confirmEmailInitResponse = new ConfirmEmailInitResponse(true, confirmEmailData, optionalConfirmEmailAddonMsg(aUser));
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
            try {
                Dataverse rootDataverse = dataverseService.findRootDataverse();
                if (rootDataverse != null) {
                    String rootDataverseName = rootDataverse.getName();
                    // FIXME: consider refactoring this into MailServiceBean.sendNotificationEmail. CONFIRMEMAIL may be the only type where we don't want an in-app notification.
                    UserNotification userNotification = new UserNotification();
                    userNotification.setType(UserNotification.Type.CONFIRMEMAIL);
                    String subject = MailUtil.getSubjectTextBasedOnNotification(userNotification, rootDataverseName, null);
                    logger.fine("sending email to " + toAddress + " with this subject: " + subject);
                    mailService.sendSystemEmail(toAddress, subject, messageBody);
                }
            } catch (Exception e) {
                logger.info("The root dataverse is not present. Don't send a notification to dataverseAdmin.");
            }
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
     * Process the email confirmation token, allowing the user to confirm the
     * email address or report on a invalid token.
     *
     * @param tokenQueried
     */
    public ConfirmEmailExecResponse processToken(String tokenQueried) {
        deleteAllExpiredTokens();
        ConfirmEmailExecResponse tokenUnusable = new ConfirmEmailExecResponse(tokenQueried, null);
        ConfirmEmailData confirmEmailData = findSingleConfirmEmailDataByToken(tokenQueried);
        if (confirmEmailData != null) {
            if (confirmEmailData.isExpired()) {
                // shouldn't reach here since tokens are being expired above
                return tokenUnusable;
            } else {
                ConfirmEmailExecResponse goodTokenCanProceed = new ConfirmEmailExecResponse(tokenQueried, confirmEmailData);
                if (confirmEmailData == null) {
                    logger.fine("Invalid token.");
                    return null;
                }
                long nowInMilliseconds = new Date().getTime();
                Timestamp emailConfirmed = new Timestamp(nowInMilliseconds);
                AuthenticatedUser authenticatedUser = confirmEmailData.getAuthenticatedUser();
                authenticatedUser.setEmailConfirmed(emailConfirmed);
                em.remove(confirmEmailData);
                return goodTokenCanProceed;
            }
        } else {
            return tokenUnusable;
        }
    }

    /**
     * @param token
     * @return Null or a single row of email confirmation data.
     */
    private ConfirmEmailData findSingleConfirmEmailDataByToken(String token) {
        ConfirmEmailData confirmEmailData = null;
        TypedQuery<ConfirmEmailData> typedQuery = em.createNamedQuery("ConfirmEmailData.findByToken", ConfirmEmailData.class);
        typedQuery.setParameter("token", token);
        try {
            confirmEmailData = typedQuery.getSingleResult();
        } catch (NoResultException | NonUniqueResultException ex) {
            logger.fine("When looking up " + token + " caught " + ex);
        }
        return confirmEmailData;
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

    public String optionalConfirmEmailAddonMsg(AuthenticatedUser user) {
        final String emptyString = "";
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
