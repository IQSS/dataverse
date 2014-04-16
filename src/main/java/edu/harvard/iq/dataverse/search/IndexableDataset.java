package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.DatasetVersion;

public class IndexableDataset extends IndexableObject {

    DatasetState datasetState;

    private final DatasetVersion datasetVersion;

    public IndexableDataset(DatasetVersion datasetVersion) {
        this.datasetVersion = datasetVersion;
        super.setType(IndexableObject.IndexableTypes.DATASET.getName());
        if (datasetVersion.isWorkingCopy()) {
            this.datasetState = DatasetState.WORKING_COPY;
        } else {
            this.datasetState = DatasetState.PUBLISHED;
        }
    }

    public DatasetVersion getDatasetVersion() {
        return datasetVersion;
    }

    public String getSolrDocId() {
        return super.getType() + "_" + datasetVersion.getDataset().getId() + datasetState.suffix;
    }

    public DatasetState getDatasetState() {
        return datasetState;
    }

    public enum DatasetState {

        WORKING_COPY("_draft"), PUBLISHED("");

        private String suffix;

        private DatasetState(String string) {
            suffix = string;
        }

        public void setSuffix(String suffix) {
            this.suffix = suffix;
        }

        public String getSuffix() {
            return suffix;
        }

    }

}
