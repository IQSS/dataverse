package edu.harvard.iq.dataverse.confirmemail;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.MailServiceBean;
import edu.harvard.iq.dataverse.authorization.providers.shib.ShibAuthenticationProvider;
import edu.harvard.iq.dataverse.util.BundleUtil;
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
    AuthenticationServiceBean dataverseUserService;

    @EJB
    MailServiceBean mailService;

    @EJB
    SystemConfig systemConfig;

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    /**
     * Initiate the email confirmation process.
     *
     * @param emailAddress
     * @return {@link ConfirmEmailInitResponse}
     */
    public ConfirmEmailInitResponse beginConfirm(String emailAddress) throws ConfirmEmailException {
        deleteAllExpiredTokens();
        AuthenticatedUser user = dataverseUserService.getAuthenticatedUserByEmail(emailAddress);
        if (user != null) {
            return sendConfirm(user, true);
        } else {
            return new ConfirmEmailInitResponse(false);
        }
    }

    private ConfirmEmailInitResponse sendConfirm(AuthenticatedUser aUser, boolean sendEmail) throws ConfirmEmailException {
        // delete old tokens for the user
        List<ConfirmEmailData> oldTokens = findConfirmEmailDataByDataverseUser(aUser);
        for (ConfirmEmailData oldToken : oldTokens) {
            em.remove(oldToken);
        }

        aUser.setEmailConfirmed(null);
        aUser = em.merge(aUser);

        // create a fresh token for the user
        ConfirmEmailData confirmEmailData = new ConfirmEmailData(aUser, systemConfig.getMinutesUntilConfirmEmailTokenExpires());
        try {
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
        /**
         * @todo Move this to Bundle.properties and use
         * BundleUtil.getStringFromBundle
         */
        ConfirmEmailUtil confirmEmailUtil = new ConfirmEmailUtil();
        String messageBody = BundleUtil.getStringFromBundle("notification.email.changeEmail", Arrays.asList(
                aUser.getFirstName(),
                confirmationUrl,
                confirmEmailUtil.friendlyExpirationTime(systemConfig.getMinutesUntilConfirmEmailTokenExpires())
        ));
        logger.info("messageBody:" + messageBody);

        try {
            String toAddress = aUser.getEmail();
            String subject = "Dataverse Email Confirmation";
            mailService.sendSystemEmail(toAddress, subject, messageBody);
        } catch (Exception ex) {
            /**
             * @todo get more specific about the exception that's thrown when
             * `asadmin create-javamail-resource` (or equivalent) hasn't been
             * run.
             */
            throw new ConfirmEmailException("Problem sending email confirmation link possibily due to mail server not being configured.");
        }
        logger.log(Level.INFO, "attempted to send mail to {0}", aUser.getEmail());
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
                    logger.info("Invalid token.");
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

    public List<ConfirmEmailData> findConfirmEmailDataByDataverseUser(AuthenticatedUser user) {
        TypedQuery<ConfirmEmailData> typedQuery = em.createNamedQuery("ConfirmEmailData.findByUser", ConfirmEmailData.class);
        typedQuery.setParameter("user", user);
        List<ConfirmEmailData> confirmEmailDatas = typedQuery.getResultList();
        return confirmEmailDatas;
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

    public ConfirmEmailData createToken(AuthenticatedUser au) {
        ConfirmEmailData confirmEmailData = new ConfirmEmailData(au, systemConfig.getMinutesUntilConfirmEmailTokenExpires());
        em.persist(confirmEmailData);
        return confirmEmailData;
    }

    public String optionalConfirmEmailAddonMsg(AuthenticatedUser user) {
        final String emptyString = "";
        if (user == null) {
            logger.info("Can't return confirm email URL. AuthenticatedUser was null!");
            return emptyString;
        }
        if (ShibAuthenticationProvider.PROVIDER_ID.equals(user.getAuthenticatedUserLookup().getAuthenticationProviderId())) {
            // Shib users don't have to confirm their email address.
            return emptyString;
        }
        List<ConfirmEmailData> datas = findConfirmEmailDataByDataverseUser(user);
        int size = datas.size();
        if (size != 1) {
            logger.info("Can't return confirm email URL. ConfirmEmailData rows found for user id " + user.getId() + " was " + size + " rather than 1");
            return emptyString;
        }
        ConfirmEmailData confirmEmailData = datas.get(0);
        String confirmEmailUrl = systemConfig.getDataverseSiteUrl() + "/confirmemail.xhtml?token=" + confirmEmailData.getToken();
        String optionalConfirmEmailMsg = BundleUtil.getStringFromBundle("notification.email.welcomeConfirmEmailAddOn", Arrays.asList(confirmEmailUrl));
        return optionalConfirmEmailMsg;
    }

}
