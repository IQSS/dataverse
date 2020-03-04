package edu.harvard.iq.dataverse.consent;

import edu.harvard.iq.dataverse.persistence.consent.AcceptedConsent;
import edu.harvard.iq.dataverse.persistence.consent.Consent;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Stateless
public class ConsentService {

    private ConsentDao consentDao;
    private ConsentMapper consentMapper;

    // -------------------- CONSTRUCTORS --------------------

    @Deprecated
    public ConsentService() {
    }

    @Inject
    public ConsentService(ConsentDao consentDao, ConsentMapper consentMapper) {
        this.consentDao = consentDao;
        this.consentMapper = consentMapper;
    }

    // -------------------- LOGIC --------------------

    public List<ConsentDto> prepareConsentsForView(Locale preferredLocale) {
        List<Consent> consents = consentDao.findNotHiddenConsents();

        return consents.stream()
                .map(consent -> consentMapper.consentToConsentDto(consent, preferredLocale))
                .sorted(Comparator.comparing(ConsentDto::getDisplayOrder))
                .collect(Collectors.toList());
    }

    public List<AcceptedConsent> saveAcceptedConsents(List<ConsentDto> consents, AuthenticatedUser consentAccepter){
        List<ConsentDto> acceptedConsentsFromView = consents.stream()
                .filter(consentDto -> consentDto.getConsentDetails().isAccepted())
                .collect(Collectors.toList());

        List<AcceptedConsent> acceptedConsents = acceptedConsentsFromView.stream()
                .map(consentDto -> consentMapper.consentDtoToAcceptedConsent(consentDto, consentAccepter))
                .collect(Collectors.toList());

        acceptedConsents.forEach(acceptedConsent -> consentDao.saveAcceptedConsent(acceptedConsent));

        return acceptedConsents;
    }
}
