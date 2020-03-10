package edu.harvard.iq.dataverse.consent.action;

import edu.harvard.iq.dataverse.consent.ConsentActionDto;
import edu.harvard.iq.dataverse.consent.ConsentDetailsDto;
import edu.harvard.iq.dataverse.consent.ConsentDto;
import edu.harvard.iq.dataverse.mail.MailService;
import edu.harvard.iq.dataverse.persistence.consent.ConsentActionType;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Locale;

@ExtendWith(MockitoExtension.class)
class SendNewsletterEmailActionTest {

    @Mock
    private MailService mailService;

    @Mock
    private AuthenticatedUser user;

    @InjectMocks
    private SendNewsletterEmailAction newsAction;

    @BeforeEach
    void setUp() {
        user = new AuthenticatedUser();
        user.setFirstName("John");
        user.setLastName("Tester");
    }

    // -------------------- TESTS --------------------

    @Test
    public void executeAction() {
        //given
        ConsentDetailsDto consentDetailsDto = new ConsentDetailsDto(1L, Locale.ENGLISH, "");
        ConsentDto consentDto = new ConsentDto(1L, "", consentDetailsDto, 0, true);
        ConsentActionDto consentActionDto = new ConsentActionDto(1L,
                                                                 ConsentActionType.SEND_NEWSLETTER_EMAIL,
                                                                 prepareActionOptions(),
                                                                 consentDto);

        //when

        newsAction.executeAction(consentActionDto);

        //then
        Mockito.verify(mailService, Mockito.times(1)).sendMailAsync(Mockito.any(), Mockito.any());
    }

    // -------------------- PRIVATE --------------------

    private String prepareActionOptions() {
        return "{\"email\": \"test@gmail.com\"}";
    }
}