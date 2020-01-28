package edu.harvard.iq.dataverse.annotations.processors.permissions.extractors;

import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;

public class DatasetFromVersion implements DvObjectExtractor {

    @Override
    public DvObject extract(Object input) {
        if (input == null) {
            throw new IllegalArgumentException("Provided object is null. Cannot extract.");
        }
        return ((DatasetVersion) input).getDataset();
    }
}
