package edu.harvard.iq.dataverse.annotations.processors.permissions.extractors;

import edu.harvard.iq.dataverse.persistence.DvObject;

public class CastingExtractor implements DvObjectExtractor {

    @Override
    public DvObject extract(Object input) {
        return (DvObject) input;
    }
}
