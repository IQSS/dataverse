package edu.harvard.iq.dataverse.consent.action;

import com.github.sleroy.fakesmtp.model.EmailModel;
import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import edu.harvard.iq.dataverse.consent.ConsentActionDto;
import edu.harvard.iq.dataverse.consent.ConsentDetailsDto;
import edu.harvard.iq.dataverse.consent.ConsentDto;
import edu.harvard.iq.dataverse.mail.FakeSmtpServerUtil;
import edu.harvard.iq.dataverse.persistence.consent.Consent;
import edu.harvard.iq.dataverse.persistence.consent.ConsentAction;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(Arquillian.class)
@Transactional(TransactionMode.ROLLBACK)
public class SendNewsletterEmailActionIT extends WebappArquillianDeployment {

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    @Inject
    private ConsentActionFactory consentActionFactory;

    @Test
    public void shouldSendNotificationEmail() {
        //given
        Consent consent = em.find(Consent.class, 1L);
        ConsentAction sendNewsletterAction = consent.getConsentActions().get(0);

        ConsentDetailsDto consentDetailsDto = new ConsentDetailsDto(1L,
                                                                    Locale.ENGLISH,
                                                                    sendNewsletterAction.getConsent().getConsentDetails().get(0).getText());
        ConsentDto consentDto = new ConsentDto(1L, "", consentDetailsDto, 0, true);
        ConsentActionDto consentActionDto = new ConsentActionDto(sendNewsletterAction.getId(),
                                                                 sendNewsletterAction.getConsentActionType(),
                                                                 sendNewsletterAction.getActionOptions(),
                                                                 consentDto);

        //when
        consentActionFactory.retrieveAction(consentActionDto.getConsentActionType(), em.find(AuthenticatedUser.class, 2L))
                .executeAction(consentActionDto);

        //then
        EmailModel emailSent = FakeSmtpServerUtil.waitForEmailSentTo(smtpServer, "test@gmail.com");

        assertEquals("test@gmail.com", emailSent.getTo());
        assertEquals("Root New consent for personal data processing", emailSent.getSubject());
        assertTrue(emailSent.getEmailStr().contains("Consent text: "+ consentDetailsDto.getText()));
        assertTrue(emailSent.getEmailStr().contains("First name: firstname"));
        assertTrue(emailSent.getEmailStr().contains("Last name: lastname"));
        assertTrue(emailSent.getEmailStr().contains("E-mail: test@gmail.com"));

    }
}