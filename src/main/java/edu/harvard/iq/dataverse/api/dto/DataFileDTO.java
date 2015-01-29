
package edu.harvard.iq.dataverse.api.dto;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author ellenk
 */
public class DataFileDTO {
    private String fileName;
    private String id;
    private String name;
    private String contentType;
    private String filename;
    private String originalFileFormat;
    private String originalFormatLabel;
    private String UNF;
    private String md5;
    private String description;
    private List<DataTableDTO> dataTables = new ArrayList();

    public List<DataTableDTO> getDataTables() {
        return dataTables;
    }

    public void setDataTables(List<DataTableDTO> dataTables) {
        this.dataTables = dataTables;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getOriginalFileFormat() {
        return originalFileFormat;
    }

    public void setOriginalFileFormat(String originalFileFormat) {
        this.originalFileFormat = originalFileFormat;
    }

    public String getOriginalFormatLabel() {
        return originalFormatLabel;
    }

    public void setOriginalFormatLabel(String originalFormatLabel) {
        this.originalFormatLabel = originalFormatLabel;
    }

    public String getUNF() {
        return UNF;
    }

    public void setUNF(String UNF) {
        this.UNF = UNF;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    
    
}
