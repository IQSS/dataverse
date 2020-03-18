package edu.harvard.iq.dataverse.consent.api;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.consent.ConsentDao;
import edu.harvard.iq.dataverse.persistence.consent.Consent;
import edu.harvard.iq.dataverse.persistence.consent.ConsentAction;
import edu.harvard.iq.dataverse.persistence.consent.ConsentActionType;
import edu.harvard.iq.dataverse.persistence.consent.ConsentDetails;
import io.vavr.control.Option;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ConsentApiServiceTest {

    @Mock
    private ConsentDao consentDao;

    @Mock
    private ConsentApiMapper consentMapper;

    @Mock
    private ConsentValidator consentValidator;

    @InjectMocks
    private ConsentApiService consentApiService;

    // -------------------- TESTS --------------------

    @Test
    public void listAvailableConsents() {
        //given
        Consent consent = prepareTestConsent();

        //when
        when(consentDao.findConsents()).thenReturn(Lists.newArrayList(consent));
        when(consentMapper.consentToConsentApiDto(consent)).thenAnswer(Answers.RETURNS_MOCKS);
        consentApiService.listAvailableConsents();

        //when
        verify(consentDao, times(1)).findConsents();
        verify(consentMapper, times(1)).consentToConsentApiDto(any());

    }

    @Test
    public void fetchApiConsent() {
        //given
        Consent consent = prepareTestConsent();

        //when
        when(consentDao.findConsent("testConsent")).thenReturn(Option.of(consent));
        when(consentMapper.consentToConsentApiDto(consent)).thenAnswer(Answers.RETURNS_MOCKS);
        consentApiService.fetchApiConsent("testConsent");

        //when
        verify(consentDao, times(1)).findConsent("testConsent");
        verify(consentMapper, times(1)).consentToConsentApiDto(consent);
    }

    @Test
    public void fetchConsent() {
        //given
        Consent consent = prepareTestConsent();

        //when
        when(consentDao.findConsent("testConsent")).thenReturn(Option.of(consent));
        Option<Consent> testConsent = consentApiService.fetchConsent("testConsent");

        //when
        verify(consentDao, times(1)).findConsent("testConsent");
        assertEquals(consent, testConsent.getOrNull());

    }

    @Test
    public void validateUpdatedConsent() {
        //given
        Consent consent = prepareTestConsent();

        //when
        when(consentValidator.validateConsentEditing(any(), any())).thenReturn(new ArrayList<>());
        consentApiService.validateUpdatedConsent(null, consent);

        //when
        verify(consentValidator, times(1)).validateConsentEditing(null, consent);
    }

    @Test
    public void validateCreatedConsent() {
        //when
        when(consentValidator.validateConsentCreation(any())).thenReturn(new ArrayList<>());
        consentApiService.validateCreatedConsent(null);

        //when
        verify(consentValidator, times(1)).validateConsentCreation(null);
    }

    @Test
    public void saveEditedConsent() {
        //given
        Consent consent = prepareTestConsent();

        //when
        when(consentMapper.updateAllowedProperties(any(), any())).thenReturn(consent);
        when(consentDao.mergeConsent(consent)).thenReturn(consent);
        consentApiService.saveEditedConsent(null, consent);

        //when
        verify(consentMapper, times(1)).updateAllowedProperties(null, consent);
        verify(consentDao, times(1)).mergeConsent(consent);
    }

    @Test
    public void saveNewConsent() {
        //given
        Consent consent = prepareTestConsent();

        //when
        when(consentMapper.consentApiDtoToConsent(any())).thenReturn(consent);
        when(consentDao.mergeConsent(consent)).thenReturn(consent);
        consentApiService.saveNewConsent(null);

        //when
        verify(consentMapper, times(1)).consentApiDtoToConsent(null);
        verify(consentDao, times(1)).mergeConsent(consent);
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
}