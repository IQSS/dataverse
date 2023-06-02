package edu.harvard.iq.dataverse.api.datadeposit;

import edu.harvard.iq.dataverse.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldConstant;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.TermsOfUseAndAccess;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.dataset.DatasetUtil;
import edu.harvard.iq.dataverse.license.License;
import edu.harvard.iq.dataverse.license.LicenseServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.util.ArrayList;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.swordapp.server.SwordEntry;
import org.swordapp.server.SwordError;

@Stateless
@Named
public class SwordServiceBean {

    private static final Logger logger = Logger.getLogger(SwordServiceBean.class.getCanonicalName());

    @EJB
    DatasetFieldServiceBean datasetFieldService;
    @Inject
    LicenseServiceBean licenseServiceBean;
    @EJB
    SystemConfig systemConfig;

    /**
     * Mutate the dataset version, adding a datasetContact (email address) from
     * the dataverse that will own the dataset.
     */
    public void addDatasetContact(DatasetVersion newDatasetVersion, User user) {
        DatasetFieldType emailDatasetFieldType = datasetFieldService.findByNameOpt(DatasetFieldConstant.datasetContact);
        DatasetField emailDatasetField = DatasetField.createNewEmptyDatasetField(emailDatasetFieldType, newDatasetVersion);

        for (DatasetField childField : emailDatasetField.getDatasetFieldCompoundValues().get(0).getChildDatasetFields()) {
            if (DatasetFieldConstant.datasetContactEmail.equals(childField.getDatasetFieldType().getName())) {
                // set the value to the  in user's email
                childField.getSingleValue().setValue(user.getDisplayInfo().getEmailAddress());
            }
            // We don't see any error from EZID but when using DataCite, we were seeing this error: Response code: 400, [xml] xml error: cvc-minLength-valid: Value '' with length = '0' is not facet-valid with respect to minLength '1' for type '#AnonType_contributorNamecontributorcontributorsresource'.
            if (DatasetFieldConstant.datasetContactName.equals(childField.getDatasetFieldType().getName())) {
                childField.getSingleValue().setValue(user.getDisplayInfo().getTitle());
            }
        }

        newDatasetVersion.getDatasetFields().add(emailDatasetField);

    }

    /**
     * Mutate the dataset version, adding a depositor for the dataset.
     */
    public void addDatasetDepositor(DatasetVersion newDatasetVersion, User user) {
        if (!user.isAuthenticated()) {
            logger.info("returning early since user is not authenticated");
            return;
        }
        AuthenticatedUser au = (AuthenticatedUser) user;
        DatasetFieldType depositorDatasetFieldType = datasetFieldService.findByNameOpt(DatasetFieldConstant.depositor);
        DatasetField depositorDatasetField = DatasetField.createNewEmptyDatasetField(depositorDatasetFieldType, newDatasetVersion);
        depositorDatasetField.setSingleValue(au.getLastName() + ", " + au.getFirstName());

        newDatasetVersion.getDatasetFields().add(depositorDatasetField);
    }

    /**
     * If no subject exists, mutate the dataset version, adding "N/A" for the
     * subject. Otherwise, leave the dataset alone.
     */
    public void addDatasetSubjectIfMissing(DatasetVersion datasetVersion) {
        DatasetFieldType subjectDatasetFieldType = datasetFieldService.findByNameOpt(DatasetFieldConstant.subject);

        boolean subjectFieldExists = false;
        List<DatasetField> datasetFields = datasetVersion.getDatasetFields();
        for (DatasetField datasetField : datasetFields) {
            logger.fine("datasetField: " + datasetField.getDisplayValue() + " ... " + datasetField.getDatasetFieldType().getName());
            if (datasetField.getDatasetFieldType().getName().equals(subjectDatasetFieldType.getName())) {
                subjectFieldExists = true;
                logger.fine("subject field exists already");
                break;
            }
        }

        if (subjectFieldExists) {
            // return early. nothing to do. dataset already has a subject
            logger.fine("returning early because subject exists already");
            return;
        }

        // if we made it here, we must not have a subject, so let's add one
        DatasetField subjectDatasetField = DatasetField.createNewEmptyDatasetField(subjectDatasetFieldType, datasetVersion);
        /**
         * @todo Once dataverse has subject
         * (https://github.com/IQSS/dataverse/issues/769), we should get subject
         * from there for now, we'll use the global NA value. However, there is
         * currently oddness in that if you go to edit the title of a dataset
         * via the GUI you can not save the dataset without selecting a Subject:
         * https://github.com/IQSS/dataverse/issues/1296#issuecomment-70146314
         */
        ControlledVocabularyValue cvv = datasetFieldService.findNAControlledVocabularyValue();
        subjectDatasetField.setSingleControlledVocabularyValue(cvv);

        datasetVersion.getDatasetFields().add(subjectDatasetField);
    }

