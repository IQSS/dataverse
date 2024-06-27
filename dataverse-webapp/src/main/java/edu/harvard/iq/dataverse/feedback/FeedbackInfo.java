package edu.harvard.iq.dataverse.feedback;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.persistence.DvObject;

import javax.mail.internet.InternetAddress;

/**
 * Context information for feedback provided by user.
 * @param <T> type of the resource getting the feedback
 */
public class FeedbackInfo<T extends DvObject> {
    private T feedbackTarget;
    private FeedbackRecipient recipient;
    private String userEmail;
    private String systemEmail;
    private String messageSubject;
    private String userMessage;
    private String dataverseSiteUrl;
    private String installationBrandName;
    private String supportTeamName;

    // -------------------- GETTERS --------------------

    public T getFeedbackTarget() {
        return feedbackTarget;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public String getSystemEmail() {
        return systemEmail;
    }

    public String getMessageSubject() {
        return messageSubject;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public String getDataverseSiteUrl() {
        return dataverseSiteUrl;
    }

    public String getInstallationBrandName() {
        return installationBrandName;
    }

    public String getSupportTeamName() {
        return supportTeamName;
    }

    public FeedbackRecipient getRecipient() {
        return recipient;
    }

    // -------------------- LOGIC --------------------

    public FeedbackInfo<T> withFeedbackTarget(T feedbackTarget) {
        this.feedbackTarget = feedbackTarget;
        return this;
    }

    public FeedbackInfo<T> withRecipient(FeedbackRecipient recipient) {
        this.recipient = recipient;
        return this;
    }

    public FeedbackInfo<T> withUserEmail(DataverseSession dataverseSession, String fallbackEmail) {
        if (isLoggedIn(dataverseSession)) {
            this.userEmail = loggedInUserEmail(dataverseSession);
        } else {
            this.userEmail = fallbackEmail;
        }
        return this;
    }

    public FeedbackInfo<T> withUserEmail(String userEmail) {
        this.userEmail = userEmail;
        return this;
    }

    public FeedbackInfo<T> withSystemEmail(InternetAddress systemAddress) {
        this.systemEmail = systemAddress.getAddress();
        return this;
    }

    public FeedbackInfo<T> withMessageSubject(String messageSubject) {
        this.messageSubject = messageSubject;
        return this;
    }

    public FeedbackInfo<T> withUserMessage(String userMessage) {
        this.userMessage = userMessage;
        return this;
    }

    public FeedbackInfo<T> withDataverseSiteUrl(String dataverseSiteUrl) {
        this.dataverseSiteUrl = dataverseSiteUrl;
        return this;
    }

    public FeedbackInfo<T> withInstallationBrandName(String installationBrandName) {
        this.installationBrandName = installationBrandName;
        return this;
    }

    public FeedbackInfo<T> withSupportTeamName(String supportTeamName) {
        this.supportTeamName = supportTeamName;
        return this;
    }

    // -------------------- PRIVATE --------------------

    private static boolean isLoggedIn(DataverseSession dataverseSession) {
        if (dataverseSession != null) {
            return dataverseSession.getUser().isAuthenticated();
        }
        return false;
    }

    private static String loggedInUserEmail(DataverseSession dataverseSession) {
        return dataverseSession.getUser().getDisplayInfo().getEmailAddress();
    }
}
