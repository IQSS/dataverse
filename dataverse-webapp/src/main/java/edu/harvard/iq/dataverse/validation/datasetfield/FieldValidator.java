package edu.harvard.iq.dataverse.validation.datasetfield;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;

import java.util.List;
import java.util.Map;

public interface FieldValidator {

    String getName();

    ValidationResult isValid(DatasetField field, Map<String, String> params, Map<String, List<DatasetField>> fieldIndex);

}