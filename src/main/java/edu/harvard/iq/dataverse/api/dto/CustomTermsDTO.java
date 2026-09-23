package edu.harvard.iq.dataverse.api.dto;

import edu.harvard.iq.dataverse.TermsOfUseAndAccess;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Custom dataset terms of access payload, including use terms, restrictions, citation requirements, conditions, and disclaimer text.")
public class CustomTermsDTO {
    private String termsOfUse;
    private String confidentialityDeclaration;
    private String specialPermissions;
    private String restrictions;
    private String citationRequirements;
    private String depositorRequirements;
    private String conditions;
    private String disclaimer;

    public String getTermsOfUse() {
        return termsOfUse;
    }

    public void setTermsOfUse(String termsOfUse) {
        this.termsOfUse = termsOfUse;
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

    public TermsOfUseAndAccess toTermsOfUseAndAccess() {
        TermsOfUseAndAccess termsOfUseAndAccess = new TermsOfUseAndAccess();
        termsOfUseAndAccess.setTermsOfUse(termsOfUse);
        termsOfUseAndAccess.setConfidentialityDeclaration(confidentialityDeclaration);
        termsOfUseAndAccess.setSpecialPermissions(specialPermissions);
        termsOfUseAndAccess.setRestrictions(restrictions);
        termsOfUseAndAccess.setCitationRequirements(citationRequirements);
        termsOfUseAndAccess.setDepositorRequirements(depositorRequirements);
        termsOfUseAndAccess.setConditions(conditions);
        termsOfUseAndAccess.setDisclaimer(disclaimer);
        return termsOfUseAndAccess;
    }
}
