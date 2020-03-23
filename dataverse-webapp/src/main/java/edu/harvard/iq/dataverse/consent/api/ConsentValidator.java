package edu.harvard.iq.dataverse.consent.api;

import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.persistence.consent.Consent;
import edu.harvard.iq.dataverse.persistence.consent.ConsentActionType;
import edu.harvard.iq.dataverse.persistence.consent.ConsentDetails;
import io.vavr.Tuple2;
import io.vavr.control.Option;
import org.apache.commons.collections4.CollectionUtils;

import javax.ejb.Stateless;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Stateless
public class ConsentValidator {

    private static final String SEND_NEWSLETTER_PATTERN = "(\"email\"):(\"((.+)@(.+))\")"; //{"email":"test@gmail.com"}

    /**
     * Checks whether consent was correctly edited.
     *
     * @return List of errors if there are any.
     */
    public List<String> validateConsentEditing(ConsentApiDto consentApiDto, Consent consent) {
        ArrayList<String> errors = new ArrayList<>();

        validateName(consentApiDto.getName(), consent.getName())
                .peek(errors::add);

        errors.addAll(validateEditedConsentDetails(consentApiDto.getConsentDetails(), consent.getConsentDetails()));

        validateConsentActions(consentApiDto.getConsentActions())
                .peek(errors::add);

        return errors;
    }

    public List<String> validateConsentCreation(ConsentApiDto consentApiDto) {
        ArrayList<String> errors = new ArrayList<>();

        if (consentApiDto.getName().isEmpty()) {
            errors.add(BundleUtil.getStringFromBundle("consent.validation.emptyName"));
        }

        errors.addAll(validateNewConsentDetails(consentApiDto.getConsentDetails()));

        validateConsentActions(consentApiDto.getConsentActions())
                .peek(errors::add);

        return errors;
    }

    // -------------------- PRIVATE --------------------

    private Option<String> validateName(String editedName, String originalName) {
        if (!editedName.equals(originalName)) {
            return Option.of(BundleUtil.getStringFromBundle("consent.validation.notEqualNames"));
        }

        return Option.none();
    }

    private List<String> validateEditedConsentDetails(List<ConsentDetailsApiDto> editedConsentDetails, List<ConsentDetails> originalConsentDetails) {
        ArrayList<String> errors = new ArrayList<>();

        checkIfConsentDetailsWereCorrectlyEdited(editedConsentDetails, originalConsentDetails)
                .peek(errors::add);

        if (isEditedConsentsContainsMissingConsents(editedConsentDetails, originalConsentDetails)) {
            errors.add(BundleUtil.getStringFromBundle("consent.validation.consentMissing"));
        }

        if (isConsentsContainsDuplicatedLocale(editedConsentDetails, originalConsentDetails)) {
            errors.add(BundleUtil.getStringFromBundle("consent.validation.duplicatedLanguage"));
        }

        if (isFreshConsentContainsDefaultLanguage(editedConsentDetails)){
            errors.add(BundleUtil.getStringFromBundle("consent.validation.missingDefaultLanguage"));
        }

        for (ConsentDetailsApiDto freshConsent : editedConsentDetails) {
            String missingTextErrorMsg = BundleUtil.getStringFromBundle("consent.validation.missingConsentText");

            if (freshConsent.getText().isEmpty() && !errors.contains(missingTextErrorMsg)) {
                errors.add(missingTextErrorMsg);
            }
        }

        return errors;
    }

    private List<String> validateNewConsentDetails(List<ConsentDetailsApiDto> editedConsentDetails) {
        ArrayList<String> errors = new ArrayList<>();

        if (isFreshConsentContainsDuplicatedLocale(editedConsentDetails)) {
            errors.add(BundleUtil.getStringFromBundle("consent.validation.duplicatedLanguage"));
        }

        if (isFreshConsentContainsDefaultLanguage(editedConsentDetails)){
            errors.add(BundleUtil.getStringFromBundle("consent.validation.missingDefaultLanguage"));
        }

        for (ConsentDetailsApiDto freshConsent : editedConsentDetails) {
            String missingTextErrorMsg = BundleUtil.getStringFromBundle("consent.validation.missingConsentText");

            if (freshConsent.getText().isEmpty() && !errors.contains(missingTextErrorMsg)) {
                errors.add(missingTextErrorMsg);
            }
        }

        return errors;
    }

