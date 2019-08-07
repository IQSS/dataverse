package edu.harvard.iq.dataverse.datafile.page;

import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

class FileMetadataOrder {

    /**
     * Sets display order same as index in the list.
     *
     * @param filesToReorder
     * @return filemetadas with changed display order.
     */
    static List<FileMetadata> reorderDisplayOrder(List<FileMetadata> filesToReorder) {
        List<FileMetadata> changes = newArrayList();

        for (int i = 0; i < filesToReorder.size(); i++) {
            FileMetadata fileMetadata = filesToReorder.get(i);
            if (fileMetadata.getDisplayOrder() != i) {
                fileMetadata.setDisplayOrder(i);
                changes.add(fileMetadata);
            }
        }

        return changes;
    }
}
