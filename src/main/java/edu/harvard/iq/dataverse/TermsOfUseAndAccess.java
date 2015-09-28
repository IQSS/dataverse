/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;

/**
 *
 * 
 * @author skraffmi
 */
@Entity
public class TermsOfUseAndAccess implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    
    @OneToOne(mappedBy = "termsOfUseAndAccess")
    private DatasetVersion datasetVersion;

    public DatasetVersion getDatasetVersion() {
        return datasetVersion;
    }

    public void setDatasetVersion(DatasetVersion datasetVersion) {
        this.datasetVersion = datasetVersion;
    }
    
    @OneToOne(mappedBy = "termsOfUseAndAccess")
    private Template template;

    public Template getTemplate() {
        return template;
    }

    public void setTemplate(Template template) {
        this.template = template;
    }
    
    
    @Enumerated(EnumType.STRING)
    private TermsOfUseAndAccess.License license;
    
    @Column(columnDefinition="TEXT")      
    private String termsOfUse;
    
    @Column(columnDefinition="TEXT") 
    private String termsOfAccess;
    
    @Column(columnDefinition="TEXT") 
    private String confidentialityDeclaration;
    
    @Column(columnDefinition="TEXT") 
    private String specialPermissions;
    
    @Column(columnDefinition="TEXT") 
    private String restrictions;
    
    @Column(columnDefinition="TEXT") 
    private String citationRequirements;
    
    @Column(columnDefinition="TEXT") 
    private String depositorRequirements;
    
    @Column(columnDefinition="TEXT") 
    private String conditions;
    
    @Column(columnDefinition="TEXT") 
    private String disclaimer;
    
    @Column(columnDefinition="TEXT") 
    private String dataAccessPlace;
    
    @Column(columnDefinition="TEXT") 
    private String originalArchive;
    
    @Column(columnDefinition="TEXT") 
    private String availabilityStatus;
    
    @Column(columnDefinition="TEXT") 
    private String contactForAccess;
    
    @Column(columnDefinition="TEXT") 
    private String sizeOfCollection;
    
    @Column(columnDefinition="TEXT") 
    private String studyCompletion;
    
    private boolean fileAccessRequest;

    public boolean isFileAccessRequest() {
        return fileAccessRequest;
    }

    public void setFileAccessRequest(boolean fileAccessRequest) {
        this.fileAccessRequest = fileAccessRequest;
    }
    
    public TermsOfUseAndAccess.License getLicense() {
        return license;
    }

    public void setLicense(TermsOfUseAndAccess.License license) {
        this.license = license;
    }

    public String getTermsOfUse() {
        return termsOfUse;
    }

    public void setTermsOfUse(String termsOfUse) {
        this.termsOfUse = termsOfUse;
    }

    public String getTermsOfAccess() {
        return termsOfAccess;
    }

    public void setTermsOfAccess(String termsOfAccess) {
        this.termsOfAccess = termsOfAccess;
    }

    public String getConfidentialityDeclaration() {
        return confidentialityDeclaration;
    }

    public void setConfidentialityDeclaration(String confidentialityDeclaration) {
        this.confidentialityDeclaration = confidentialityDeclaration;
    }

    public String getSpecialPermissions() {
        return specialPermissions;
    }

    public void setSpecialPermissions(String specialPermissions) {
        this.specialPermissions = specialPermissions;
    }

    public String getRestrictions() {
        return restrictions;
    }

    public void setRestrictions(String restrictions) {
        this.restrictions = restrictions;
    }

    public String getCitationRequirements() {
        return citationRequirements;
    }

    public void setCitationRequirements(String citationRequirements) {
        this.citationRequirements = citationRequirements;
    }

    public String getDepositorRequirements() {
        return depositorRequirements;
    }

    public void setDepositorRequirements(String depositorRequirements) {
        this.depositorRequirements = depositorRequirements;
    }

    public String getConditions() {
        return conditions;
    }

    public void setConditions(String conditions) {
        this.conditions = conditions;
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
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
    
        
    public TermsOfUseAndAccess copyTermsOfUseAndAccess(){

        TermsOfUseAndAccess retVal = new TermsOfUseAndAccess();
        retVal.setAvailabilityStatus(this.getAvailabilityStatus());
        retVal.setCitationRequirements(this.getCitationRequirements());
        retVal.setConditions(this.getConditions());
        retVal.setConfidentialityDeclaration(this.getConfidentialityDeclaration());
        retVal.setContactForAccess(this.getContactForAccess());
        retVal.setDataAccessPlace(this.getDataAccessPlace());
        retVal.setDepositorRequirements(this.getDepositorRequirements());
        retVal.setDisclaimer(this.getDisclaimer());
        retVal.setLicense(this.getLicense());
        retVal.setOriginalArchive(this.getOriginalArchive());
        retVal.setRestrictions(this.getRestrictions());
        retVal.setSizeOfCollection(this.getSizeOfCollection());
        retVal.setSpecialPermissions(this.getSpecialPermissions());
        retVal.setStudyCompletion(this.getStudyCompletion());
        retVal.setTermsOfAccess(this.getTermsOfAccess());
        retVal.setTermsOfUse(this.getTermsOfUse());
        retVal.setFileAccessRequest(this.isFileAccessRequest());

        return retVal;
    }

    
        
    public enum License {
        NONE, CC0
    }
    
        /**
     * @todo What does the GUI use for a default license? What does the "native"
     * API use? See also https://github.com/IQSS/dataverse/issues/1385
     */
    public static TermsOfUseAndAccess.License defaultLicense = TermsOfUseAndAccess.License.CC0;

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof TermsOfUseAndAccess)) {
            return false;
        }
        TermsOfUseAndAccess other = (TermsOfUseAndAccess) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "edu.harvard.iq.dataverse.TermsOfUseAndAccess[ id=" + id + " ]";
    }
    
}
