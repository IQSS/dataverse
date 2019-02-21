package edu.harvard.iq.dataverse.bannersandmessages.banners.dto;

import org.primefaces.model.StreamedContent;
import org.primefaces.model.UploadedFile;

import java.util.Objects;

public class DataverseLocalizedBannerDto {

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

    private Long id;

    private String locale;

    private StreamedContent miniDisplayImage;

    private StreamedContent displayedImage;

    private String imageLink;

    private UploadedFile file;

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

    public StreamedContent getMiniDisplayImage() {
        return miniDisplayImage;
    }

    public void setMiniDisplayImage(StreamedContent miniDisplayImage) {
        this.miniDisplayImage = miniDisplayImage;
    }

    public StreamedContent getDisplayedImage() {
        return displayedImage;
    }

    public void setDisplayedImage(StreamedContent displayedImage) {
        this.displayedImage = displayedImage;
    }

    public String getImageLink() {
        return imageLink;
    }

    public void setImageLink(String imageLink) {
        this.imageLink = imageLink;
    }

    /**
     * Prime Face class which is made when file is uploaded.
     *
     * @return UploadedFile
     */
    public UploadedFile getFile() {
        return file;
    }

    public void setFile(UploadedFile file) {
        this.file = file;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataverseLocalizedBannerDto that = (DataverseLocalizedBannerDto) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(locale, that.locale) &&
                Objects.equals(miniDisplayImage, that.miniDisplayImage) &&
                Objects.equals(displayedImage, that.displayedImage) &&
                Objects.equals(imageLink, that.imageLink) &&
                Objects.equals(file, that.file);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, locale, miniDisplayImage, displayedImage, imageLink, file);
    }
}
