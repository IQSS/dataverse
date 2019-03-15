package edu.harvard.iq.dataverse.license;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import com.google.common.base.Preconditions;

/**
 * Entity class representing license.
 * Licenses can be attached to data files.
 * Licenses determine on what terms files can
 * be used by users of the application.
 * 
 * @author madryk
 */
@Entity
public class License implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, unique=true)
    private String name;

    @Column(nullable=false)
    private String url;

    @OneToOne(mappedBy="license", cascade=CascadeType.ALL, optional=true)
    private LicenseIcon icon;

    private boolean active;

    @Column(nullable=false)
    private Long position;

    @ElementCollection
    @CollectionTable(name="license_localizedname")
    private List<LocaleText> localizedNames = new ArrayList<>();

    
    //-------------------- GETTERS --------------------
    
    /**
     * Returns database id of license
     */
    public Long getId() {
        return id;
    }

    /**
     * Returns universal name of license that can be presented
     * in external representations (such as Dublin Core, DDI, etc.)
     * or if locale specific version of name is not present
     */
    public String getName() {
        return name;
    }

    /**
     * Returns url to page where license is described in details
     */
    public String getUrl() {
        return url;
    }

    /**
     * Returns graphical representation of license
     */
    public LicenseIcon getIcon() {
        return icon;
    }

    /**
     * Returns true if license can be assigned
     * to newly created data files.
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Returns on what position license should be displayed
     * when presenting all or subset of all licenses.
     */
    public Long getPosition() {
        return position;
    }

    /**
     * Returns list with locale specific names of license
     */
    public List<LocaleText> getLocalizedNames() {
        return Collections.unmodifiableList(localizedNames);
    }
    
    
    //-------------------- LOGIC --------------------
    
    /**
     * Adds locale specific name of the license.
     */
    public void addLocalizedName(LocaleText localizedName) {
        Preconditions.checkNotNull(localizedName);
        Preconditions.checkArgument(!containsNameWithLocale(localizedName.getLocale()));
        
        localizedNames.add(localizedName);
    }
    
    /**
     * Returns true if license already have localized name
     * with the given locale.
     */
    public boolean containsNameWithLocale(Locale locale) {
        for (LocaleText localeText : localizedNames) {
            if (localeText.getLocale().equals(locale)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Returns localized version of license name
     * if it exists or {@link #getName()} otherwise.
     */
    public String getLocalizedName(Locale locale) {
        for (LocaleText localeText : localizedNames) {
            if (localeText.getLocale().equals(locale)) {
                return localeText.getText();
            }
        }
        return getName();
    }

    //-------------------- SETTERS --------------------
    
    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setIcon(LicenseIcon icon) {
        if (icon != null) {
            icon.setLicense(this);
        }
        this.icon = icon;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setPosition(Long position) {
        this.position = position;
    }

    @SuppressWarnings("unused") /** for jpa only */
    private void setLocalizedNames(List<LocaleText> localizedNames) {
        this.localizedNames = localizedNames;
    }

}
