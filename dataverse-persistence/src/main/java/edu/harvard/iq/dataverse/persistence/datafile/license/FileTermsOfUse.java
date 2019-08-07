package edu.harvard.iq.dataverse.persistence.datafile.license;

import edu.harvard.iq.dataverse.persistence.datafile.license.License;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import java.io.Serializable;

/**
 * Entity describing on what terms
 * file can be used by app users.
 *
 * @author madryk
 */
@Entity
public class FileTermsOfUse implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum TermsOfUseType {
        LICENSE_BASED,
        ALL_RIGHTS_RESERVED,
        RESTRICTED
    }

    public enum RestrictType {
        ACADEMIC_PURPOSE,
        NOT_FOR_REDISTRIBUTION,
        ACADEMIC_PURPOSE_AND_NOT_FOR_REDISTRIBUTION,
        CUSTOM
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(mappedBy = "termsOfUse")
    private FileMetadata fileMetadata;

    @ManyToOne
    private License license;

    private boolean allRightsReserved;

    @Enumerated(EnumType.STRING)
    private RestrictType restrictType;

    @Column(columnDefinition = "TEXT")
    private String restrictCustomText;


    // -------------------- GETTERS --------------------

    public Long getId() {
        return id;
    }

    /**
     * Returns file metadata associated with this
     * terms of use
     */
    public FileMetadata getFileMetadata() {
        return fileMetadata;
    }

    /**
     * Returns license describing terms
     * of use if {@link #getTermsOfUseType()} is
     * equal to {@link TermsOfUseType#LICENSE_BASED}
     */
    public License getLicense() {
        return license;
    }

    /**
     * Returns true if all rights are reserved
     * for associated file
     */
    public boolean isAllRightsReserved() {
        return allRightsReserved;
    }

    /**
     * Returns type of restriction if {@link #getTermsOfUseType()} is
     * equal to {@link TermsOfUseType#RESTRICTED}
     */
    public RestrictType getRestrictType() {
        return restrictType;
    }

    /**
     * Returns text describing on what terms
     * associated file is accessible
     */
    public String getRestrictCustomText() {
        return restrictCustomText;
    }

    // -------------------- LOGIC --------------------

    public TermsOfUseType getTermsOfUseType() {
        if (license != null) {
            return TermsOfUseType.LICENSE_BASED;
        }
        if (allRightsReserved) {
            return TermsOfUseType.ALL_RIGHTS_RESERVED;
        }
        if (restrictType != null) {
            return TermsOfUseType.RESTRICTED;
        }
        throw new RuntimeException("Unknown terms of use type");
    }

    public FileTermsOfUse createCopy() {
        FileTermsOfUse copy = new FileTermsOfUse();
        copy.setFileMetadata(getFileMetadata());
        copy.setLicense(getLicense());
        copy.setAllRightsReserved(isAllRightsReserved());
        copy.setRestrictType(getRestrictType());
        copy.setRestrictCustomText(getRestrictCustomText());
        return copy;
    }

    // -------------------- SETTERS --------------------

    public void setId(Long id) {
        this.id = id;
    }

    public void setFileMetadata(FileMetadata fileMetadata) {
        this.fileMetadata = fileMetadata;
    }

    public void setLicense(License license) {
        this.license = license;
    }

    public void setAllRightsReserved(boolean allRightsReserved) {
        this.allRightsReserved = allRightsReserved;
    }

    public void setRestrictType(RestrictType restrictType) {
        this.restrictType = restrictType;
    }

    public void setRestrictCustomText(String restrictCustomText) {
        this.restrictCustomText = restrictCustomText;
    }


}
