package edu.harvard.iq.dataverse.consent;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.persistence.consent.AcceptedConsent;
import edu.harvard.iq.dataverse.persistence.consent.Consent;
import edu.harvard.iq.dataverse.persistence.consent.ConsentDetails;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Locale;

@ExtendWith(MockitoExtension.class)
class ConsentServiceTest {

    @Mock
    private ConsentDao consentDao;

    private ConsentMapper consentMapper = new ConsentMapper();

    @InjectMocks
    private ConsentService consentService;

    private static final Locale PREFERRED_LOCALE = Locale.CHINA;

    @BeforeEach
    void setUp() {
        consentService = new ConsentService(consentDao, consentMapper);
    }

    @Test
    public void prepareConsentsForView() {
        //given
        List<Consent> testConsents = prepareTestConsents();

        //when
        Mockito.when(consentDao.findNotHiddenConsents()).thenReturn(testConsents);
        List<ConsentDto> preperedConsents = consentService.prepareConsentsForView(Locale.CHINA);

        //then
        Assertions.assertAll(() -> Assertions.assertEquals(testConsents.get(2).getName(),
                                                           preperedConsents.get(0).getName()),
                             () -> Assertions.assertEquals(testConsents.get(2).getConsentDetails().get(0).getText(),
                                                           preperedConsents.get(0).getConsentDetails().getText()),
                             () -> Assertions.assertEquals(testConsents.get(0).getName(),
                                                           preperedConsents.get(1).getName()),
                             () -> Assertions.assertEquals(testConsents.get(0).getConsentDetails().get(0).getText(),
                                                           preperedConsents.get(1).getConsentDetails().getText()),
                             () -> Assertions.assertEquals(testConsents.get(1).getName(),
                                                           preperedConsents.get(2).getName()),
                             () -> Assertions.assertEquals(testConsents.get(1).getConsentDetails().get(1).getText(),
                                                           preperedConsents.get(2).getConsentDetails().getText()));

    }

    @Test
    public void saveAcceptedConsents() {
        //given
        AuthenticatedUser authenticatedUser = new AuthenticatedUser();
        List<ConsentDto> testDtoConsents = prepareTestDtoConsents();

        //when
        List<AcceptedConsent> acceptedConsents = consentService.saveAcceptedConsents(testDtoConsents,
                                                                                     authenticatedUser);

        //then
        Assertions.assertAll(() -> Assertions.assertEquals(1, acceptedConsents.size()),
                             () -> Assertions.assertEquals(testDtoConsents.get(0).getName(),
                                                           acceptedConsents.get(0).getName()),
                             () -> Assertions.assertEquals(testDtoConsents.get(0).getConsentDetails().getText(),
                                                           acceptedConsents.get(0).getText()),
                             () -> Assertions.assertEquals(authenticatedUser.getAcceptedConsents().get(0).getName(),
                                                           acceptedConsents.get(0).getName()));

    }

    // -------------------- PRIVATE --------------------

    private List<ConsentDto> prepareTestDtoConsents() {
        ConsentDetailsDto englishCons = new ConsentDetailsDto(1L, Locale.ENGLISH, "english cons");
        ConsentDto firstConsent = new ConsentDto(1L, "first consent", englishCons, Lists.newArrayList(), 0, true);
        firstConsent.getConsentDetails().setAccepted(true);

        ConsentDetailsDto secondEnglishCons = new ConsentDetailsDto(1L, Locale.ENGLISH, "second english cons");
        ConsentDto secondConsent = new ConsentDto(2L,
                                                  "second consent",
                                                  secondEnglishCons,
                                                  Lists.newArrayList(),
                                                  1,
                                                  true);

        return Lists.newArrayList(firstConsent, secondConsent);
    }

    private List<Consent> prepareTestConsents() {
        Consent requiredEnglish = new Consent("requiredEnglish", 1, true, false);
        requiredEnglish.setId(1L);
        ConsentDetails consentDetails = new ConsentDetails(requiredEnglish, Locale.ENGLISH, "required consent");
        consentDetails.setId(1L);
        requiredEnglish.getConsentDetails().add(consentDetails);

        Consent requiredPolEng = new Consent("requiredPolEng", 2, true, false);
        requiredPolEng.setId(2L);
        ConsentDetails reqPolEngDetails = new ConsentDetails(requiredPolEng, Locale.ENGLISH, "required consent");
        reqPolEngDetails.setId(2L);
        ConsentDetails reqPolEngDetails2 = new ConsentDetails(requiredPolEng, PREFERRED_LOCALE, "wymagana zgoda");
        reqPolEngDetails2.setId(3L);
        requiredPolEng.getConsentDetails().add(reqPolEngDetails);
        requiredPolEng.getConsentDetails().add(reqPolEngDetails2);

        Consent nonRequiredEnglish = new Consent("requiredEnglish", 0, false, false);
        nonRequiredEnglish.setId(3L);
        ConsentDetails nonReqEnglish = new ConsentDetails(nonRequiredEnglish, Locale.ENGLISH, "non required consent");
        nonReqEnglish.setId(4L);
        nonRequiredEnglish.getConsentDetails().add(nonReqEnglish);

        return Lists.newArrayList(requiredEnglish, requiredPolEng, nonRequiredEnglish);
    }
}