    private Option<String> checkIfConsentDetailsWereCorrectlyEdited(List<ConsentDetailsApiDto> editedConsentDetails, List<ConsentDetails> originalConsentDetails) {

        editedConsentDetails.sort(Comparator.comparing(cons -> cons.getId() == null ? Long.MAX_VALUE : cons.getId()));
        originalConsentDetails.sort(Comparator.comparing(ConsentDetails::getId));

        io.vavr.collection.List<Tuple2<ConsentDetailsApiDto, ConsentDetails>> zippedConsents = io.vavr.collection.List
                .ofAll(editedConsentDetails)
                .zip(originalConsentDetails);

        return zippedConsents
                .map(consents -> validateConsentDetail(consents._1(), consents._2()))
                .filter(Option::isDefined)
                .getOrElse(Option.none());
    }

    private Option<String> validateConsentDetail(ConsentDetailsApiDto editedConsentDetail, ConsentDetails originalConsentDetail) {
        if (!editedConsentDetail.getText().equals(originalConsentDetail.getText()) ||
                !editedConsentDetail.getLanguage().equals(originalConsentDetail.getLanguage())) {

            return Option.of(BundleUtil.getStringFromBundle("consent.validation.editedConsent"));
        }

        return Option.none();
    }

    private boolean isFreshConsentContainsDuplicatedLocale(List<ConsentDetailsApiDto> freshConsents) {
        return !freshConsents.stream()
                .map(ConsentDetailsApiDto::getLanguage)
                .allMatch(new HashSet<>()::add);
    }

    private boolean isEditedConsentsContainsMissingConsents(List<ConsentDetailsApiDto> freshConsents, List<ConsentDetails> originalConsents) {
        List<Long> editedConsentsIds = freshConsents.stream()
                .filter(consentDetailsApiDto -> consentDetailsApiDto.getId() != null)
                .map(ConsentDetailsApiDto::getId)
                .collect(Collectors.toList());

        List<Long> originalConsentsIds = originalConsents.stream()
                .map(ConsentDetails::getId)
                .collect(Collectors.toList());

        return !CollectionUtils.containsAll(editedConsentsIds, originalConsentsIds);
    }

    private boolean isConsentsContainsDuplicatedLocale(List<ConsentDetailsApiDto> freshConsents, List<ConsentDetails> originalConsents) {
        List<Long> originalConsentIds = originalConsents.stream()
                .map(ConsentDetails::getId)
                .collect(Collectors.toList());

        List<Locale> freshLanguages = freshConsents.stream()
                .filter(cons -> !originalConsentIds.contains(cons.getId()))
                .map(ConsentDetailsApiDto::getLanguage)
                .collect(Collectors.toList());

        List<Locale> originalLanguages = originalConsents.stream()
                .map(ConsentDetails::getLanguage)
                .collect(Collectors.toList());

        return CollectionUtils.containsAny(freshLanguages, originalLanguages);
    }

    private boolean isFreshConsentContainsDefaultLanguage(List<ConsentDetailsApiDto> freshConsents) {
        return freshConsents.stream()
                .noneMatch(cons -> cons.getLanguage().equals(Locale.ENGLISH));
    }

    private Option<String> validateConsentActions(List<ConsentActionApiDto> editedConsentActions) {
        for (ConsentActionApiDto editedConsentAction : editedConsentActions) {

            if (editedConsentAction.getConsentActionType().equals(ConsentActionType.SEND_NEWSLETTER_EMAIL) &&
                    !Pattern.compile(SEND_NEWSLETTER_PATTERN).matcher(editedConsentAction.getActionOptions()).find()) {
                return Option.of(BundleUtil.getStringFromBundle("consent.validation.actionOptionsFail") + ConsentActionType.SEND_NEWSLETTER_EMAIL.toString());
            }
        }

        return Option.none();
    }
}
