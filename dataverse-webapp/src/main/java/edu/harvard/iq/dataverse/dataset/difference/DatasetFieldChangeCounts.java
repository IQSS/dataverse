package edu.harvard.iq.dataverse.dataset.difference;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;

/**
 * Class that contains number of dataset field values that
 * have been added/removed/changed inside dataset field
 * of some type.
 * 
 * @author madryk
 */
public class DatasetFieldChangeCounts extends ChangeCounts<DatasetFieldType> {

    // -------------------- CONSTRUCTORS --------------------
    
    public DatasetFieldChangeCounts(DatasetFieldType item) {
        super(item);
    }
}