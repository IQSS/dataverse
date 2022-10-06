package edu.harvard.iq.dataverse.consent.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.harvard.iq.dataverse.consent.ConsentActionDto;
import edu.harvard.iq.dataverse.consent.ConsentDetailsDto;
import edu.harvard.iq.dataverse.mail.EmailContent;
import edu.harvard.iq.dataverse.mail.MailService;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import io.vavr.control.Try;

class SendNewsletterEmailAction implements Action{

    private MailService mailService;
    private String repositoryName;
    private AuthenticatedUser user;

    // -------------------- CONSTRUCTORS --------------------

    SendNewsletterEmailAction(MailService mailService, String repositoryName, AuthenticatedUser registeredUser) {
        this.mailService = mailService;
        this.repositoryName = repositoryName;
        this.user = registeredUser;
    }

    // -------------------- LOGIC --------------------

    public void executeAction(ConsentActionDto consentActionDto){
        SendNewsletterEmailContent sendNewsletterEmailContent = parseEmailActionContent(consentActionDto);

        EmailContent emailContent = prepareEmail(repositoryName, consentActionDto, sendNewsletterEmailContent);

        mailService.sendMailAsync(sendNewsletterEmailContent.getSenderEmail(), null, emailContent);
    }

    // -------------------- PRIVATE --------------------

    private SendNewsletterEmailContent parseEmailActionContent(ConsentActionDto consentActionDto) {
        ObjectMapper objectMapper = new ObjectMapper();
        EmailField emailField = Try.of(() -> objectMapper.readValue(consentActionDto.getActionOptions(),
                                                                 EmailField.class))
                .getOrElseThrow(throwable -> new RuntimeException(
                        "There was a problem with parsing consent action with id: " + consentActionDto.getId(),
                        throwable));


        return new SendNewsletterEmailContent(emailField.getEmail(), user.getEmail(), user.getFirstName(), user.getLastName());
    }

    private EmailContent prepareEmail(String repositoryName, ConsentActionDto consentActionDto, SendNewsletterEmailContent actionContent){
        String emailSubject = repositoryName + ": New consent for personal data processing";

        StringBuilder emailBodyConstructor = new StringBuilder();

        ConsentDetailsDto consentDetails = consentActionDto.getOwner().getConsentDetails();
        String consentText = consentDetails.getText();

        String emailBody = emailBodyConstructor.append("Consent text: ")
                .append(consentText)
                .append("\n\n")
                .append("First name: ")
                .append(actionContent.getRecipientFirstName())
                .append("\n")
                .append("Last name: ")
                .append(actionContent.getRecipientLastName())
                .append("\n")
                .append("E-mail: ")
                .append(actionContent.getRecipientEmail())
                .toString();

        return new EmailContent(emailSubject, emailBody, "");
    }
}
