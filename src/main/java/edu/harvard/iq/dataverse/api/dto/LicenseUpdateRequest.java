package edu.harvard.iq.dataverse.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

// This DTO acts as a wrapper for the request body.
// It can accept EITHER a 'name' or a 'customTerms' object.
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LicenseUpdateRequest {

    private String name;
    private CustomTermsDTO customTerms;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CustomTermsDTO getCustomTerms() {
        return customTerms;
    }

    public void setCustomTerms(CustomTermsDTO customTerms) {
        this.customTerms = customTerms;
    }
}
