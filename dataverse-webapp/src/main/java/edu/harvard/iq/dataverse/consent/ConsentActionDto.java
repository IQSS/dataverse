package edu.harvard.iq.dataverse.consent;

import edu.harvard.iq.dataverse.persistence.consent.ConsentActionType;

public class ConsentActionDto {

    private long id;
    private ConsentActionType consentActionType;
    private String actionOptions;

    // -------------------- CONSTRUCTORS --------------------

    public ConsentActionDto(long id, ConsentActionType consentActionType, String actionOptions) {
        this.id = id;
        this.consentActionType = consentActionType;
        this.actionOptions = actionOptions;
    }

    // -------------------- GETTERS --------------------

    public long getId() {
        return id;
    }

    public ConsentActionType getConsentActionType() {
        return consentActionType;
    }

    public String getActionOptions() {
        return actionOptions;
    }
}
