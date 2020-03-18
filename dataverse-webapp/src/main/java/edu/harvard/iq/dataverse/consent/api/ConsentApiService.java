package edu.harvard.iq.dataverse.consent.api;

import edu.harvard.iq.dataverse.consent.ConsentDao;
import edu.harvard.iq.dataverse.persistence.consent.Consent;
import io.vavr.control.Option;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

@Stateless
public class ConsentApiService {

    private ConsentDao consentDao;
    private ConsentApiMapper consentMapper;
    private ConsentValidator consentValidator;

    // -------------------- CONSTRUCTORS --------------------

    @Deprecated
    public ConsentApiService() {
    }

    @Inject
    public ConsentApiService(ConsentDao consentDao, ConsentApiMapper consentMapper, ConsentValidator consentValidator) {
        this.consentDao = consentDao;
        this.consentMapper = consentMapper;
        this.consentValidator = consentValidator;
    }

    // -------------------- LOGIC --------------------

    public List<ConsentApiDto> listAvailableConsents() {
        List<Consent> consentsFound = consentDao.findConsents();

        return consentsFound.stream()
                .map(consent -> consentMapper.consentToConsentApiDto(consent))
                .collect(Collectors.toList());
    }

    public Option<ConsentApiDto> fetchApiConsent(String alias) {
        Option<Consent> consent = consentDao.findConsent(alias);

        return consent.map(con -> consentMapper.consentToConsentApiDto(con));
    }

    public Option<Consent> fetchConsent(String alias) {
        return consentDao.findConsent(alias);
    }

    public List<String> validateUpdatedConsent(ConsentApiDto updatedConsent, Consent originalConsent){
       return consentValidator.validateConsentEditing(updatedConsent, originalConsent);
    }

    public List<String> validateCreatedConsent(ConsentApiDto updatedConsent){
       return consentValidator.validateConsentCreation(updatedConsent);
    }

    public Consent saveNewConsent(ConsentApiDto freshConsent) {
        Consent consent = consentMapper.consentApiDtoToConsent(freshConsent);

        return consentDao.mergeConsent(consent);
    }

    public Consent saveEditedConsent(ConsentApiDto updatedConsent, Consent originalConsent) {
        Consent remappedConsent = consentMapper.updateAllowedProperties(updatedConsent, originalConsent);

        return consentDao.mergeConsent(remappedConsent);
    }
}
