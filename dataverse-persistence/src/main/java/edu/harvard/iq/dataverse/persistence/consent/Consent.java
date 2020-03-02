package edu.harvard.iq.dataverse.persistence.consent;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;

/**
 * Class represents consents for users.
 * By nature removing/modifying anything from {@link ConsentDetails} is forbidden since we don't want to alter history.
 */
@Entity
public class Consent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, updatable = false)
    private String name;

    @OneToMany(mappedBy = "consent", cascade = CascadeType.ALL)
    private List<ConsentDetails> consentDetails = new ArrayList<>();

    @OneToMany(mappedBy = "consent", cascade = CascadeType.ALL)
    private List<ConsentAction> consentActions = new ArrayList<>();

    @Column(nullable = false)
    private int displayOrder;

    @Column(nullable = false)
    private boolean required;

    @Column(nullable = false)
    private boolean hidden;

    // -------------------- CONSTRUCTORS --------------------

    protected Consent() {
    }

    public Consent(Long id, String name, List<ConsentDetails> consentDetails,
                   int displayOrder, boolean required, boolean hidden) {
        this.id = id;
        this.name = name;
        this.consentDetails = consentDetails;
        this.displayOrder = displayOrder;
        this.required = required;
        this.hidden = hidden;
    }

    // -------------------- GETTERS --------------------

    public Long getId() {
        return id;
    }

    /**
     * Unique name for consent.
     */
    public String getName() {
        return name;
    }

    /**
     * Details about this consent, contains for example text that is specific to locale.
     */
    public List<ConsentDetails> getConsentDetails() {
        return consentDetails;
    }

    /**
     * Applicable actions for a given consent will be taken if for example user will accept it.
     * Can be empty.
     */
    public List<ConsentAction> getConsentActions() {
        return consentActions;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    /**
     * Determines if the consent is required to be accepted.
     */
    public boolean isRequired() {
        return required;
    }

    /**
     * Determines if the consent is hidden from the user.
     */
    public boolean isHidden() {
        return hidden;
    }

    // -------------------- SETTERS --------------------

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }
}
