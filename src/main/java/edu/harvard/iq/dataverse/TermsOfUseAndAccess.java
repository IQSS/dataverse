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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Transient;

import edu.harvard.iq.dataverse.license.License;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;

@NamedQueries({
    // TermsOfUseAndAccess.findByDatasetVersionIdAndDefaultTerms 
    // is used to determine if the dataset terms were set by the multi license support update 
    // as part of the 5.10 release.
    
    @NamedQuery(name = "TermsOfUseAndAccess.findByDatasetVersionIdAndDefaultTerms", 
                query = "SELECT o FROM TermsOfUseAndAccess o, DatasetVersion dv WHERE "
                        + "dv.id =:id "
                        + "AND dv.termsOfUseAndAccess.id = o.id "
                        + "AND o.termsOfUse =:defaultTerms "
                        + "AND o.confidentialityDeclaration IS null " 
                        + "AND o.specialPermissions IS null "
                        + "AND o.restrictions IS null "
                        + "AND o.citationRequirements IS null "
                        + "AND o.depositorRequirements IS null "
                        + "AND o.conditions IS null "
                        + "AND o.disclaimer IS null "
    )
})

/**
 *
 * 
 * @author skraffmi
 */
@Entity
@ValidateTermsOfUseAndAccess
public class TermsOfUseAndAccess implements Serializable {
    
    public static final String DEFAULT_NOTERMS = "This dataset is made available without information on how it can be used. You should communicate with the Contact(s) specified before use.";

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
    
    @ManyToOne
    @JoinColumn(name="license_id")
    private License license;

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
    
    public License getLicense() {
        return license;
    }

    public void setLicense(License license) {
        this.license = license;
        if(license!=null) {
            clearCustomTermsVariables();
        }
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
        retVal.setContactForAccess(this.getContactForAccess());
        retVal.setDataAccessPlace(this.getDataAccessPlace());
        retVal.setOriginalArchive(this.getOriginalArchive());
        retVal.setSizeOfCollection(this.getSizeOfCollection());
        retVal.setStudyCompletion(this.getStudyCompletion());
        retVal.setTermsOfAccess(this.getTermsOfAccess());
        retVal.setFileAccessRequest(this.isFileAccessRequest());
        retVal.setLicense(this.getLicense());
        if (license == null) {
            retVal.setTermsOfUse(this.getTermsOfUse());
            retVal.setConfidentialityDeclaration(this.getConfidentialityDeclaration());
            retVal.setSpecialPermissions(this.getSpecialPermissions());
            retVal.setRestrictions(this.getRestrictions());
            retVal.setCitationRequirements(this.getCitationRequirements());
            retVal.setDepositorRequirements(this.getDepositorRequirements());
            retVal.setConditions(this.getConditions());
            retVal.setDisclaimer(this.getDisclaimer());
        }

        return retVal;
    }

    private void clearCustomTermsVariables(){
        termsOfUse = null;
        confidentialityDeclaration = null;
        specialPermissions = null;
        restrictions = null;
        citationRequirements = null;
        depositorRequirements = null;
        conditions = null;
        disclaimer = null;
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
