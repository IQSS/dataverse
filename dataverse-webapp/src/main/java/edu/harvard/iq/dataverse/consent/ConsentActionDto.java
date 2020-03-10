package edu.harvard.iq.dataverse.consent;

import edu.harvard.iq.dataverse.persistence.consent.ConsentActionType;

public class ConsentActionDto {

    private long id;
    private ConsentActionType consentActionType;
    private String actionOptions;
    private ConsentDto owner;

    // -------------------- CONSTRUCTORS --------------------

    public ConsentActionDto(long id, ConsentActionType consentActionType, String actionOptions, ConsentDto owner) {
        this.id = id;
        this.consentActionType = consentActionType;
        this.actionOptions = actionOptions;
        this.owner = owner;
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

    public ConsentDto getOwner() {
        return owner;
    }
}
