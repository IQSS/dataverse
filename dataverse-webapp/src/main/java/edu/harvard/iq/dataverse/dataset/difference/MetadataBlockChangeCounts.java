package edu.harvard.iq.dataverse.dataset.difference;

import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;

/**
 * Class that contains number of dataset field values that
 * have been added/removed/changed inside {@link MetadataBlock}.
 * 
 * @author madryk
 */
public class MetadataBlockChangeCounts extends ChangeCounts<MetadataBlock> {

    // -------------------- CONSTRUCTORS --------------------
    
    public MetadataBlockChangeCounts(MetadataBlock item) {
        super(item);
    }
    
}