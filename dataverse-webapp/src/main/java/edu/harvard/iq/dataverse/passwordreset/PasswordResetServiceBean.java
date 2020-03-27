package edu.harvard.iq.dataverse.passwordreset;

import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUserServiceBean;
import edu.harvard.iq.dataverse.authorization.providers.builtin.PasswordEncryption;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.mail.EmailContent;
import edu.harvard.iq.dataverse.mail.MailService;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.BuiltinUser;
import edu.harvard.iq.dataverse.persistence.user.PasswordResetData;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.validation.PasswordValidatorServiceBean;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class PasswordResetServiceBean {

    private static final Logger logger = Logger.getLogger(PasswordResetServiceBean.class.getCanonicalName());

    @EJB
    BuiltinUserServiceBean dataverseUserService;

    @EJB
    MailService mailService;

    @EJB
    PasswordValidatorServiceBean passwordValidatorService;

    @EJB
    AuthenticationServiceBean authService;

    @Inject
    private SystemConfig systemConfig;

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    /**
     * Initiate the password reset process.
     *
     * @param emailAddress
     * @return {@link PasswordResetInitResponse}
     * @throws edu.harvard.iq.dataverse.passwordreset.PasswordResetException
     */
    // inspired by Troy Hunt: Everything you ever wanted to know about building a secure password reset feature - http://www.troyhunt.com/2012/05/everything-you-ever-wanted-to-know.html
    public PasswordResetInitResponse requestReset(String emailAddress) throws PasswordResetException {
        deleteAllExpiredTokens();
        AuthenticatedUser authUser = authService.getAuthenticatedUserByEmail(emailAddress);
        BuiltinUser user = dataverseUserService.findByUserName(authUser.getUserIdentifier());

        if (user != null) {
            return requestPasswordReset(user, true, PasswordResetData.Reason.FORGOT_PASSWORD);
        } else {
            return new PasswordResetInitResponse(false);
        }
    }

    public PasswordResetInitResponse requestPasswordReset(BuiltinUser aUser, boolean sendEmail, PasswordResetData.Reason reason) throws PasswordResetException {
        AuthenticatedUser authUser = authService.getAuthenticatedUser(aUser.getUserName());

        // delete old tokens for the user
        List<PasswordResetData> oldTokens = findPasswordResetDataByDataverseUser(aUser);
        for (PasswordResetData oldToken : oldTokens) {
            em.remove(oldToken);
        }

        // create a fresh token for the user
        PasswordResetData passwordResetData = new PasswordResetData(aUser);
        passwordResetData.setExpires(new Timestamp(
                passwordResetData.getCreated().getTime() +
                        TimeUnit.MINUTES.toMillis(systemConfig.getMinutesUntilPasswordResetTokenExpires())));
        passwordResetData.setReason(reason);
        try {
            em.persist(passwordResetData);
            PasswordResetInitResponse passwordResetInitResponse =
                    new PasswordResetInitResponse(true, passwordResetData, createResetUrl(passwordResetData));
            if (sendEmail) {
                sendPasswordResetEmail(aUser, passwordResetInitResponse.getResetUrl());
            }

            return passwordResetInitResponse;

        } catch (Exception ex) {
            String msg = "Unable to save token for " + authUser.getEmail();
            throw new PasswordResetException(msg, ex);
        }

    }

    private String createResetUrl(PasswordResetData passwordResetData) {
        // default to localhost
        String finalHostname;
        String configuredHostname = systemConfig.getDataverseServer();

        if (configuredHostname.equals("localhost")) {
            // must be a dev environment
            finalHostname = "localhost:8181";
        } else {
            finalHostname = configuredHostname;
        }

        return "https://" + finalHostname + "/passwordreset.xhtml?token=" + passwordResetData.getToken();
    }


    private void sendPasswordResetEmail(BuiltinUser aUser, String passwordResetUrl) throws PasswordResetException {
        AuthenticatedUser authUser = authService.getAuthenticatedUser(aUser.getUserName());

        String pattern = BundleUtil.getStringFromBundle("notification.email.passwordReset");

        String[] paramArray = {authUser.getName(), aUser.getUserName(), passwordResetUrl, systemConfig.getMinutesUntilPasswordResetTokenExpires() + ""};
        String messageBody = MessageFormat.format(pattern, paramArray);

        String toAddress = authUser.getEmail();
        String subject = BundleUtil.getStringFromBundle("notification.email.passwordReset.subject");

        String footerMailMessage = mailService.getFooterMailMessage(authUser.getNotificationsLanguage());
        boolean emailSent = mailService.sendMail(toAddress, new EmailContent(subject, messageBody, footerMailMessage));

        if (!emailSent) {
            throw new PasswordResetException("Problem sending password reset email possibily due to mail server not being configured.");
        }

        logger.log(Level.INFO, "attempted to send mail to {0}", authUser.getEmail());
    }

    /**
     * Process the password reset token, allowing the user to reset the password
     * or report on a invalid token.
     *
     * @param tokenQueried
     */
    public PasswordResetExecResponse processToken(String tokenQueried) {
        deleteAllExpiredTokens();
        PasswordResetExecResponse tokenUnusable = new PasswordResetExecResponse(tokenQueried, null);
        PasswordResetData passwordResetData = findSinglePasswordResetDataByToken(tokenQueried);
        if (passwordResetData != null) {
            if (passwordResetData.isExpired()) {
                // shouldn't reach here since tokens are being expired above
                return tokenUnusable;
            } else {
                PasswordResetExecResponse goodTokenCanProceed = new PasswordResetExecResponse(tokenQueried, passwordResetData);
                return goodTokenCanProceed;
            }
        } else {
            return tokenUnusable;
        }
    }

    /**
     * @param token
     * @return Null or a single row of password reset data.
     */
    private PasswordResetData findSinglePasswordResetDataByToken(String token) {
        PasswordResetData passwordResetData = null;
        TypedQuery<PasswordResetData> typedQuery = em.createNamedQuery("PasswordResetData.findByToken", PasswordResetData.class);
        typedQuery.setParameter("token", token);
        try {
            passwordResetData = typedQuery.getSingleResult();
        } catch (NoResultException | NonUniqueResultException ex) {
            logger.info("When looking up " + token + " caught " + ex);
        }
        return passwordResetData;
    }

    public List<PasswordResetData> findPasswordResetDataByDataverseUser(BuiltinUser user) {
        TypedQuery<PasswordResetData> typedQuery = em.createNamedQuery("PasswordResetData.findByUser", PasswordResetData.class);
        typedQuery.setParameter("user", user);
        List<PasswordResetData> passwordResetDatas = typedQuery.getResultList();
        return passwordResetDatas;
    }

    public List<PasswordResetData> findAllPasswordResetData() {
        TypedQuery<PasswordResetData> typedQuery = em.createNamedQuery("PasswordResetData.findAll", PasswordResetData.class);
        List<PasswordResetData> passwordResetDatas = typedQuery.getResultList();
        return passwordResetDatas;
    }

    /**
     * @return The number of tokens deleted.
     */
    private long deleteAllExpiredTokens() {
        long numDeleted = 0;
        List<PasswordResetData> allData = findAllPasswordResetData();
        for (PasswordResetData data : allData) {
            if (data.isExpired()) {
                em.remove(data);
                numDeleted++;
            }
        }
        return numDeleted;
    }

    public PasswordChangeAttemptResponse attemptPasswordReset(BuiltinUser user, String newPassword, String token) {

        final String messageSummarySuccess = "Password Reset Successfully";
        final String messageDetailSuccess = "";

        // optimistic defaults :)
        String messageSummary = messageSummarySuccess;
        String messageDetail = messageDetailSuccess;

        final String messageSummaryFail = "Password Reset Problem";
        if (user == null) {
            messageSummary = messageSummaryFail;
            messageDetail = "User could not be found.";
            return new PasswordChangeAttemptResponse(false, messageSummary, messageDetail);
        }
        if (newPassword == null) {
            messageSummary = messageSummaryFail;
            messageDetail = "New password not provided.";
            return new PasswordChangeAttemptResponse(false, messageSummary, messageDetail);
        }
        if (token == null) {
            logger.info("No token provided... won't be able to delete it. Let the user change the password though.");
        }


        List<String> errors = passwordValidatorService.validate(newPassword);
        if (!errors.isEmpty()) {
            messageSummary = PasswordValidatorServiceBean.parseMessages(errors);
            logger.info(messageDetail);
            return new PasswordChangeAttemptResponse(false, messageSummary, messageSummaryFail);
        }

        String newHashedPass = PasswordEncryption.get().encrypt(newPassword);
        int latestVersionNumber = PasswordEncryption.getLatestVersionNumber();
        user.updateEncryptedPassword(newHashedPass, latestVersionNumber);
        BuiltinUser savedUser = dataverseUserService.save(user);

        if (savedUser != null) {
            messageSummary = messageSummarySuccess;
            messageDetail = messageDetailSuccess;
            boolean tokenDeleted = deleteToken(token);
            if (!tokenDeleted) {
                // suboptimal but when it expires it should be deleted
                logger.info("token " + token + " for user id " + user.getId() + " was not deleted");
            }
            AuthenticatedUser authUser = authService.getAuthenticatedUser(user.getUserName());

            String toAddress = authUser.getEmail();
            String subject = "Dataverse Password Reset Successfully Changed";


            String messageBody = "Hi " + authUser.getName() + ",\n\n"
                    + "Your Dataverse account password was successfully changed.\n\n"
                    + "Please contact us if you did not request this password reset or need further help.\n\n";

            String footerMailMessage = mailService.getFooterMailMessage(authUser.getNotificationsLanguage());
            mailService.sendMailAsync(toAddress, new EmailContent(subject, messageBody, footerMailMessage));
            return new PasswordChangeAttemptResponse(true, messageSummary, messageDetail);
        } else {
            messageSummary = messageSummaryFail;
            messageDetail = "Your password was not reset. Please contact support.";
            logger.info("Enable to save user " + user.getId());
            return new PasswordChangeAttemptResponse(false, messageSummary, messageDetail);
        }

    }

    private boolean deleteToken(String token) {
        PasswordResetData doomed = findSinglePasswordResetDataByToken(token);
        try {
            em.remove(doomed);
            return true;
        } catch (Exception ex) {
            logger.info("Caught exception trying to delete token " + token + " - " + ex);
            return false;
        }
    }
}
