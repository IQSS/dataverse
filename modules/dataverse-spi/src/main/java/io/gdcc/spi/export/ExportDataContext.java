package io.gdcc.spi.export;

/**
 *
 * @author landreev
 * Provides an optional mechanism for defining various data retrieval options 
 * for the export subsystem in a way that should allow us adding support for 
 * more options going forward with minimal or no changes to the already 
 * implemented export plugins. 
 */
public class ExportDataContext {
    private boolean datasetMetadataOnly = false; 
    private boolean publicFilesOnly = false;
    private Integer offset = null; 
    private Integer length = null; 
    
    private ExportDataContext() {
        
    }
    
    public static ExportDataContext context() {
        ExportDataContext context = new ExportDataContext();
        return context; 
    }
    
    public ExportDataContext withDatasetMetadataOnly() {
        this.datasetMetadataOnly = true;
        return this; 
    }
    
    public ExportDataContext withPublicFilesOnly() {
        this.publicFilesOnly = true;
        return this; 
    }
    
    public ExportDataContext withOffset(Integer offset) {
        this.offset = offset; 
        return this;
    }
    
    public ExportDataContext withLength(Integer length) {
        this.length = length; 
        return this;
    }
    
    public boolean isDatasetMetadataOnly() {
        return datasetMetadataOnly;
    }
    
    public boolean isPublicFilesOnly() {
        return publicFilesOnly;
    }
    
    public Integer getOffset() {
        return offset; 
    }
    
    public Integer getLength() {
        return length; 
    }    
}
