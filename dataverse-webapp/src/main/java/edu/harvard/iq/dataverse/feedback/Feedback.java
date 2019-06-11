package edu.harvard.iq.dataverse.feedback;

import javax.json.Json;
import javax.json.JsonObjectBuilder;

public class Feedback {

    private final String fromEmail;
    private final String toEmail;
    private final String subject;
    private final String body;

    public Feedback(String fromEmail, String toEmail, String subject, String body) {
        this.fromEmail = fromEmail;
        this.toEmail = toEmail;
        this.subject = subject;
        this.body = body;
    }

    public String getFromEmail() {
        return fromEmail;
    }

    public String getToEmail() {
        return toEmail;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    @Override
    public String toString() {
        return "Feedback{" + "fromEmail=" + fromEmail + ", toEmail=" + toEmail + ", subject=" + subject + ", body=" + body + '}';
    }

    public JsonObjectBuilder toJsonObjectBuilder() {
        return Json.createObjectBuilder()
                .add("fromEmail", fromEmail)
                .add("toEmail", toEmail)
                .add("subject", subject)
                .add("body", body);
    }

}
