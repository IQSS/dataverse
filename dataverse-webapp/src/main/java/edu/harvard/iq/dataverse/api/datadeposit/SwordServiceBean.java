package edu.harvard.iq.dataverse.api.datadeposit;

import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.common.DatasetFieldConstant;
import edu.harvard.iq.dataverse.persistence.dataset.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.User;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.List;
import java.util.logging.Logger;

@Stateless
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

        for (DatasetField childField : emailDatasetField.getDatasetFieldsChildren()) {
            if (DatasetFieldConstant.datasetContactEmail.equals(childField.getDatasetFieldType().getName())) {
                // set the value to the  in user's email
                childField.setFieldValue(user.getDisplayInfo().getEmailAddress());
            }
            // We don't see any error from EZID but when using DataCite, we were seeing this error: Response code: 400, [xml] xml error: cvc-minLength-valid: Value '' with length = '0' is not facet-valid with respect to minLength '1' for type '#AnonType_contributorNamecontributorcontributorsresource'.
            if (DatasetFieldConstant.datasetContactName.equals(childField.getDatasetFieldType().getName())) {
                childField.setFieldValue(user.getDisplayInfo().getTitle());
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
        depositorDatasetField.setFieldValue(au.getLastName() + ", " + au.getFirstName());

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
}
