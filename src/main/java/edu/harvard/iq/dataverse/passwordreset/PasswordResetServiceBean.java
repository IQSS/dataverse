package edu.harvard.iq.dataverse.passwordreset;

import edu.harvard.iq.dataverse.DataverseUser;
import edu.harvard.iq.dataverse.DataverseUserServiceBean;
import edu.harvard.iq.dataverse.MailServiceBean;
import edu.harvard.iq.dataverse.PasswordEncryption;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

@Stateless
@Named
public class PasswordResetServiceBean {

    private static final Logger logger = Logger.getLogger(PasswordResetServiceBean.class.getCanonicalName());

    @EJB
    DataverseUserServiceBean dataverseUserService;

    @EJB
    MailServiceBean mailService;

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
        DataverseUser user = dataverseUserService.findByEmail(emailAddress);
        if (user != null) {
            // delete old tokens for the user
            List<PasswordResetData> oldTokens = findPasswordResetDataByDataverseUser(user);
            for (PasswordResetData oldToken : oldTokens) {
                em.remove(oldToken);
            }
            // create a fresh token for the user
            PasswordResetData passwordResetData = new PasswordResetData(user);
            PasswordResetData savedPasswordResetData = null;
            try {
                em.persist(passwordResetData);
                savedPasswordResetData = em.merge(passwordResetData);
            } catch (Exception ex) {
                String msg = "Unable to save token for " + emailAddress;
                throw new PasswordResetException(msg);
            }
            if (savedPasswordResetData != null) {
                PasswordResetInitResponse passwordResetInitResponse = new PasswordResetInitResponse(true, passwordResetData);
                /**
                 * @todo is the response the best place to store the reset link?
                 */
                String passwordResetUrl = passwordResetInitResponse.getResetUrl();
                String messageBody = "Hi " + user.getDisplayName() + ",\n\n"
                        + "Someone, hopefully you, requested a password reset for " + user.getUserName() + ".\n\n"
                        + "Please click the link below to reset your Dataverse account password:\n\n"
                        + passwordResetUrl + "\n\n"
                        + "The link above will only work for the next " + SystemConfig.getMinutesUntilPasswordResetTokenExpires() + " minutes.\n\n"
                        /**
                         * @todo It would be a nice touch to show the IP from
                         * which the password reset originated.
                         */
                        + "Please contact us if you did not request this password reset or need further help.\n\n"
                        + "Thank you,\n\n"
                        + "Dataverse Support Team";
                try {
                    String fromAddress = "do-not-reply@dataverse.org";
                    String toAddress = emailAddress;
                    String subject = "Dataverse Password Reset Requested";
                    mailService.sendMail(fromAddress, toAddress, subject, messageBody);
                } catch (Exception ex) {
                    /**
                     * @todo get more specific about the exception that's thrown
                     * when `asadmin create-javamail-resource` (or equivalent)
                     * hasn't been run.
                     */
                    throw new PasswordResetException("Problem sending password reset email possibily due to mail server not being configured.");
                }
                logger.info("attempted to send mail to " + emailAddress);
                return passwordResetInitResponse;
            } else {
                throw new PasswordResetException("Internal error. Unable to persist password reset token.");
            }
        } else {
            return new PasswordResetInitResponse(false);
        }
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
        TypedQuery<PasswordResetData> typedQuery = em.createQuery("SELECT OBJECT(o) FROM PasswordResetData AS o WHERE o.token = :token", PasswordResetData.class);
        typedQuery.setParameter("token", token);
        try {
            passwordResetData = typedQuery.getSingleResult();
        } catch (NoResultException | NonUniqueResultException ex) {
            logger.info("When looking up " + token + " caught " + ex);
        }
        return passwordResetData;
    }

    public List<PasswordResetData> findPasswordResetDataByDataverseUser(DataverseUser user) {
        TypedQuery<PasswordResetData> typedQuery = em.createQuery("SELECT OBJECT(o) FROM PasswordResetData AS o WHERE o.dataverseUser = :user", PasswordResetData.class);
        typedQuery.setParameter("user", user);
        List<PasswordResetData> passwordResetDatas = typedQuery.getResultList();
        return passwordResetDatas;
    }

    public List<PasswordResetData> findAllPasswordResetData() {
        TypedQuery<PasswordResetData> typedQuery = em.createQuery("SELECT OBJECT(o) FROM PasswordResetData AS o", PasswordResetData.class);
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
        logger.fine("expired tokens deleted: " + numDeleted);
        return numDeleted;
    }

    public PasswordChangeAttemptResponse attemptPasswordReset(DataverseUser user, String newPassword, String token) {

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

        /**
         * @todo move these rules deeper into the system
         */
        int minPasswordLength = 6;
        boolean forceNumber = true;
        boolean forceSpecialChar = false;
        boolean forceCapitalLetter = false;
        int maxPasswordLength = 255;
        /**
         *
         * @todo move the business rules for password complexity (once we've
         * defined them in https://github.com/IQSS/dataverse/issues/694 ) deeper
         * into the system and have all calls to
         * DataverseUser.setEncryptedPassword call into the password complexity
         * validataion method.
         *
         * @todo maybe look into why with the combination of minimum 8
         * characters, max 255 characters, all other rules disabled that the
         * password "12345678" is not considered valid.
         */
        PasswordValidator validator = PasswordValidator.buildValidator(forceSpecialChar, forceCapitalLetter, forceNumber, minPasswordLength, maxPasswordLength);
        boolean passwordIsComplexEnough = validator.validatePassword(newPassword);
        if (!passwordIsComplexEnough) {
            messageSummary = messageSummaryFail;
            messageDetail = "Password is not complex enough. The password must have at least one letter and one number be at least " + minPasswordLength + " characters in length.";
            logger.info(messageDetail);
            return new PasswordChangeAttemptResponse(false, messageSummary, messageDetail);
        }
        user.setEncryptedPassword(PasswordEncryption.getInstance().encrypt(newPassword));
        DataverseUser savedUser = dataverseUserService.save(user);
        if (savedUser != null) {
            messageSummary = messageSummarySuccess;
            messageDetail = messageDetailSuccess;
            boolean tokenDeleted = deleteToken(token);
            if (!tokenDeleted) {
                // suboptimal but when it expires it should be deleted
                logger.info("token " + token + " for user id " + user.getId() + " was not deleted");
            }
            String fromAddress = "do-not-reply@dataverse.org";
            String toAddress = user.getEmail();
            String subject = "Dataverse Password Reset Successfully Changed";

            String messageBody = "Hi " + user.getDisplayName() + ",\n\n"
                    + "Your Dataverse account password was successfully changed.\n\n"
                    + "Please contact us if you did not request this password reset or need further help.\n\n"
                    + "Thank you,\n\n"
                    + "Dataverse Support Team";
            mailService.sendMail(fromAddress, toAddress, subject, messageBody);
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
