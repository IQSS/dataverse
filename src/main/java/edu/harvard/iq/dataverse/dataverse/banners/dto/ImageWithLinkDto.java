package edu.harvard.iq.dataverse.dataverse.banners.dto;

import org.primefaces.model.StreamedContent;

public class ImageWithLinkDto {

    private StreamedContent image;
    private String link;

    public ImageWithLinkDto(StreamedContent image, String link) {
        this.image = image;
        this.link = link;
    }

    public StreamedContent getImage() {
        return image;
    }

    public void setImage(StreamedContent image) {
        this.image = image;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }
}
