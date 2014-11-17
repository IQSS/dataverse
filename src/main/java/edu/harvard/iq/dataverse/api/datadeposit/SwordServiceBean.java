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

    public void doRequiredFieldCheck(DatasetVersion dataset) throws SwordError {
        List<String> requiredFields = new ArrayList<>();
        /**
         * @todo call into a method (once it exists) that pays attention to
         * which fields are required for the dataset passed in rather than
         * findAllRequiredFields() which check *all* metadata blocks. Without
         * this method in place, we simply skip over all the GSD fields during
         * the check below. See also
         * https://github.com/IQSS/dataverse/issues/268#issuecomment-60301025
         *
         * Alternatively, remove this entire doRequiredFieldCheck method when
         * CreateDatasetCommand and UpdateDatasetCommand throw
         * IllegalCommandException("Can't create or update dataset because the
         * following required fields were not supplied: description, subject")
         * as suggested at
         * https://github.com/IQSS/dataverse/issues/605#issuecomment-60297583
         *
         * It is much preferred to simply remove this whole method once #605 is
         * complete.
         */
        final List<DatasetFieldType> requiredDatasetFieldTypes = datasetFieldService.findAllRequiredFields();
        for (DatasetFieldType requiredField : requiredDatasetFieldTypes) {
            requiredFields.add(requiredField.getName());
        }

        List<String> createdFields = new ArrayList<>();
        final List<DatasetField> createdDatasetFields = dataset.getFlatDatasetFields();
        for (DatasetField createdField : createdDatasetFields) {
            createdFields.add(createdField.getDatasetFieldType().getName());
            logger.info(createdField.getDatasetFieldType().getName() + ":" + createdField.getValue());
        }
        logger.info("created fields: " + createdFields);

        for (String requiredField : requiredFields) {
            /**
             * @todo See the note about findAllRequiredFields() above.
             *
             * @todo We can probably pull these out after these GSD fields after
             * this ticket is closed:
             *
             * Make all fields in GSD block tsv "optional"
             * https://github.com/IQSS/dataverse/issues/1137
             */
            if (requiredField.equals("gsdStudentName")) {
                continue;
            }
            if (requiredField.equals("gsdStudentProgram")) {
                continue;
            }
            if (requiredField.equals("gsdCourseName")) {
                continue;
            }
            if (requiredField.equals("gsdFacultyName")) {
                continue;
            }
            if (requiredField.equals("gsdSemester")) {
                continue;
            }
            if (requiredField.equals("gsdRecommendation")) {
                continue;
            }
            if (requiredField.equals("gsdSiteType")) {
                continue;
            }
            if (requiredField.equals("gsdProgramBrief")) {
                continue;
            }
            if (requiredField.equals("gsdTags")) {
                continue;
            }
            if (!createdFields.contains(requiredField)) {
                /**
                 * @todo It would be nicer to return not our internal name for
                 * the metadata field (authorName) and instead the SWORD
                 * equivalent (dcterms:creator):
                 * https://github.com/IQSS/dataverse/issues/1019
                 */
                throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Can't create/update dataset. " + SwordUtil.DCTERMS + " equivalent of required field not found: " + requiredField);
            }
        }

    }

}
