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
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import edu.harvard.iq.dataverse.license.License;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
@NamedQueries({
    // TermsOfUseOrLicense.findByDatasetVersionIdAndDefaultTerms
    // is used to determine if the dataset terms were set by the multi license support update
    // as part of the 5.10 release.

    @NamedQuery(name = "TermsOfUseOrLicense.findByDatasetVersionIdAndDefaultTerms",
                query = "SELECT o FROM TermsOfUseOrLicense o, DatasetVersion dv WHERE "
                        + "dv.id =:id "
                        + "AND dv.termsOfUseOrLicense.id = o.id "
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
@Table(name = "termsofuseorlicense")
public class TermsOfUseOrLicense implements Serializable {
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

    @OneToOne(mappedBy = "termsOfUseOrLicense")
    private DatasetVersion datasetVersion;

    public DatasetVersion getDatasetVersion() {
        return datasetVersion;
    }

    public void setDatasetVersion(DatasetVersion datasetVersion) {
        this.datasetVersion = datasetVersion;
    }

    @OneToOne(mappedBy = "termsOfUseOrLicense")
    private Template template;

    public Template getTemplate() {
        return template;
    }

    public void setTemplate(Template template) {
        this.template = template;
    }

    @ManyToOne
    @JoinColumn(name = "license_id")
    private License license;

    @Column(name = "termsofuse", columnDefinition = "TEXT")
    private String termsOfUse;

    @Column(name = "confidentialitydeclaration", columnDefinition = "TEXT")
    private String confidentialityDeclaration;

    @Column(name = "specialpermissions", columnDefinition = "TEXT")
    private String specialPermissions;

    @Column(name = "restrictions", columnDefinition = "TEXT")
    private String restrictions;

    @Column(name = "citationrequirements", columnDefinition = "TEXT")
    private String citationRequirements;

    @Column(name = "depositorrequirements", columnDefinition = "TEXT")
    private String depositorRequirements;

    @Column(name = "conditions", columnDefinition = "TEXT")
    private String conditions;

    @Column(name = "disclaimer", columnDefinition = "TEXT")
    private String disclaimer;

    public License getLicense() {
        return license;
    }

    public void setLicense(License license) {
        this.license = license;
        if(license!=null) {
            //Enforce restriction that customTerms can't be used with a license
            clearCustomTermsVariables();
        }
    }

    public String getTermsOfUse() {
        return termsOfUse;
    }

    public void setTermsOfUse(String termsOfUse) {
        this.termsOfUse = termsOfUse;
        if (termsOfUse != null) {
            //Enforce restriction that customTerms can't be used with a license
            this.license = null;
        }
    }

    public String getConfidentialityDeclaration() {
        return confidentialityDeclaration;
    }

    public void setConfidentialityDeclaration(String confidentialityDeclaration) {
        this.confidentialityDeclaration = confidentialityDeclaration;
        if (confidentialityDeclaration != null) {
            //Enforce restriction that customTerms can't be used with a license
            this.license = null;
        }
    }

    public String getSpecialPermissions() {
        return specialPermissions;
    }

    public void setSpecialPermissions(String specialPermissions) {
        this.specialPermissions = specialPermissions;
        if (specialPermissions != null) {
            //Enforce restriction that customTerms can't be used with a license
            this.license = null;
        }
    }

    public String getRestrictions() {
        return restrictions;
    }

    public void setRestrictions(String restrictions) {
        this.restrictions = restrictions;
        if (restrictions != null) {
            //Enforce restriction that customTerms can't be used with a license
            this.license = null;
        }
    }

    public String getCitationRequirements() {
        return citationRequirements;
    }

    public void setCitationRequirements(String citationRequirements) {
        this.citationRequirements = citationRequirements;
        if (citationRequirements != null) {
            //Enforce restriction that customTerms can't be used with a license
            this.license = null;
        }
    }

    public String getDepositorRequirements() {
        return depositorRequirements;
    }

    public void setDepositorRequirements(String depositorRequirements) {
        this.depositorRequirements = depositorRequirements;
        if (depositorRequirements != null) {
            //Enforce restriction that customTerms can't be used with a license
            this.license = null;
        }
    }

    public String getConditions() {
        return conditions;
    }

    public void setConditions(String conditions) {
        this.conditions = conditions;
        if (conditions != null) {
            //Enforce restriction that customTerms can't be used with a license
            this.license = null;
        }
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
        if (disclaimer != null) {
            //Enforce restriction that customTerms can't be used with a license
            this.license = null;
        }
    }

    public TermsOfUseOrLicense copyTermsOfUseOrLicense(){

        TermsOfUseOrLicense retVal = new TermsOfUseOrLicense();
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
        if (!(object instanceof TermsOfUseOrLicense)) {
            return false;
        }
        TermsOfUseOrLicense other = (TermsOfUseOrLicense) object;
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

