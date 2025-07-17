package edu.harvard.iq.dataverse.feedback;

import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;

import edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder;

public class Feedback {

    private final String fromEmail;
    private final String toEmail;
    private final String ccEmail;
    private final String subject;
    private final String body;

    public Feedback(String fromEmail, String toEmail, String ccEmail, String subject, String body) {
        this.fromEmail = fromEmail;
        this.toEmail = toEmail;
        this.ccEmail=ccEmail;
        this.subject = subject;
        this.body = body;
    }

    public String getFromEmail() {
        return fromEmail;
    }

    public String getToEmail() {
        return toEmail;
    }
    
    public String getCcEmail() {
        return ccEmail;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    @Override
    public String toString() {
        return "Feedback{" + "fromEmail=" + fromEmail + ", toEmail=" + toEmail + ", ccEmail=" + ccEmail + ", subject=" + subject + ", body=" + body + '}';
    }

    public JsonObjectBuilder toJsonObjectBuilder() {
        return new NullSafeJsonBuilder()
                .add("fromEmail", fromEmail)
                .add("toEmail", toEmail)
                .add("ccEmail", ccEmail)
                .add("subject", subject)
                .add("body", body);
    }

    public JsonObjectBuilder toLimitedJsonObjectBuilder() {
        return new NullSafeJsonBuilder()
                .add("fromEmail", fromEmail)
                .add("subject", subject)
                .add("body", body);
    }
}
