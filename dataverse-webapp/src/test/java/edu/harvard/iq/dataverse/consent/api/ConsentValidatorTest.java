package edu.harvard.iq.dataverse.consent.api;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.persistence.consent.Consent;
import edu.harvard.iq.dataverse.persistence.consent.ConsentAction;
import edu.harvard.iq.dataverse.persistence.consent.ConsentActionType;
import edu.harvard.iq.dataverse.persistence.consent.ConsentDetails;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsentValidatorTest {

    private ConsentValidator consentValidator = new ConsentValidator();

    // -------------------- TESTS --------------------

    @Test
    public void validateConsentEditing_WithCorrectEditedConsent() {
        //given
        ConsentApiDto consentApiDto = prepareTestConsentApiDto();
        Consent consent = prepareTestConsent();

        //when
        List<String> errors = consentValidator.validateConsentEditing(consentApiDto, consent);

        //then
        Assert.assertEquals(0, errors.size());
    }

    @Test
    public void validateConsentEditing_WithIncorrectlyEditedConsent() {
        //given
        ConsentApiDto consentApiDto = prepareTestConsentApiDtoWithInvalidProperties();
        Consent consent = prepareTestConsent();

        //when
        List<String> errors = consentValidator.validateConsentEditing(consentApiDto, consent);

        //then
        Assert.assertFalse(errors.isEmpty());
        Assertions.assertAll(() -> assertEquals(6, errors.size()),
                             () -> assertTrue(containsText("Consent names must be equal", errors)),
                             () -> assertTrue(containsText("Consent details cannot be edited!", errors)),
                             () -> assertTrue(containsText("New consent detail text cannot be empty", errors)),
                             () -> assertTrue(containsText("Consent contains duplicated language", errors)),
                             () -> assertTrue(containsText(
                                     "Action options were not correctly filled out for: SEND_NEWSLETTER_EMAIL",
                                     errors)),
                             () -> assertTrue(containsText("There are consents missing", errors)));
    }

    @Test
    public void validateCreatedConsent_WithCorrectConsent() {
        //given
        ConsentApiDto consentApiDto = prepareTestConsentApiDto();

        //when
        List<String> errors = consentValidator.validateConsentCreation(consentApiDto);

        //when
        Assert.assertEquals(0, errors.size());

    }

    @Test
    public void validateCreatedConsent_WithIncorrectConsent() {
        //given
        ConsentApiDto consentApiDto = prepareIncorrectlyCreatedConsentApiDto();

        //when
        List<String> errors = consentValidator.validateConsentCreation(consentApiDto);

        //when
        Assertions.assertAll(() -> assertEquals(5, errors.size()),
                             () -> assertTrue(containsText("Consent name cannot be empty", errors)),
                             () -> assertTrue(containsText("Consent needs to contain english version", errors)),
                             () -> assertTrue(containsText("New consent detail text cannot be empty", errors)),
                             () -> assertTrue(containsText("Consent contains duplicated language", errors)),
                             () -> assertTrue(containsText(
                                     "Action options were not correctly filled out for: SEND_NEWSLETTER_EMAIL",
                                     errors)));

    }

    // -------------------- PRIVATE --------------------

    private boolean containsText(String textToFind, List<String> errors) {
        return errors.stream()
                .anyMatch(error -> error.equals(textToFind));
    }

    private ConsentApiDto prepareTestConsentApiDto() {
        ConsentApiDto cons = new ConsentApiDto(1L,
                                               "testName",
                                               1,
                                               true,
                                               false,
                                               Lists.newArrayList(),
                                               Lists.newArrayList());

        ConsentDetailsApiDto consDetails = new ConsentDetailsApiDto(1L, Locale.ENGLISH, "testCons");
        ConsentActionApiDto consAction = new ConsentActionApiDto(1L,
                                                                 ConsentActionType.SEND_NEWSLETTER_EMAIL,
                                                                 "{\"email\":\"test@gmail.com\"}");

        cons.getConsentDetails().add(consDetails);
        cons.getConsentActions().add(consAction);

        return cons;
    }

    private ConsentApiDto prepareTestConsentApiDtoWithInvalidProperties() {
        ConsentApiDto cons = new ConsentApiDto(1L,
                                               "invalidName",
                                               2,
                                               true,
                                               false,
                                               Lists.newArrayList(),
                                               Lists.newArrayList());

        ConsentDetailsApiDto consDetails = new ConsentDetailsApiDto(2L, Locale.ENGLISH, "");
        ConsentDetailsApiDto consDetails2 = new ConsentDetailsApiDto(null, Locale.ENGLISH, "");
        ConsentActionApiDto consAction = new ConsentActionApiDto(1L,
                                                                 ConsentActionType.SEND_NEWSLETTER_EMAIL,
                                                                 "");

        cons.getConsentDetails().add(consDetails);
        cons.getConsentDetails().add(consDetails2);
        cons.getConsentActions().add(consAction);

        return cons;
    }

    private ConsentApiDto prepareIncorrectlyCreatedConsentApiDto() {
        ConsentApiDto cons = new ConsentApiDto(1L,
                                               "",
                                               2,
                                               true,
                                               false,
                                               Lists.newArrayList(),
                                               Lists.newArrayList());

        ConsentDetailsApiDto consDetails = new ConsentDetailsApiDto(1L, Locale.CHINA, "");
        ConsentDetailsApiDto consDetails2 = new ConsentDetailsApiDto(null, Locale.CHINA, "");
        ConsentActionApiDto consAction = new ConsentActionApiDto(1L,
                                                                 ConsentActionType.SEND_NEWSLETTER_EMAIL,
                                                                 "");

        cons.getConsentDetails().add(consDetails);
        cons.getConsentDetails().add(consDetails2);
        cons.getConsentActions().add(consAction);

        return cons;
    }

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
}