package edu.harvard.iq.dataverse.consent.api;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.persistence.consent.Consent;
import edu.harvard.iq.dataverse.persistence.consent.ConsentAction;
import edu.harvard.iq.dataverse.persistence.consent.ConsentActionType;
import edu.harvard.iq.dataverse.persistence.consent.ConsentDetails;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

public class ConsentApiMapperTest {

    private ConsentApiMapper consentApiMapper = new ConsentApiMapper();

    @Test
    public void consentToConsentApiDto() {
        //given
        Consent consent = prepareTestConsent();

        //when
        ConsentApiDto mappedConsent = consentApiMapper.consentToConsentApiDto(consent);

        //then
        Assertions.assertAll(() -> assertEquals(consent.getId(), mappedConsent.getId()),
                             () -> assertEquals(consent.getDisplayOrder(), mappedConsent.getDisplayOrder()),
                             () -> assertEquals(consent.getName(), mappedConsent.getName()),
                             () -> assertEquals(consent.getConsentDetails().get(0).getId(),
                                                mappedConsent.getConsentDetails().get(0).getId()),
                             () -> assertEquals(consent.getConsentDetails().get(0).getLanguage(),
                                                mappedConsent.getConsentDetails().get(0).getLanguage()),
                             () -> assertEquals(consent.getConsentDetails().get(0).getText(),
                                                mappedConsent.getConsentDetails().get(0).getText()),
                             () -> assertEquals(consent.getConsentActions().get(0).getId(),
                                                mappedConsent.getConsentActions().get(0).getId()),
                             () -> assertEquals(consent.getConsentActions().get(0).getActionOptions(),
                                                mappedConsent.getConsentActions().get(0).getActionOptions()),
                             () -> assertEquals(consent.getConsentActions().get(0).getConsentActionType(),
                                                mappedConsent.getConsentActions().get(0).getConsentActionType()));
    }

    @Test
    public void consentApiDtoToConsent() {
        //given
        ConsentApiDto consentApiDto = prepareTestConsentApiDto();
        consentApiDto.setId(null);

        //when
        Consent consent = consentApiMapper.consentApiDtoToConsent(consentApiDto);

        //when
        Assertions.assertAll(() -> assertEquals(consentApiDto.getId(), consent.getId()),
                             () -> assertEquals(consentApiDto.getDisplayOrder(), consent.getDisplayOrder()),
                             () -> assertEquals(consentApiDto.getName(), consent.getName()),
                             () -> assertEquals(consentApiDto.getConsentDetails().get(0).getLanguage(),
                                                consent.getConsentDetails().get(0).getLanguage()),
                             () -> assertEquals(consentApiDto.getConsentDetails().get(0).getText(),
                                                consent.getConsentDetails().get(0).getText()),
                             () -> assertEquals(consentApiDto.getConsentActions().get(0).getActionOptions(),
                                                consent.getConsentActions().get(0).getActionOptions()),
                             () -> assertEquals(consentApiDto.getConsentActions().get(0).getConsentActionType(),
                                                consent.getConsentActions().get(0).getConsentActionType()));
    }

    @Test
    public void updateAllowedProperties() {
        //given
        ConsentApiDto updatedConsent = prepareTestConsentApiDto();
        Consent originalConsent = prepareTestConsent();

        //when
        Consent mappedConsent = consentApiMapper.updateAllowedProperties(updatedConsent, originalConsent);

        //then
        Assertions.assertAll(() -> assertEquals(mappedConsent.isRequired(), updatedConsent.isRequired()),
                             () -> assertEquals(mappedConsent.isHidden(), updatedConsent.isHidden()),
                             () -> assertEquals(updatedConsent.getName(), mappedConsent.getName()),
                             () -> assertEquals(updatedConsent.getConsentActions().size(),
                                                mappedConsent.getConsentActions().size()),
                             () -> assertNotEquals(updatedConsent.getConsentDetails().size(),
                                                   mappedConsent.getConsentDetails().size()),
                             () -> assertNotSame(updatedConsent.getConsentDetails().get(0).getId(),
                                                 mappedConsent.getConsentDetails().get(0).getId()),
                             () -> assertSame(updatedConsent.getConsentDetails().get(0).getLanguage(),
                                              mappedConsent.getConsentDetails().get(0).getLanguage()),
                             () -> assertSame(updatedConsent.getConsentDetails().get(0).getText(),
                                              mappedConsent.getConsentDetails().get(1).getText()),
                             () -> assertSame(updatedConsent.getConsentActions().get(0).getActionOptions(),
                                              mappedConsent.getConsentActions().get(0).getActionOptions()),
                             () -> assertSame(updatedConsent.getConsentActions().get(0).getConsentActionType(),
                                              mappedConsent.getConsentActions().get(0).getConsentActionType())
        );
    }

    // -------------------- PRIVATE --------------------

    private Consent prepareTestConsent() {
        Consent cons = new Consent("testName", 1, true, false);
        cons.setId(1L);
        ConsentDetails consDetails = new ConsentDetails(cons, Locale.ENGLISH, "testCons");
        consDetails.setId(1L);
        ConsentAction consAction = new ConsentAction(cons,
                                                     ConsentActionType.SEND_NEWSLETTER_EMAIL,
                                                     "{\"email\":\"test@gmail.com\"}");
        consAction.setId(1L);

        cons.getConsentDetails().add(consDetails);
        cons.getConsentActions().add(consAction);

        return cons;
    }

    private ConsentApiDto prepareTestConsentApiDto() {
        ConsentApiDto cons = new ConsentApiDto(1L,
                                               "testName",
                                               1,
                                               false,
                                               true,
                                               Lists.newArrayList(),
                                               Lists.newArrayList());

        ConsentDetailsApiDto consDetails = new ConsentDetailsApiDto(null, Locale.ENGLISH, "newCons");
        ConsentActionApiDto consAction = new ConsentActionApiDto(null,
                                                                 ConsentActionType.SEND_NEWSLETTER_EMAIL,
                                                                 "{\"email\":\"new@gmail.com\"}");

        cons.getConsentDetails().add(consDetails);
        cons.getConsentActions().add(consAction);

        return cons;
    }
}