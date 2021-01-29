/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.CascadeType;
import javax.persistence.OneToOne;
import javax.persistence.MapsId;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import java.util.Date;


/**
 *
 * @author marina
 */
@Entity
public class FileDownload implements Serializable { 
    
    @Id
    private Long id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    private GuestbookResponse guestbookResponse;
    
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date downloadTimestamp;
    
    /*
    Transient Values carry non-written information 
    that will assist in the download process
    - selected file ids is a comma delimited list that contains the file ids for multiple download
    - fileFormat tells the download api which format a subsettable file should be downloaded as
    */
    
    @Transient
    private String selectedFileIds;
    
    @Transient 
    private String fileFormat;
    
    
    /**
     * Possible values for downloadType include "Download", "Subset",
     * or the displayName of an ExternalTool.
     *
     * TODO: Types like "Download" and "Subset" should
     * be defined once as constants (likely an enum) rather than having these
     * strings duplicated in various places when setDownloadtype() is called.
     */
    private String downloadtype;
    private String sessionId;
    
     public FileDownload(){
        
    }
     
    public FileDownload(FileDownload source){
        this.setDownloadTimestamp(source.getDownloadTimestamp());
        this.setDownloadtype(source.getDownloadtype());
        this.setFileFormat(source.getFileFormat());
        this.setGuestbookResponse(source.getGuestbookResponse());
        this.setSelectedFileIds(source.getSelectedFileIds());
        this.setSessionId(source.getSessionId());
    }
    
    public String getFileFormat() {
        return fileFormat;
    }

    //for download
    public void setFileFormat(String downloadFormat) {
        this.fileFormat = downloadFormat;
    }
    
    public String getDownloadtype() {
        return downloadtype;
    }

    public void setDownloadtype(String downloadtype) {
        this.downloadtype = downloadtype;
    }
    
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getSelectedFileIds() {
        return selectedFileIds;
    }

    public void setSelectedFileIds(String selectedFileIds) {
        this.selectedFileIds = selectedFileIds;
    }
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    
    public Date getDownloadTimestamp(){
        return this.downloadTimestamp;
    }
    
    public void setDownloadTimestamp(Date downloadTimestamp){
        this.downloadTimestamp = downloadTimestamp;
    }
    
    
    public void setGuestbookResponse(GuestbookResponse gbr){
        this.guestbookResponse = gbr;
    }

    public GuestbookResponse getGuestbookResponse(){
        return this.guestbookResponse;
    }
    
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof FileDownload)) {
            return false;
        }
        FileDownload other = (FileDownload) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "edu.harvard.iq.dataverse.FileDownload[ id=" + id + " ]";
    }
    
    
}
