package io.gdcc.spi.export;

/**
 *
 * @author landreev
 * Provides a mechanism for defining various data retrieval options for the 
 * export subsystem in a way that should allow us adding support for more 
 * options going forward with minimal or no changes to the existing code in 
 * export plugins. 
 */
public class ExportDataOption {
    
    public enum SupportedOptions {
        DatasetMetadataOnly,
        PublicFilesOnly;
    }
    
    private SupportedOptions optionType; 
    
    /*public static ExportDataOption addOption(String option) {
        ExportDataOption ret = new ExportDataOption(); 
        
        for (SupportedOptions supported : SupportedOptions.values()) {
            if (supported.toString().equals(option)) {
                ret.optionType = supported;
            }
        }
        return ret; 
    }*/
    
    public static ExportDataOption addDatasetMetadataOnly() {
        ExportDataOption ret = new ExportDataOption();
        ret.optionType = SupportedOptions.DatasetMetadataOnly;
        return ret; 
    }
    
    public static ExportDataOption addPublicFilesOnly() {
        ExportDataOption ret = new ExportDataOption();
        ret.optionType = SupportedOptions.PublicFilesOnly;
        return ret; 
    }
    
    public boolean isDatasetMetadataOnly() {
        return SupportedOptions.DatasetMetadataOnly.equals(optionType);
    }
    
    public boolean isPublicFilesOnly() {
        return SupportedOptions.PublicFilesOnly.equals(optionType);
    }
}
