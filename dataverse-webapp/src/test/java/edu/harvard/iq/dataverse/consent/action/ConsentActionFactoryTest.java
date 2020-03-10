package edu.harvard.iq.dataverse.consent.action;

import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.mail.MailService;
import edu.harvard.iq.dataverse.persistence.consent.ConsentActionType;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConsentActionFactoryTest {

    @Mock
    private MailService mailService;

    @Mock
    private DataverseSession dataverseSession;

    @Mock
    private DataverseDao dataverseDao;

    @InjectMocks
    private ConsentActionFactory consentActionFactory;

    @Test
    public void retrieveAction_ForSendNewsletter() {
        //given
        ConsentActionType actionToBeTaken = ConsentActionType.SEND_NEWSLETTER_EMAIL;

        //when
        Dataverse dv = new Dataverse();
        dv.setName("ROOT");
        Mockito.when(dataverseDao.findRootDataverse()).thenReturn(dv);

        Action action = consentActionFactory.retrieveAction(actionToBeTaken, new AuthenticatedUser());

        //then
        Assert.assertEquals(SendNewsletterEmailAction.class, action.getClass());

    }
}