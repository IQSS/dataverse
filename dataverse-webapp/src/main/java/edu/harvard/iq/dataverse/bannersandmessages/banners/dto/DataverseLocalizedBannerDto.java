package edu.harvard.iq.dataverse.bannersandmessages.banners.dto;

import java.util.Objects;

public class DataverseLocalizedBannerDto {

    private Long id;

    private String locale;

    private String imageLink;
    
    private String contentType;
    private String filename;
    private byte[] content;

    // -------------------- CONSTRUCTORS --------------------
    
    public DataverseLocalizedBannerDto() {
    }

    public DataverseLocalizedBannerDto(String locale) {
        this.locale = locale;
    }

    public DataverseLocalizedBannerDto(Long id, String locale, String imageLink) {
        this.id = id;
        this.locale = locale;
        this.imageLink = imageLink;
    }

    // -------------------- GETTERS --------------------

    public Long getId() {
        return id;
    }

    public String getLocale() {
        return locale;
    }
    
    public String getImageLink() {
        return imageLink;
    }
    
    public String getContentType() {
        return contentType;
    }

    public String getFilename() {
        return filename;
    }

    public byte[] getContent() {
        return content;
    }

    // -------------------- SETTERS --------------------
    
    public void setId(Long id) {
        this.id = id;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public void setImageLink(String imageLink) {
        this.imageLink = imageLink;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    // -------------------- hashCode & equals --------------------
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DataverseLocalizedBannerDto that = (DataverseLocalizedBannerDto) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(locale, that.locale) &&
                Objects.equals(imageLink, that.imageLink);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, locale, imageLink);
    }
}
