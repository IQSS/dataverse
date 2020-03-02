package edu.harvard.iq.dataverse.persistence.consent;

import edu.harvard.iq.dataverse.persistence.config.LocaleConverter;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.util.Locale;

@Entity
public class AcceptedConsent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(updatable = false)
    private String name;

    @Column(nullable = false, updatable = false)
    @Convert(converter = LocaleConverter.class)
    private Locale language;

    @Column(nullable = false, updatable = false)
    private String text;

    @Column(nullable = false)
    private boolean required;

    @ManyToOne
    @JoinColumn(nullable = false, updatable = false, name = "user_id")
    private AuthenticatedUser user;

    // -------------------- CONSTRUCTORS --------------------

    protected AcceptedConsent() {
    }

    public AcceptedConsent(ConsentDetails acceptedConsentDetails, AuthenticatedUser user) {
        this.name = acceptedConsentDetails.getConsent().getName();
        this.language = acceptedConsentDetails.getLanguage();
        this.text = acceptedConsentDetails.getText();
        this.required = acceptedConsentDetails.getConsent().isRequired();
        this.user = user;
    }

    // -------------------- GETTERS --------------------

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Locale getLanguage() {
        return language;
    }

    public String getText() {
        return text;
    }

    public boolean isRequired() {
        return required;
    }
}
