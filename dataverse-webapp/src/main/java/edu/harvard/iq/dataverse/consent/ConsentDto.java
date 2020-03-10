package edu.harvard.iq.dataverse.consent;

import java.util.ArrayList;
import java.util.List;

public class ConsentDto {

    private long id;
    private String name;
    private ConsentDetailsDto consentDetails;
    private List<ConsentActionDto> consentActions = new ArrayList<>();
    private int displayOrder;
    private boolean required;

    // -------------------- CONSTRUCTORS --------------------

    public ConsentDto(long id, String name, ConsentDetailsDto consentDetails, int displayOrder, boolean required) {
        this.id = id;
        this.name = name;
        this.consentDetails = consentDetails;
        this.displayOrder = displayOrder;
        this.required = required;
    }

    // -------------------- GETTERS --------------------

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public ConsentDetailsDto getConsentDetails() {
        return consentDetails;
    }

    public List<ConsentActionDto> getConsentActions() {
        return consentActions;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public boolean isRequired() {
        return required;
    }
}
