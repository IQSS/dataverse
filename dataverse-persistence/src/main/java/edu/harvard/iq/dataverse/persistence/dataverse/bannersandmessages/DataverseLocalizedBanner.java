package edu.harvard.iq.dataverse.persistence.dataverse.bannersandmessages;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import java.util.Optional;

@Entity
public class DataverseLocalizedBanner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String locale;

    @Column(nullable = false)
    @Lob
    @Basic(fetch = FetchType.LAZY)
    private byte[] image;

    private String contentType;

    private String imageName;

    private String imageLink;

    @ManyToOne(fetch = FetchType.LAZY)
    private DataverseBanner dataverseBanner;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public byte[] getImage() {
        return image;
    }

    public void setImage(byte[] image) {
        this.image = image;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public Optional<String> getImageLink() {
        return Optional.ofNullable(imageLink);
    }

    public void setImageLink(String imageLink) {
        this.imageLink = imageLink;
    }

    public DataverseBanner getDataverseBanner() {
        return dataverseBanner;
    }

    public void setDataverseBanner(DataverseBanner dataverseBanner) {
        this.dataverseBanner = dataverseBanner;
    }
}
