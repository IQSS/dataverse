package edu.harvard.iq.dataverse.dataset.difference;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;

/**
 * Class that contains old and new value of {@link DatasetField}
 * that is different between two {@link DatasetVersion}s
 * 
 * @author madryk
 */
public class DatasetFieldDiff extends ItemDiff<DatasetField> {

    // -------------------- CONSTRUCTORS --------------------
    
    public DatasetFieldDiff(DatasetField oldValue, DatasetField newValue) {
        super(oldValue, newValue);
    }
    
}