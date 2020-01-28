package edu.harvard.iq.dataverse.annotations.processors.permissions.extractors;

import edu.harvard.iq.dataverse.persistence.DvObject;

public interface DvObjectExtractor {
    DvObject extract(Object input);
}
