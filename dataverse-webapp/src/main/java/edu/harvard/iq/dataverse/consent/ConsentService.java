package edu.harvard.iq.dataverse.consent;

import edu.harvard.iq.dataverse.consent.action.ConsentActionFactory;
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
    private ConsentActionFactory consentActionFactory;

    // -------------------- CONSTRUCTORS --------------------

    @Deprecated
    public ConsentService() {
    }

    @Inject
    public ConsentService(ConsentDao consentDao, ConsentMapper consentMapper, ConsentActionFactory consentActionFactory) {
        this.consentDao = consentDao;
        this.consentMapper = consentMapper;
        this.consentActionFactory = consentActionFactory;
    }

    // -------------------- LOGIC --------------------

    public List<ConsentDto> prepareConsentsForView(Locale preferredLocale) {
        List<Consent> consents = consentDao.findNotHiddenConsents();

        return consents.stream()
                .map(consent -> consentMapper.consentToConsentDto(consent, preferredLocale))
                .sorted(Comparator.comparing(ConsentDto::getDisplayOrder))
                .collect(Collectors.toList());
    }

    /**
     * Executes actions that were associated with consents if there were any and saves consents that were accepted.
     */
    public List<AcceptedConsent> executeActionsAndSaveAcceptedConsents(List<ConsentDto> consents, AuthenticatedUser consentAccepter) {
        List<ConsentDto> acceptedConsentsFromView = consents.stream()
                .filter(consentDto -> consentDto.getConsentDetails().isAccepted())
                .collect(Collectors.toList());

        List<ConsentDto> consentsAfterActionExecution = executeConsentActions(acceptedConsentsFromView, consentAccepter);

        List<AcceptedConsent> acceptedConsents = consentsAfterActionExecution.stream()
                .map(consentDto -> consentMapper.consentDtoToAcceptedConsent(consentDto, consentAccepter))
                .collect(Collectors.toList());

        acceptedConsents.forEach(acceptedConsent -> consentDao.saveAcceptedConsent(acceptedConsent));

        return acceptedConsents;
    }

    // -------------------- PRIVATE --------------------

    private List<ConsentDto> executeConsentActions(List<ConsentDto> consents, AuthenticatedUser registeredUser) {
        consents.stream()
                .flatMap(consentDto -> consentDto.getConsentActions().stream())
                .forEach(consentActionDto -> consentActionFactory
                        .retrieveAction(consentActionDto.getConsentActionType(), registeredUser)
                        .executeAction(consentActionDto));

        return consents;
    }
}
