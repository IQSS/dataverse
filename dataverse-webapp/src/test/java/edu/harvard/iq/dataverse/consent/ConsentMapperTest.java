package edu.harvard.iq.dataverse.consent;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.persistence.consent.AcceptedConsent;
import edu.harvard.iq.dataverse.persistence.consent.Consent;
import edu.harvard.iq.dataverse.persistence.consent.ConsentDetails;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

class ConsentMapperTest {

    private ConsentMapper consentMapper = new ConsentMapper();

    private static final Locale PREFERRED_LOCALE = Locale.CHINA;

    @Test
    public void consentToConsentDto_onlyEnglishRequiredConsent() {
        //given
        Consent cosnent = prepareTestConsents().get(0);

        //when
        ConsentDto preparedConsent = consentMapper.consentToConsentDto(cosnent, Locale.CHINA);

        //then
        Assertions.assertAll(() -> Assertions.assertEquals(cosnent.getName(),
                                                           preparedConsent.getName()),
                             () -> Assertions.assertEquals(cosnent.getConsentDetails().get(0).getText(),
                                                           preparedConsent.getConsentDetails().getText()),
                             () -> Assertions.assertEquals(cosnent.isRequired(),
                                                           preparedConsent.isRequired()));

    }

    @Test
    public void consentToConsentDto_onlyEnglishPolishRequiredConsent() {
        //given
        Consent cosnent = prepareTestConsents().get(1);

        //when
        ConsentDto preparedConsent = consentMapper.consentToConsentDto(cosnent, Locale.CHINA);

        //then
        Assertions.assertAll(() -> Assertions.assertEquals(cosnent.getName(),
                                                           preparedConsent.getName()),
                             () -> Assertions.assertEquals(cosnent.getConsentDetails().get(1).getText(),
                                                           preparedConsent.getConsentDetails().getText()),
                             () -> Assertions.assertEquals(cosnent.isRequired(),
                                                           preparedConsent.isRequired()));

    }

    @Test
    public void consentToConsentDto_onlyEnglishNonRequiredConsent() {
        //given
        Consent cosnent = prepareTestConsents().get(2);

        //when
        ConsentDto preparedConsent = consentMapper.consentToConsentDto(cosnent, Locale.CHINA);

        //then
        Assertions.assertAll(() -> Assertions.assertEquals(cosnent.getName(),
                                                           preparedConsent.getName()),
                             () -> Assertions.assertEquals(cosnent.getConsentDetails().get(0).getText(),
                                                           preparedConsent.getConsentDetails().getText()),
                             () -> Assertions.assertEquals(cosnent.isRequired(),
                                                           preparedConsent.isRequired()));

    }

    @Test
    public void consentDtoToAcceptedConsent_onlyAcceptedConsent() {
        //given
        ConsentDto consentDto = prepareTestDtoConsents().get(0);
        AuthenticatedUser authenticatedUser = new AuthenticatedUser();

        //when
        AcceptedConsent acceptedConsents =  consentMapper.consentDtoToAcceptedConsent(consentDto, authenticatedUser);

        //then
        Assertions.assertAll(() -> Assertions.assertEquals(consentDto.getName(), acceptedConsents.getName()),
                             () -> Assertions.assertEquals(consentDto.getConsentDetails().getText(), acceptedConsents.getText()),
                             () -> Assertions.assertEquals(authenticatedUser.getAcceptedConsents().get(0).getName(), consentDto.getName()));

    }

    @Test
    public void consentDtoToAcceptedConsent_onlyNonAcceptedConsent() {
        //given
        ConsentDto consentDto = prepareTestDtoConsents().get(1);
        AuthenticatedUser authenticatedUser = new AuthenticatedUser();

        //when
        AcceptedConsent acceptedConsents =  consentMapper.consentDtoToAcceptedConsent(consentDto, authenticatedUser);

        //then
        Assertions.assertAll(() -> Assertions.assertEquals(consentDto.getName(), acceptedConsents.getName()),
                             () -> Assertions.assertEquals(consentDto.getConsentDetails().getText(), acceptedConsents.getText()),
                             () -> Assertions.assertEquals(authenticatedUser.getAcceptedConsents().get(0).getName(), consentDto.getName()));

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

        Consent nonRequiredEnglish = new Consent("nonRequiredEnglish", 0, false, false);
        nonRequiredEnglish.setId(3L);
        ConsentDetails nonReqEnglish = new ConsentDetails(nonRequiredEnglish, Locale.ENGLISH, "non required consent");
        nonReqEnglish.setId(4L);
        nonRequiredEnglish.getConsentDetails().add(nonReqEnglish);

        return Lists.newArrayList(requiredEnglish, requiredPolEng, nonRequiredEnglish);
    }
}