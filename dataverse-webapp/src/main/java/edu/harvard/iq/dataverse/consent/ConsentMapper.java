package edu.harvard.iq.dataverse.consent;

import edu.harvard.iq.dataverse.persistence.consent.AcceptedConsent;
import edu.harvard.iq.dataverse.persistence.consent.Consent;
import edu.harvard.iq.dataverse.persistence.consent.ConsentAction;
import edu.harvard.iq.dataverse.persistence.consent.ConsentDetails;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;

import javax.ejb.Stateless;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Stateless
public class ConsentMapper {

    // -------------------- LOGIC --------------------

    /**
     * Maps entity to dto with Locale filtering since we only need to display one language version of consent to user.
     */
    public ConsentDto consentToConsentDto(Consent consent, Locale consentLocale) {

        ConsentDetails consentWithUserLocale = consent.getConsentDetails().stream()
                .filter(consentDetails -> consentDetails.getLanguage().equals(consentLocale))
                .findFirst()
                .orElseGet(() -> consent.getConsentDetails().stream()
                        .filter(consentDetails -> consentDetails.getLanguage().equals(Locale.ENGLISH))
                        .findFirst()
                        .orElseThrow(() -> new ConsentMissingException(
                                "Could not find english version of consent details for consent id: " + consent.getId())));

        ConsentDetailsDto consentDetailsDto = consentDetailsToConsentDetailsDto(consentWithUserLocale);


        ConsentDto consentDto = new ConsentDto(consent.getId(),
                                               consent.getName(),
                                               consentDetailsDto,
                                               consent.getDisplayOrder(),
                                               consent.isRequired());

        List<ConsentActionDto> consentActionDtos = consent.getConsentActions().stream()
                .map(consentAction -> consentActionToConsentActionDto(consentAction, consentDto))
                .collect(Collectors.toList());

        consentDto.getConsentActions().addAll(consentActionDtos);

        return consentDto;
    }

    public AcceptedConsent consentDtoToAcceptedConsent(ConsentDto consentDto, AuthenticatedUser user) {

        AcceptedConsent acceptedConsent = new AcceptedConsent(consentDto.getName(),
                                                              consentDto.getConsentDetails().getLanguage(),
                                                              consentDto.getConsentDetails().getText(),
                                                              consentDto.isRequired(),
                                                              user);
        user.getAcceptedConsents().add(acceptedConsent);

        return acceptedConsent;
    }

    // -------------------- PRIVATE --------------------

    private ConsentDetailsDto consentDetailsToConsentDetailsDto(ConsentDetails consentDetails) {
        return new ConsentDetailsDto(consentDetails.getId(), consentDetails.getLanguage(), consentDetails.getText());
    }

    private ConsentActionDto consentActionToConsentActionDto(ConsentAction consentAction, ConsentDto consentDto) {
        return new ConsentActionDto(consentAction.getId(),
                                    consentAction.getConsentActionType(),
                                    consentAction.getActionOptions(),
                                    consentDto);
    }
}
