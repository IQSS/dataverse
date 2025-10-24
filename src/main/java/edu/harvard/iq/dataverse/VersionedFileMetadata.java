package edu.harvard.iq.dataverse;

/**
 * A Data Transfer Object (DTO) to hold a DatasetVersion and its
 * corresponding FileMetadata for a specific file. The fileMetadata
 * field can be null if the file does not exist in that version.
 */
public class VersionedFileMetadata {
    private final DatasetVersion datasetVersion;
    private final FileMetadata fileMetadata; // This can be null

    public VersionedFileMetadata(DatasetVersion datasetVersion, FileMetadata fileMetadata) {
        this.datasetVersion = datasetVersion;
        this.fileMetadata = fileMetadata;
    }

    // Add getters for both fields
    public DatasetVersion getDatasetVersion() {
        return datasetVersion;
    }

    public FileMetadata getFileMetadata() {
        return fileMetadata;
    }
}
