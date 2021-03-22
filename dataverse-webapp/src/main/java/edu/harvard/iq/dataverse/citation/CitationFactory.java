package edu.harvard.iq.dataverse.citation;

import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;

import javax.ejb.Stateless;
import javax.inject.Inject;

@Stateless
public class CitationFactory {

    private CitationDataExtractor dataExtractor;

    private CitationFormatsConverter converter;

    // -------------------- CONSTRUCTORS --------------------

    public CitationFactory() { }

    @Inject
    public CitationFactory(CitationDataExtractor dataExtractor, CitationFormatsConverter converter) {
        this.dataExtractor = dataExtractor;
        this.converter = converter;
    }

    // -------------------- LOGIC --------------------

    public Citation create(DatasetVersion datasetVersion) {
        return new Citation(dataExtractor.create(datasetVersion), converter);
    }

    public Citation create(FileMetadata fileMetadata, boolean direct) {
        return new Citation(dataExtractor.create(fileMetadata, direct), converter);
    }

    public Citation create(FileMetadata fileMetadata) {
        return create(fileMetadata, false);
    }
}
