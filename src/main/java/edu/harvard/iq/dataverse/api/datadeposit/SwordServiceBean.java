package edu.harvard.iq.dataverse.api.datadeposit;

import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldConstant;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetFieldValue;
import edu.harvard.iq.dataverse.DatasetVersion;
import java.util.ArrayList;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;

@Stateless
@Named
public class SwordServiceBean {

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

}
