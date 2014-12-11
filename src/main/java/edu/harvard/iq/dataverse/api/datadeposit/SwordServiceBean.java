package edu.harvard.iq.dataverse.api.datadeposit;

import edu.harvard.iq.dataverse.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldConstant;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetFieldValue;
import edu.harvard.iq.dataverse.DatasetVersion;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;
import org.swordapp.server.SwordError;
import org.swordapp.server.UriRegistry;

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
    public void addDatasetContact(DatasetVersion newDatasetVersion) {
        DatasetFieldType emailDatasetFieldType = datasetFieldService.findByNameOpt(DatasetFieldConstant.datasetContact);
        DatasetField emailDatasetField = DatasetField.createNewEmptyDatasetField(emailDatasetFieldType, newDatasetVersion);

        for (DatasetField childField : emailDatasetField.getDatasetFieldCompoundValues().get(0).getChildDatasetFields()) {
            if (DatasetFieldConstant.datasetContactEmail.equals(childField.getDatasetFieldType().getName())) {
                // set the value to that of the owning dataverse
                childField.getSingleValue().setValue(newDatasetVersion.getDataset().getOwner().getContactEmail());
            }
        }

        newDatasetVersion.getDatasetFields().add(emailDatasetField);

    }

    public void addDatasetSubject(DatasetVersion datasetVersion) {
        DatasetFieldType subjectDatasetFieldType = datasetFieldService.findByNameOpt(DatasetFieldConstant.subject);
        DatasetField subjectDatasetField = DatasetField.createNewEmptyDatasetField(subjectDatasetFieldType, datasetVersion);

        /**
         * @todo Rather than hard coding "Other" here, inherit the Subject from
         * the parent dataverse, when it's available once
         * https://github.com/IQSS/dataverse/issues/769 has been implemented
         */
        String subjectOther = "Other";
        ControlledVocabularyValue cvv = datasetFieldService.findControlledVocabularyValueByDatasetFieldTypeAndStrValue(subjectDatasetFieldType, subjectOther);
        if (cvv != null) {
            subjectDatasetField.setSingleControlledVocabularyValue(cvv);
            
            datasetVersion.getDatasetFields().add(subjectDatasetField);

        } else {
            logger.info(subjectDatasetFieldType.getName() + "could not be populated with '" + subjectOther + "': null returned from lookup.");
        }
    }
}
