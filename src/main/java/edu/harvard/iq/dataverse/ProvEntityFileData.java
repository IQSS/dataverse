package edu.harvard.iq.dataverse;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author madunlap
 */
public class ProvEntityFileData {
    String entityName;
    String fileName;
    String fileType;

     ProvEntityFileData(String entityName, String fileName, String fileType) {
        this.entityName = entityName;
        this.fileName = fileName;
        this.fileType = fileType;
    }
    
    public String getEntityName() {
        return entityName;
    }
    public String getFileName() {
        return fileName;
    }
    public String getFileType() {
        return fileType;
    }
}
