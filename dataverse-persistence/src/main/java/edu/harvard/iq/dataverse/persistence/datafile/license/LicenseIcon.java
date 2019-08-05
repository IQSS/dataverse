package edu.harvard.iq.dataverse.persistence.datafile.license;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.io.Serializable;

/**
 * Entity class representing icon (image) of the license.
 *
 * @author madryk
 */
@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"license_id"})})
public class LicenseIcon implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private byte[] content;

    @Column(nullable = false)
    private String contentType;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "license_id", nullable = false)
    private License license;


    //-------------------- GETTERS --------------------

    /**
     * Returns database id of license icon
     */
    public Long getId() {
        return id;
    }

    /**
     * Returns bytes of image representing this icon
     */
    public byte[] getContent() {
        return content;
    }

    /**
     * Returns content-type of the image file
     * saved in {{@link #getContent()}
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Returns license associated with this icon
     */
    public License getLicense() {
        return license;
    }


    //-------------------- SETTERS --------------------

    public void setId(Long id) {
        this.id = id;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void setLicense(License license) {
        this.license = license;
    }


}
