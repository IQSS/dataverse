/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 *
 * 
 * @author skraffmi
 */
@Entity
@Table(name = "termsofaccess")
@ValidateTermsOfAccess
public class TermsOfAccess implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    
    @OneToOne(mappedBy = "termsOfAccess")
    private DatasetVersion datasetVersion;

    public DatasetVersion getDatasetVersion() {
        return datasetVersion;
    }

    public void setDatasetVersion(DatasetVersion datasetVersion) {
        this.datasetVersion = datasetVersion;
    }
    
    @OneToOne(mappedBy = "termsOfAccess")
    private Template template;

    public Template getTemplate() {
        return template;
    }

    public void setTemplate(Template template) {
        this.template = template;
    }
    

    @Column(name = "termsofaccess", columnDefinition = "TEXT")
    private String termsOfAccess;
    
    @Column(name = "dataaccessplace", columnDefinition = "TEXT")
    private String dataAccessPlace;
    
    @Column(name = "originalarchive", columnDefinition = "TEXT")
    private String originalArchive;
    
    @Column(name = "availabilitystatus", columnDefinition = "TEXT")
    private String availabilityStatus;
    
    @Column(name = "contactforaccess", columnDefinition = "TEXT")
    private String contactForAccess;
    
    @Column(name = "sizeofcollection", columnDefinition = "TEXT")
    private String sizeOfCollection;
    
    @Column(name = "studycompletion", columnDefinition = "TEXT")
    private String studyCompletion;

    @Column(name = "fileaccessrequest")
    private boolean fileAccessRequest;

    public boolean isFileAccessRequest() {
        return fileAccessRequest;
    }

    public void setFileAccessRequest(boolean fileAccessRequest) {
        this.fileAccessRequest = fileAccessRequest;
    }

    public String getTermsOfAccess() {
        return termsOfAccess;
    }

    public void setTermsOfAccess(String termsOfAccess) {
        this.termsOfAccess = termsOfAccess;
    }

    public String getDataAccessPlace() {
        return dataAccessPlace;
    }

    public void setDataAccessPlace(String dataAccessPlace) {
        this.dataAccessPlace = dataAccessPlace;
    }

    public String getOriginalArchive() {
        return originalArchive;
    }

    public void setOriginalArchive(String originalArchive) {
        this.originalArchive = originalArchive;
    }

    public String getAvailabilityStatus() {
        return availabilityStatus;
    }

    public void setAvailabilityStatus(String availabilityStatus) {
        this.availabilityStatus = availabilityStatus;
    }

    public String getContactForAccess() {
        return contactForAccess;
    }

    public void setContactForAccess(String contactForAccess) {
        this.contactForAccess = contactForAccess;
    }

    public String getSizeOfCollection() {
        return sizeOfCollection;
    }

    public void setSizeOfCollection(String sizeOfCollection) {
        this.sizeOfCollection = sizeOfCollection;
    }

    public String getStudyCompletion() {
        return studyCompletion;
    }

    public void setStudyCompletion(String studyCompletion) {
        this.studyCompletion = studyCompletion;
    }
    
        
    public TermsOfAccess copyTermsOfAccess(){

        TermsOfAccess retVal = new TermsOfAccess();
        retVal.setAvailabilityStatus(this.getAvailabilityStatus());
        retVal.setContactForAccess(this.getContactForAccess());
        retVal.setDataAccessPlace(this.getDataAccessPlace());
        retVal.setOriginalArchive(this.getOriginalArchive());
        retVal.setSizeOfCollection(this.getSizeOfCollection());
        retVal.setStudyCompletion(this.getStudyCompletion());
        retVal.setTermsOfAccess(this.getTermsOfAccess());
        retVal.setFileAccessRequest(this.isFileAccessRequest());
        return retVal;
    }

    @Transient
    private String validationMessage;

    public String getValidationMessage() {
        return validationMessage;
    }

    public void setValidationMessage(String validationMessage) {
        this.validationMessage = validationMessage;
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
        if (!(object instanceof TermsOfAccess)) {
            return false;
        }
        TermsOfAccess other = (TermsOfAccess) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return this.getClass().getCanonicalName() + "[ id=" + id + " ]";
    }
    
}
