package edu.harvard.iq.dataverse.api.datadeposit;

import edu.harvard.iq.dataverse.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldConstant;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.authorization.users.User;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;

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
         * @todo Once dataverse has subject (https://github.com/IQSS/dataverse/issues/769), we should get subject from there 
         * for now, we'll use the global NA value
         */
        ControlledVocabularyValue cvv = datasetFieldService.findNAControlledVocabularyValue();
        subjectDatasetField.setSingleControlledVocabularyValue(cvv);
            
        datasetVersion.getDatasetFields().add(subjectDatasetField);
    }
}