    /**
     * The rules (from https://github.com/IQSS/dataverse/issues/805 ) are below.
     * Edited to support multi-license.
     *
     * If you don't provide `<dcterms:license>` the license should be set to
     * "Default License" on dataset creation.
     *
     * If you don't provide `<dcterms:license>` the license on dataset
     * modification, the license should not change.
     *
     * To provide `<dcterms:rights>` you much either omit `<dcterms:license>`
     * (for backwards compatibility since `<dcterms:license>` was not a required
     * field for SWORD in DVN 3.6) or set `<dcterms:license>` to "Custom License".
     *
     * It is invalid to provide a license under `<dcterms:license>` in combination
     * with any value under `<dcterms:rights>`.
     *
     * It is invalid to attempt to change the license if Terms of Use
     * (`<dcterms:rights>`) is already on file.
     *
     * Both `<dcterms:rights>` and `<dcterms:license>` can only be specified
     * once. Multiples are not allowed.
     *
     * Blank values are not allowed for `<dcterms:license>` (since it's new) but
     * for backwards compatibility, blank values are allowed for
     * `<dcterms:rights>` per
     * https://github.com/IQSS/dataverse/issues/805#issuecomment-71670396
     *
     * @todo What about the "native" API? Are similar rules enforced? See also
     * https://github.com/IQSS/dataverse/issues/1385
     */
    public void setDatasetLicenseAndTermsOfUse(DatasetVersion datasetVersionToMutate, SwordEntry swordEntry) throws SwordError {
        Map<String, List<String>> dcterms = swordEntry.getDublinCore();
        List<String> listOfLicensesProvided = dcterms.get("license");
        List<String> rights = dcterms.get("rights");
        if (rights != null && !systemConfig.isAllowCustomTerms()) {
            throw new SwordError("Custom Terms (dcterms:rights) are not allowed.");
        }

        TermsOfUseAndAccess terms = new TermsOfUseAndAccess();
        datasetVersionToMutate.setTermsOfUseAndAccess(terms);
        terms.setDatasetVersion(datasetVersionToMutate);
        
        if (listOfLicensesProvided == null) {
            License existingLicense = DatasetUtil.getLicense(datasetVersionToMutate);
            if (existingLicense != null) {
                // leave the license alone but set terms of use
                setTermsOfUse(datasetVersionToMutate, dcterms, existingLicense);
            } else {
                License defaultLicense = licenseServiceBean.getDefault();
                List<String> listOfRights = dcterms.get("rights");
                if (listOfRights != null) {
                    int numRightsProvided = listOfRights.size();
                    if (numRightsProvided != 1) {
                        throw new SwordError("Only one Terms of Use (dcterms:rights) can be provided per dataset, not " + numRightsProvided);
                    } else {
                        // Set to Custom for backwards compatibility. We didn't require a license for SWORD in DVN 3.x.
                        defaultLicense = null;
                    }
                }
                terms.setLicense(defaultLicense);
                terms.setFileAccessRequest(datasetVersionToMutate.getTermsOfUseAndAccess().isFileAccessRequest());
                terms.setDatasetVersion(datasetVersionToMutate);
                setTermsOfUse(datasetVersionToMutate, dcterms, defaultLicense);
            }
            return;
        }
        int numLicensesProvided = listOfLicensesProvided.size();
        if (numLicensesProvided != 1) {
            throw new SwordError("Only one license can be provided per dataset, not " + numLicensesProvided + ".");
        }
        String licenseProvided = listOfLicensesProvided.get(0);
        if (StringUtils.isBlank(licenseProvided)) {
            throw new SwordError("License provided was blank.");
        }
        if (StringUtils.equalsIgnoreCase(licenseProvided, BundleUtil.getStringFromBundle("license.custom"))){
            terms.setLicense(null);
            setTermsOfUse(datasetVersionToMutate, dcterms, null);
        } else {
            License licenseToSet = licenseServiceBean.getByNameOrUri(licenseProvided);
            if (licenseToSet == null || !licenseToSet.isActive()) {
                List<String> licenses = new ArrayList<>();
                for (License license : licenseServiceBean.listAllActive()) {
                    licenses.add(license.getName());
                }
                throw new SwordError("Couldn't find an active license with: " + licenseProvided + ". Valid licenses: " + licenses);
            }
            terms.setLicense(licenseToSet);
            setTermsOfUse(datasetVersionToMutate, dcterms, licenseToSet);
        }
    }

    private void setTermsOfUse(DatasetVersion datasetVersionToMutate, Map<String, List<String>> dcterms, License providedLicense) throws SwordError {
        if (providedLicense != null) {
            String existingTermsOfUse = datasetVersionToMutate.getTermsOfUseAndAccess().getTermsOfUse();
            if (existingTermsOfUse != null) {
                throw new SwordError("Can not change license to \"" + providedLicense.getName() + "\" due to existing Terms of Use (dcterms:rights): \"" + existingTermsOfUse + "\". You can specify a Custom license.");
            }
        }
        List<String> listOfRightsProvided = dcterms.get("rights");
        if (listOfRightsProvided != null) {
            int numRightsProvided = listOfRightsProvided.size();
            if (providedLicense != null) {
                if (numRightsProvided > 0) {
                    throw new SwordError("Terms of Use (dcterms:rights) cannot be specified in combination with a license.");
                }
            } else {
                if (numRightsProvided != 1) {
                    throw new SwordError("Only one Terms of Use (dcterms:rights) can be provided per dataset, not " + numRightsProvided);
                }
                String termsOfUseProvided = listOfRightsProvided.get(0);
                if (StringUtils.isBlank(termsOfUseProvided)) {
                    /**
                     * for backwards compatibility, let dcterms:rights be blank
                     * (don't throw an error) (but don't persist an empty
                     * string):
                     * https://github.com/IQSS/dataverse/issues/805#issuecomment-71670396
                     */
                    // throw new SwordError("Terms of Use (dcterms:rights) provided was blank.");
                } else {
                    datasetVersionToMutate.getTermsOfUseAndAccess().setTermsOfUse(termsOfUseProvided);
                }
            }
        }
    }

}
