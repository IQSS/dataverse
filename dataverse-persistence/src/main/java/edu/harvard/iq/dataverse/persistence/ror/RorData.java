package edu.harvard.iq.dataverse.persistence.ror;

import edu.harvard.iq.dataverse.persistence.JpaEntity;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Entity
public class RorData implements JpaEntity<Long>, Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 9, unique = true)
    private String rorId;

    @Column
    private String name;

    @Column
    private String countryName;

    @Column(length = 16)
    private String countryCode;

    @Column
    private String website;

    @Column
    private String city;

    @ElementCollection
    @CollectionTable(name = "rordata_namealias", joinColumns = @JoinColumn(name = "rordata_id"))
    @Column(name = "namealias")
    private Set<String> nameAliases = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "rordata_acronym", joinColumns = @JoinColumn(name = "rordata_id"))
    @Column(name = "acronym")
    private Set<String> acronyms = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "rordata_label", joinColumns = @JoinColumn(name = "rordata_id"))
    private Set<RorLabel> labels = new HashSet<>();

    // -------------------- CONSTRUCTORS --------------------

    public RorData() { }

    public RorData(String rorId, String name, String countryName, String countryCode, String website, String city,
                   Set<String> nameAliases, Set<String> acronyms, Set<RorLabel> labels) {
        this.rorId = rorId;
        this.name = name;
        this.countryName = countryName;
        this.countryCode = countryCode;
        this.website = website;
        this.city = city;
        this.nameAliases.addAll(nameAliases);
        this.acronyms.addAll(acronyms);
        this.labels.addAll(labels);
    }

    // -------------------- GETTERS --------------------

    public Long getId() {
        return id;
    }

    public String getRorId() {
        return rorId;
    }

    public String getName() {
        return name;
    }

    public String getCountryName() {
        return countryName;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public Set<String> getNameAliases() {
        return nameAliases;
    }

    public Set<String> getAcronyms() {
        return acronyms;
    }

    public Set<RorLabel> getLabels() {
        return labels;
    }

    public String getWebsite() {
        return website;
    }

    public String getCity() {
        return city;
    }

    // -------------------- SETTERS --------------------

    public RorData setId(Long id) {
        this.id = id;
        return this;
    }

    public void setRorId(String rorId) {
        this.rorId = rorId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public void setCity(String city) {
        this.city = city;
    }
}
