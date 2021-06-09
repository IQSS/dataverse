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
import java.util.List;
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

    public RorData(String rorId, String name, String countryName, String countryCode,
                   Set<String> nameAliases, Set<String> acronyms, List<RorLabel> labels) {
        this.rorId = rorId;
        this.name = name;
        this.countryName = countryName;
        this.countryCode = countryCode;
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

    // -------------------- SETTERS --------------------

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
}
