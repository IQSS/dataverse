package edu.harvard.iq.dataverse.ingest;

import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Stateless
public class UningestInfoService {
    private DataFileServiceBean dataFileService;

    // -------------------- CONSTRUCTORS --------------------

    public UningestInfoService() { }

    @Inject
    public UningestInfoService(DataFileServiceBean dataFileService) {
        this.dataFileService = dataFileService;
    }

    // -------------------- LOGIC --------------------

    public List<DataFile> listUningestableFiles(Dataset dataset) {
        if (dataset == null || !dataset.getLatestVersion().isDraft()) {
            return Collections.emptyList();
        }
        List<DataFile> uningestable = new ArrayList<>();
        // Only certain files from draft version can be uningested:
        List<FileMetadata> fileMetadatas = dataset.getLatestVersion().getFileMetadatas();
        for (FileMetadata metadata : fileMetadatas) {
            DataFile dataFile = metadata.getDataFile();
            if (canUningestFile(dataFile)) {
                uningestable.add(dataFile);
            }
        }
        return uningestable;
    }

    public boolean hasUningestableFiles(Dataset dataset) {
       if (dataset == null || !dataset.getLatestVersion().isDraft()) {
           return false;
       }
       return dataset.getLatestVersion().getFileMetadatas().stream()
               .map(FileMetadata::getDataFile)
               .anyMatch(this::canUningestFile);
    }

    // -------------------- PRIVATE --------------------

    private boolean canUningestFile(DataFile file) {
        // File from draft version can be uningested if it:
        // (1) was not published yet (ie. it has only one metadata set, but we assume that
        //     the file is from the latest, draft version – which is NOT checked here);
        // (2) is of XLSX, CSV or TSV type;
        // (3) has been ingested (successfully or not).
        return file.getFileMetadatas().size() == 1 // 1
                && dataFileService.isSelectivelyIngestableFile(file) // 2
                && (file.getIngestStatus() != DataFile.INGEST_STATUS_NONE || file.isTabularData()); // 3
    }
}