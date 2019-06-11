package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.DatasetVersion;

public class IndexableDataset extends IndexableObject {

    DatasetState datasetState;
    boolean filesShouldBeIndexed;

    private final DatasetVersion datasetVersion;

    public IndexableDataset(DatasetVersion datasetVersion) {
        this.datasetVersion = datasetVersion;
        super.setType(IndexableObject.IndexableTypes.DATASET.getName());
        if (datasetVersion.isWorkingCopy()) {
            this.datasetState = DatasetState.WORKING_COPY;
            this.filesShouldBeIndexed = true;
        } else if (datasetVersion.isReleased()) {
            this.datasetState = DatasetState.PUBLISHED;
            this.filesShouldBeIndexed = true;
        } else if (datasetVersion.isDeaccessioned()) {
            this.datasetState = DatasetState.DEACCESSIONED;
            this.filesShouldBeIndexed = false;
        }
    }

    public boolean isFilesShouldBeIndexed() {
        return filesShouldBeIndexed;
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

        WORKING_COPY(IndexServiceBean.draftSuffix), PUBLISHED(""), DEACCESSIONED(IndexServiceBean.deaccessionedSuffix);

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
