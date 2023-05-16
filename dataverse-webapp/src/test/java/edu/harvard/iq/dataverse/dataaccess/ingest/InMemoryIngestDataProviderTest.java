package edu.harvard.iq.dataverse.dataaccess.ingest;

class InMemoryIngestDataProviderTest implements IngestDataProviderTest {

    @Override
    public IngestDataProvider getProvider() {
        return new InMemoryIngestDataProvider();
    }
}