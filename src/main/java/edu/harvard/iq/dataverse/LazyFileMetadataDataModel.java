/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.util.List;
import java.util.Map;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;

/**
 *
 * @author skraffmi
 */
public class LazyFileMetadataDataModel extends LazyDataModel<FileMetadata> {
    
    private final DataFileServiceBean fileServiceBean;
    private final Long datasetVersionId;

    public LazyFileMetadataDataModel(Long datasetVersionId, DataFileServiceBean fileServiceBean) {
        this.fileServiceBean = fileServiceBean;
        this.datasetVersionId = datasetVersionId;
    }
    
    
    @Override
    public List<FileMetadata> load(int first, int pageSize, String sortField,
            SortOrder sortOrder, Map<String, FilterMeta> filters) {

        List<FileMetadata>  listFileMetadata = null; //fileServiceBean.findFileMetadataByDatasetVersionIdLazy(datasetVersionId, pageSize, sortField, sortField, first);
        //this.setRowCount(fileServiceBean.findCountByDatasetVersionId(datasetVersionId).intValue());
        return listFileMetadata;
    }
    
    
}
