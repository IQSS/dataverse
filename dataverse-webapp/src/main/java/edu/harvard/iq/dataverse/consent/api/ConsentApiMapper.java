package edu.harvard.iq.dataverse.consent.api;

import edu.harvard.iq.dataverse.persistence.consent.Consent;
import edu.harvard.iq.dataverse.persistence.consent.ConsentAction;
import edu.harvard.iq.dataverse.persistence.consent.ConsentDetails;

import javax.ejb.Stateless;
import java.util.List;
import java.util.stream.Collectors;

@Stateless
public class ConsentApiMapper {

    public ConsentApiDto consentToConsentApiDto(Consent consent) {


        List<ConsentDetailsApiDto> consentDetails = consent.getConsentDetails().stream()
                .map(this::consentDetailsToConsentDetailsDto)
                .collect(Collectors.toList());

        List<ConsentActionApiDto> consentActionDtos = consent.getConsentActions().stream()
                .map(this::consentActionToConsentActionDto)
                .collect(Collectors.toList());

        return new ConsentApiDto(consent.getId(),
                                 consent.getName(),
                                 consent.getDisplayOrder(),
                                 consent.isRequired(),
                                 consent.isHidden(),
                                 consentDetails,
                                 consentActionDtos);
    }

    public Consent consentApiDtoToConsent(ConsentApiDto consentApiDto) {
        Consent consent = new Consent(consentApiDto.getName(),
                                      consentApiDto.getDisplayOrder(),
                                      consentApiDto.isRequired(),
                                      consentApiDto.isHidden());

        List<ConsentDetails> freshConsentDetails = consentApiDto.getConsentDetails().stream()
                .map(cons -> consentDetailsApiDtoToConsentDetails(cons, consent))
                .collect(Collectors.toList());

        List<ConsentAction> freshConsentActions = consentApiDto.getConsentActions().stream()
                .map(cons -> consentActionApiDtoToConsentAction(cons, consent))
                .collect(Collectors.toList());

        consent.getConsentDetails().addAll(freshConsentDetails);
        consent.getConsentActions().addAll(freshConsentActions);

        return consent;
    }

    public Consent updateAllowedProperties(ConsentApiDto updatedConsent, Consent originalConsent) {
        originalConsent.setHidden(updatedConsent.isHidden());
        originalConsent.setDisplayOrder(updatedConsent.getDisplayOrder());
        originalConsent.setRequired(updatedConsent.isRequired());

        List<ConsentDetails> addedConsentDetails = mapNewConsentDetails(updatedConsent, originalConsent);

        List<ConsentAction> freshConsentActions = mapNewConsentActions(updatedConsent, originalConsent);


        originalConsent.getConsentDetails().addAll(addedConsentDetails);

        updateConsentActions(updatedConsent, originalConsent);
        originalConsent.getConsentActions().removeIf(consentAction -> !isConsentActionPresent(updatedConsent,
                                                                                              consentAction));
        originalConsent.getConsentActions().addAll(freshConsentActions);

        return originalConsent;
    }


    // -------------------- PRIVATE --------------------

    private List<ConsentAction> mapNewConsentActions(ConsentApiDto updatedConsent, Consent originalConsent) {
        return updatedConsent.getConsentActions().stream()
                .filter(updatedCons -> updatedCons.getId() == null)
                .map(updatedCons -> consentActionApiDtoToConsentAction(updatedCons, originalConsent))
                .collect(Collectors.toList());
    }

    private List<ConsentDetails> mapNewConsentDetails(ConsentApiDto updatedConsent, Consent originalConsent) {
        return updatedConsent.getConsentDetails().stream()
                .filter(updatedCons -> updatedCons.getId() == null)
                .map(updatedCons -> consentDetailsApiDtoToConsentDetails(updatedCons, originalConsent))
                .collect(Collectors.toList());
    }

    private ConsentDetailsApiDto consentDetailsToConsentDetailsDto(ConsentDetails consentDetails) {
        return new ConsentDetailsApiDto(consentDetails.getId(), consentDetails.getLanguage(), consentDetails.getText());
    }

    private ConsentActionApiDto consentActionToConsentActionDto(ConsentAction consentAction) {
        return new ConsentActionApiDto(consentAction.getId(),
                                       consentAction.getConsentActionType(),
                                       consentAction.getActionOptions());
    }

    private ConsentDetails consentDetailsApiDtoToConsentDetails(ConsentDetailsApiDto updatedConsentDetails, Consent detailsOwner) {
        return new ConsentDetails(detailsOwner, updatedConsentDetails.getLanguage(), updatedConsentDetails.getText());
    }

    private ConsentAction consentActionApiDtoToConsentAction(ConsentActionApiDto updatedConsentAction, Consent actionOwner) {

        return new ConsentAction(actionOwner,
                                 updatedConsentAction.getConsentActionType(),
                                 updatedConsentAction.getActionOptions());
    }

    private void updateConsentActions(ConsentApiDto updatedConsent, Consent originalConsent) {
        for (ConsentActionApiDto updatedConsentAction : updatedConsent.getConsentActions()) {
            if (updatedConsentAction.getId() != null) {

                for (ConsentAction originalConsAction : originalConsent.getConsentActions()) {
                    if (updatedConsentAction.getId().equals(originalConsAction.getId())) {
                        updateConsentAction(updatedConsentAction, originalConsAction);
                    }

                }

            }
        }
    }

    private ConsentAction updateConsentAction(ConsentActionApiDto updatedConsentAction, ConsentAction originalAction) {

        originalAction.setActionOptions(updatedConsentAction.getActionOptions());
        originalAction.setConsentActionType(updatedConsentAction.getConsentActionType());

        return originalAction;
    }

    private boolean isConsentActionPresent(ConsentApiDto updatedConsent, ConsentAction consentAction) {
        return updatedConsent.getConsentActions().stream()
                .filter(consAction -> consAction.getId() != null)
                .anyMatch(consAction -> consAction.getId().equals(consentAction.getId()));
    }

}
