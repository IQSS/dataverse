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
        DatasetField emailDatasetField = new DatasetField();
        DatasetFieldType emailDatasetFieldType = datasetFieldService.findByNameOpt(DatasetFieldConstant.datasetContact);
        List<DatasetFieldValue> values = new ArrayList<>();
        values.add(new DatasetFieldValue(emailDatasetField, newDatasetVersion.getDataset().getOwner().getContactEmail()));
        emailDatasetField.setDatasetFieldValues(values);
        emailDatasetField.setDatasetFieldType(emailDatasetFieldType);
        List<DatasetField> currentFields = newDatasetVersion.getDatasetFields();
        currentFields.add(emailDatasetField);
        newDatasetVersion.setDatasetFields(currentFields);
    }

    public void addDatasetSubject(DatasetVersion datasetVersion) {
        DatasetField subjectDatasetField = new DatasetField();
        DatasetFieldType subjectDatasetFieldType = datasetFieldService.findByNameOpt(DatasetFieldConstant.subject);
        subjectDatasetField.setDatasetFieldType(subjectDatasetFieldType);
        /**
         * @todo Rather than hard coding "Other" here, inherit the Subject from
         * the parent dataverse, when it's available once
         * https://github.com/IQSS/dataverse/issues/769 has been implemented
         */
        String subjectOther = "Other";
        ControlledVocabularyValue cvv = datasetFieldService.findControlledVocabularyValueByDatasetFieldTypeAndStrValue(subjectDatasetFieldType, subjectOther);
        if (cvv != null) {
            List<ControlledVocabularyValue> controlledVocabularyValues = new ArrayList();
            controlledVocabularyValues.add(cvv);
            subjectDatasetField.setControlledVocabularyValues(controlledVocabularyValues);

            // add the new subject field to the rest of the fields
            List<DatasetField> currentFields = datasetVersion.getDatasetFields();
            currentFields.add(subjectDatasetField);
            datasetVersion.setDatasetFields(currentFields);
        } else {
            logger.info(subjectDatasetFieldType.getName() + "could not be populated with '" + subjectOther + "': null returned from lookup.");
        }
    }
}
