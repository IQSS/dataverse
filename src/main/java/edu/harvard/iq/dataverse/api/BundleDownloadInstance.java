/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.api;

/**
 *
 * @author Leonid Andreev
 */
public class BundleDownloadInstance {
   

    private DownloadInfo downloadInfo = null;
    private String fileCitationEndNote = "";
    private String fileCitationRIS = "";
    private String fileDDIXML = "";
    private String fileCitationBibtex = "";

    public BundleDownloadInstance() {
        
    }
    
    public BundleDownloadInstance(DownloadInfo info) {
        this.downloadInfo = info;
    }

    public DownloadInfo getDownloadInfo() {
        return downloadInfo;
    }

    public void setDownloadInfo(DownloadInfo info) {
        this.downloadInfo = info;
    }

    public String getFileCitationEndNote() {
        return fileCitationEndNote;
    }
    
    public void setFileCitationEndNote(String fileCitationEndNote) {
        this.fileCitationEndNote = fileCitationEndNote;
    }
    
    public String getFileCitationRIS() {
        return fileCitationRIS;
    }
    
    public void setFileCitationRIS(String fileCitationRIS) {
        this.fileCitationRIS = fileCitationRIS;
    }
    
    public String getFileDDIXML() {
        return fileDDIXML;
    }
    
    public void setFileDDIXML(String fileDDIXML) {
        this.fileDDIXML = fileDDIXML;
    }

    public String getFileCitationBibtex() {
        return fileCitationBibtex;
    }

    public void setFileCitationBibtex(String fileCitationBibtex) {
        this.fileCitationBibtex = fileCitationBibtex;
    }

}
