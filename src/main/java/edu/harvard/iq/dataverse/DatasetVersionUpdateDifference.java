package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.license.License;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Compares the values that can be affected by PUT /versions/:draft to detect no-op updates.
 *
 * This reflects the endpoint's two behaviors:
 *
 * - Existing draft: only metadata fields and terms/license are replaced.
 * - Published version: a new draft is created, so version-level fields can also be replaced.
 *
 * This is deliberately separate from {@link DatasetVersionDifference}, which is a presentation-oriented comparison
 * and uses display values. This class compares raw values instead.
 *
 * @author Vera Clemens (ZB MED)
 */
public final class DatasetVersionUpdateDifference {

    private final DatasetVersion incomingVersion;
    private final DatasetVersion currentVersion;

    public DatasetVersionUpdateDifference(DatasetVersion incomingVersion, DatasetVersion currentVersion) {
        this.incomingVersion = incomingVersion;
        this.currentVersion = currentVersion;
    }

    public boolean isEmpty() {
        if (!metadataIsEqual() || !termsAreEqual()) {
            return false;
        }

        // Updating an existing draft only replaces metadata and terms/license, while
        // creating a new draft also replaces some version-level values
        return DatasetVersion.VersionState.DRAFT.equals(currentVersion.getVersionState()) || versionFieldsAreEqual();
    }

    private boolean metadataIsEqual() {
        return fieldsAreEqual(incomingVersion.getDatasetFields(), currentVersion.getDatasetFields());
    }

    private boolean fieldsAreEqual(List<DatasetField> incomingFields, List<DatasetField> currentFields) {
        List<DatasetField> incomingNonEmptyFields = nonEmptyFieldsSortedByType(incomingFields);
        List<DatasetField> currentNonEmptyFields = nonEmptyFieldsSortedByType(currentFields);
        if (incomingNonEmptyFields.size() != currentNonEmptyFields.size()) {
            return false;
        }

        for (int i = 0; i < incomingNonEmptyFields.size(); i++) {
            if (!fieldsAreEqual(incomingNonEmptyFields.get(i), currentNonEmptyFields.get(i))) {
                return false;
            }
        }
        return true;
    }

    private List<DatasetField> nonEmptyFieldsSortedByType(List<DatasetField> fields) {
        List<DatasetField> nonEmptyFields = new ArrayList<>();
        for (DatasetField field : fields) {
            if (!field.isEmpty()) {
                nonEmptyFields.add(field);
            }
        }
        nonEmptyFields.sort(Comparator.comparing(field -> field.getDatasetFieldType().getName()));
        return nonEmptyFields;
    }

    private boolean fieldsAreEqual(DatasetField incomingField, DatasetField currentField) {
        if (!incomingField.getDatasetFieldType().getName().equals(currentField.getDatasetFieldType().getName())) {
            return false;
        }
        if (incomingField.getDatasetFieldType().isCompound()) {
            List<DatasetFieldCompoundValue> incomingValues = incomingField.getDatasetFieldCompoundValues();
            List<DatasetFieldCompoundValue> currentValues = currentField.getDatasetFieldCompoundValues();
            if (incomingValues.size() != currentValues.size()) {
                return false;
            }
            for (int i = 0; i < incomingValues.size(); i++) {
                if (!fieldsAreEqual(incomingValues.get(i).getChildDatasetFields(), currentValues.get(i).getChildDatasetFields())) {
                    return false;
                }
            }
            return true;
        } else {
            // Use getValues_nondisplay() to compares raw metadata field value (or values, if the field allows multiple)
            return Objects.equals(incomingField.getValues_nondisplay(), currentField.getValues_nondisplay());
        }
    }

    private boolean termsAreEqual() {
        TermsOfUseAndAccess incomingTerms = incomingVersion.getTermsOfUseAndAccess();
        TermsOfUseAndAccess currentTerms = currentVersion.getTermsOfUseAndAccess();
        if (incomingTerms == null || currentTerms == null) {
            return incomingTerms == currentTerms;
        }

        return licenseIsEqual(incomingTerms.getLicense(), currentTerms.getLicense())
                && Objects.equals(incomingTerms.getTermsOfUse(), currentTerms.getTermsOfUse())
                && Objects.equals(incomingTerms.getConfidentialityDeclaration(), currentTerms.getConfidentialityDeclaration())
                && Objects.equals(incomingTerms.getSpecialPermissions(), currentTerms.getSpecialPermissions())
                && Objects.equals(incomingTerms.getRestrictions(), currentTerms.getRestrictions())
                && Objects.equals(incomingTerms.getCitationRequirements(), currentTerms.getCitationRequirements())
                && Objects.equals(incomingTerms.getDepositorRequirements(), currentTerms.getDepositorRequirements())
                && Objects.equals(incomingTerms.getConditions(), currentTerms.getConditions())
                && Objects.equals(incomingTerms.getDisclaimer(), currentTerms.getDisclaimer())
                && Objects.equals(incomingTerms.getTermsOfAccess(), currentTerms.getTermsOfAccess())
                && Objects.equals(incomingTerms.getDataAccessPlace(), currentTerms.getDataAccessPlace())
                && Objects.equals(incomingTerms.getOriginalArchive(), currentTerms.getOriginalArchive())
                && Objects.equals(incomingTerms.getAvailabilityStatus(), currentTerms.getAvailabilityStatus())
                && Objects.equals(incomingTerms.getContactForAccess(), currentTerms.getContactForAccess())
                && Objects.equals(incomingTerms.getSizeOfCollection(), currentTerms.getSizeOfCollection())
                && Objects.equals(incomingTerms.getStudyCompletion(), currentTerms.getStudyCompletion())
                && incomingTerms.isFileAccessRequest() == currentTerms.isFileAccessRequest();
    }

    private boolean licenseIsEqual(License incomingLicense, License currentLicense) {
        if (incomingLicense == null || currentLicense == null) {
            return incomingLicense == currentLicense;
        }
        return Objects.equals(incomingLicense.getName(), currentLicense.getName())
                && Objects.equals(incomingLicense.getUri(), currentLicense.getUri());
    }

    private boolean versionFieldsAreEqual() {
        return specifiedValueIsEqual(incomingVersion.getDeaccessionLink(), currentVersion.getDeaccessionLink())
                && specifiedValueIsEqual(incomingVersion.getDeaccessionNote(), currentVersion.getDeaccessionNote())
                && specifiedValueIsEqual(incomingVersion.getVersionNote(), currentVersion.getVersionNote())
                && specifiedValueIsEqual(incomingVersion.getReleaseTime(), currentVersion.getReleaseTime())
                && specifiedValueIsEqual(incomingVersion.getArchiveTime(), currentVersion.getArchiveTime())
                && specifiedValueIsEqual(incomingVersion.getUNF(), currentVersion.getUNF());
    }

    private boolean specifiedValueIsEqual(Object incomingValue, Object currentValue) {
        // The parser represents omitted version-level fields as null
        // Treat null as "not supplied" rather than as a change, so requests which are otherwise unchanged do not
        // create a draft merely because they omit fields such as versionNote, releaseTime, or UNF
        return incomingValue == null || Objects.equals(incomingValue, currentValue);
    }
}
