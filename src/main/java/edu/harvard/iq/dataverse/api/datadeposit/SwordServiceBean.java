package edu.harvard.iq.dataverse.api.datadeposit;

import edu.harvard.iq.dataverse.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldConstant;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.authorization.users.User;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;
import org.apache.commons.lang.StringUtils;
import org.swordapp.server.SwordEntry;
import org.swordapp.server.SwordError;

@Stateless
@Named
public class SwordServiceBean {

    private static final Logger logger = Logger.getLogger(SwordServiceBean.class.getCanonicalName());

    @EJB
    DatasetFieldServiceBean datasetFieldService;

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
        }

        newDatasetVersion.getDatasetFields().add(emailDatasetField);

    }

    public void addDatasetSubject(DatasetVersion datasetVersion) {
        DatasetFieldType subjectDatasetFieldType = datasetFieldService.findByNameOpt(DatasetFieldConstant.subject);
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
     * @todo make sure this is called from both "create" and "edit"
     */
    public void addDatasetLicense(DatasetVersion datasetVersionToMutate, SwordEntry swordEntry) throws SwordError {
        Map<String, List<String>> dcterms = swordEntry.getDublinCore();
        List<String> listOfLicensesProvided = dcterms.get("license");
        if (listOfLicensesProvided == null) {
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
        DatasetVersion.License licenseToSet;
        try {
            licenseToSet = DatasetVersion.License.valueOf(licenseProvided);
        } catch (IllegalArgumentException ex) {
            throw new SwordError("License provided was \"" + licenseProvided + "\" but one " + Arrays.toString(DatasetVersion.License.values()) + " was expected.");
        }
        datasetVersionToMutate.setLicense(licenseToSet);
    }

}
