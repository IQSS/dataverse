package edu.harvard.iq.dataverse.datafile.file.dto;

import edu.harvard.iq.dataverse.datafile.file.FileMetadataService;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Class created in order to use Lazy-Loading in p:datatable.
 */
public class LazyFileMetadataModel extends LazyDataModel<FileMetadata> {

    private FileMetadataService fileMetadataService;
    private long dsvId;
    private boolean loadedSearchData;
    private int loadedSearchDataSize;
    private List<FileMetadata> loadedData = new ArrayList<>();
    private List<Long> allFileIds = new ArrayList<>();

    // -------------------- CONSTRUCTORS --------------------

    public LazyFileMetadataModel(FileMetadataService fileMetadataService, long dsvId) {
        this.fileMetadataService = fileMetadataService;
        this.dsvId = dsvId;
    }

    // -------------------- LOGIC --------------------

    @Override
    public Object getRowKey(FileMetadata object) {
        return String.valueOf(object.getId());
    }

    @Override
    public FileMetadata getRowData(String rowKey) {
        return loadedData.stream()
                         .filter(fileMetadata -> fileMetadata.getId().equals(Long.parseLong(rowKey)))
                         .findFirst()
                         .orElse(null);
    }

    @Override
    public List<FileMetadata> load(int first, int pageSize, Map<String, SortMeta> sortBy, Map<String, FilterMeta> filterBy) {
        if (loadedSearchData) {
            return this.getWrappedData();
        }

        int pageNumber = first / pageSize;
        loadedData = fileMetadataService.findAccessibleFileMetadataSorted(dsvId, pageNumber, pageSize);

        if (allFileIds.isEmpty()) {
        allFileIds = fileMetadataService.findFileMetadataIds(dsvId);
        this.setRowCount(allFileIds.size());
        }

        return loadedData;
    }

    public int getAllAvailableFilesCount() {
        return loadedSearchData ? loadedSearchDataSize : allFileIds.size();
    }

    /**
     * Indicates if data was acquired using search input.
     */
    public LazyFileMetadataModel setLoadedSearchData(boolean loadedSearchData) {
        this.loadedSearchData = loadedSearchData;
        return this;
    }

    /**
     * Indicates amounts of data that is found using search term.
     */
    public LazyFileMetadataModel setLoadedSearchDataSize(int loadedSearchDataSize) {
        this.loadedSearchDataSize = loadedSearchDataSize;
        return this;
    }

    /**
     * Used to override data so for ex. when using search the data needs to be the same as
     * {@link LazyDataModel#getWrappedData()} in order make checkboxes work correctly.
     */
    public LazyFileMetadataModel setLoadedData(List<FileMetadata> loadedData) {
        this.loadedData = loadedData;
        return this;
    }
}
