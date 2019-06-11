package edu.harvard.iq.dataverse.datafile.page;

import edu.harvard.iq.dataverse.FileMetadata;
import org.primefaces.event.ReorderEvent;

/**
 * Helper class which tells which file was moved where during reordering.
 */
class FileMetadataEvent {

    FileMetadataEvent(ReorderEvent reorderEvent, FileMetadata fileMetadata) {
        this.reorderEvent = reorderEvent;
        this.fileMetadata = fileMetadata;
    }

    private ReorderEvent reorderEvent;
    private FileMetadata fileMetadata;

    ReorderEvent getReorderEvent() {
        return reorderEvent;
    }

    FileMetadata getFileMetadata() {
        return fileMetadata;
    }

    void setReorderEvent(ReorderEvent reorderEvent) {
        this.reorderEvent = reorderEvent;
    }

    void setFileMetadata(FileMetadata fileMetadata) {
        this.fileMetadata = fileMetadata;
    }
}
