package edu.harvard.iq.dataverse.consent.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

public class ConsentApiDto {

    private Long id;
    private String name;
    private List<ConsentDetailsApiDto> consentDetails;
    private List<ConsentActionApiDto> consentActions;
    private int displayOrder;
    private boolean required;
    private boolean hidden;

    // -------------------- CONSTRUCTORS --------------------

    public ConsentApiDto(@JsonProperty(value = "id")Long id,
                         @JsonProperty(value = "name", required = true) String name,
                         @JsonProperty(value = "displayOrder", required = true) Integer displayOrder,
                         @JsonProperty(value = "required", required = true) Boolean required,
                         @JsonProperty(value = "hidden", required = true) Boolean hidden,
                         @JsonProperty(value = "consentDetails", required = true) List<ConsentDetailsApiDto> consentDetails,
                         @JsonProperty(value = "consentActions") List<ConsentActionApiDto> consentActions){
        Objects.requireNonNull(name);
        Objects.requireNonNull(displayOrder);
        Objects.requireNonNull(required);
        Objects.requireNonNull(hidden);
        Objects.requireNonNull(consentDetails);

        this.id = id;
        this.name = name;
        this.displayOrder = displayOrder;
        this.required = required;
        this.hidden = hidden;
        this.consentDetails = consentDetails;
        this.consentActions = consentActions != null ? consentActions : Lists.newArrayList();
    }

    // -------------------- GETTERS --------------------

    @Nullable
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<ConsentDetailsApiDto> getConsentDetails() {
        return consentDetails;
    }

    public List<ConsentActionApiDto> getConsentActions() {
        return consentActions;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public boolean isRequired() {
        return required;
    }

    public boolean isHidden() {
        return hidden;
    }

    // -------------------- SETTERS --------------------

    public void setId(Long id) {
        this.id = id;
    }
}
