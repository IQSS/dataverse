package edu.harvard.iq.dataverse.persistence.consent;

import edu.harvard.iq.dataverse.persistence.config.PostgresJsonConverter;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * Class used to determinate which action to fortake
 */
@Entity
public class ConsentAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "consent_id")
    private Consent consent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConsentActionType consentActionType;

    @Convert(converter = PostgresJsonConverter.class)
    private String actionOptions;

    // -------------------- CONSTRUCTORS --------------------

    public ConsentAction() {
    }

    public ConsentAction(Consent consent, ConsentActionType consentActionType, String actionOptions) {
        this.consent = consent;
        this.consentActionType = consentActionType;
        this.actionOptions = actionOptions;
    }

    // -------------------- GETTERS --------------------

    public Long getId() {
        return id;
    }

    public Consent getConsent() {
        return consent;
    }

    /**
     * Enum used to determinate what to do for specific types.
     */
    public ConsentActionType getConsentActionType() {
        return consentActionType;
    }

    /**
     * Json based options that are specific to {@link ConsentActionType}
     * used to usually do something after consent was accepted by user.
     */
    public String getActionOptions() {
        return actionOptions;
    }

    // -------------------- SETTERS --------------------


    public void setId(Long id) {
        this.id = id;
    }

    public void setConsent(Consent consent) {
        this.consent = consent;
    }

    public void setConsentActionType(ConsentActionType consentActionType) {
        this.consentActionType = consentActionType;
    }

    public void setActionOptions(String actionOptions) {
        this.actionOptions = actionOptions;
    }
}
