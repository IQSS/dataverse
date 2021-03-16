package edu.harvard.iq.dataverse.citation;

import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;

import javax.ejb.Stateless;

@Stateless
public class CitationFactory {

    // -------------------- LOGIC --------------------

    public Citation create(DatasetVersion datasetVersion, boolean direct) {
        return new Citation(datasetVersion, direct);
    }

    public Citation create(DatasetVersion datasetVersion) {
        return create(datasetVersion, false);
    }

    public Citation create(FileMetadata fileMetadata, boolean direct) {
        return new Citation(fileMetadata, direct);
    }

    public Citation create(FileMetadata fileMetadata) {
        return create(fileMetadata, false);
    }
}
