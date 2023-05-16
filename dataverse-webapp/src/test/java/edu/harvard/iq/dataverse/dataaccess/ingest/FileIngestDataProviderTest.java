package edu.harvard.iq.dataverse.dataaccess.ingest;

class FileIngestDataProviderTest implements IngestDataProviderTest {

    @Override
    public IngestDataProvider getProvider() {
        return new FileIngestDataProvider();
    }
}