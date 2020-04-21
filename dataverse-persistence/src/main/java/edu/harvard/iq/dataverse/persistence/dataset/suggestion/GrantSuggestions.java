package edu.harvard.iq.dataverse.persistence.dataset.suggestion;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.util.Locale;

@Entity
public class GrantSuggestions {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String grantAgency;

    private String grantAgencyAcronym;

    private String fundingProgram;

    private String foreignName;

    private Locale foreignNameLocale;

    // -------------------- CONSTRUCTORS --------------------

    @Deprecated
    public GrantSuggestions() {
    }

    public GrantSuggestions(String grantAgency, String grantAgencyAcronym, String fundingProgram, String foreignName, Locale foreignNameLocale) {
        this.grantAgency = grantAgency;
        this.grantAgencyAcronym = grantAgencyAcronym;
        this.fundingProgram = fundingProgram;
        this.foreignName = foreignName;
        this.foreignNameLocale = foreignNameLocale;
    }

    // -------------------- GETTERS --------------------


    public Long getId() {
        return id;
    }

    public String getGrantAgency() {
        return grantAgency;
    }

    public String getGrantAgencyAcronym() {
        return grantAgencyAcronym;
    }

    public String getFundingProgram() {
        return fundingProgram;
    }

    public String getForeignName() {
        return foreignName;
    }

    public Locale getForeignNameLocale() {
        return foreignNameLocale;
    }
}
