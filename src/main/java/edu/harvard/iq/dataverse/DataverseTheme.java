/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import java.io.Serializable;
import java.util.Objects;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 *
 * @author ellenk
 */

@Entity
@Table(indexes = {@Index(columnList="dataverse_id")})
public class DataverseTheme implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Enumerated(EnumType.STRING)
    private ImageFormat logoFormat;

    public enum Alignment {
        LEFT, CENTER, RIGHT
    }
    @Enumerated(EnumType.STRING)
    private Alignment logoAlignment;
    private String logoBackgroundColor;
    private String logo;
    private Alignment logoFooterAlignment;
    private String logoFooterBackgroundColor;
    private String logoFooter;
    private String tagline;
    private String linkUrl;
    private String linkColor;
    private String textColor;
    private String backgroundColor;
   public enum ImageFormat {

        SQUARE, RECTANGLE
    }

    public ImageFormat getLogoFormat() {
        return logoFormat;
    }

    public void setLogoFormat(ImageFormat logoFormat) {
        this.logoFormat = logoFormat;
    }

    public Alignment getLogoAlignment() {
        return logoAlignment;
    }

    public void setLogoAlignment(Alignment logoAlignment) {
        this.logoAlignment = logoAlignment;
    }

    public String getLogoBackgroundColor() {
        return logoBackgroundColor;
    }

    public void setLogoBackgroundColor(String logoBackgroundColor) {
        this.logoBackgroundColor = logoBackgroundColor;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public Alignment getLogoFooterAlignment() {
        return logoFooterAlignment;
    }

    public void setLogoFooterAlignment(Alignment logoFooterAlignment) {
        this.logoFooterAlignment = logoFooterAlignment;
    }

    public String getLogoFooterBackgroundColor() {
        return logoFooterBackgroundColor;
    }

    public void setLogoFooterBackgroundColor(String logoFooterBackgroundColor) {
        this.logoFooterBackgroundColor = logoFooterBackgroundColor;
    }

    public String getLogoFooter() {
        return logoFooter;
    }

    public void setLogoFooter(String logoFooter) {
        this.logoFooter = logoFooter;
    }

    public String getTagline() {
        return tagline;
    }

    public void setTagline(String tagline) {
        this.tagline = tagline;
    }

    public String getLinkUrl() {
        return linkUrl;
    }

    public void setLinkUrl(String linkUrl) {
        this.linkUrl = linkUrl;
    }

    public String getLinkColor() {
        return linkColor;
    }

    public void setLinkColor(String linkColor) {
        this.linkColor = linkColor;
    }

    public String getTextColor() {
        return textColor;
    }

    public void setTextColor(String textColor) {
        this.textColor = textColor;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }
    @OneToOne
    @JoinColumn(name="dataverse_id")
    private Dataverse dataverse;

    public Dataverse getDataverse() {
        return dataverse;
    }

    public void setDataverse(Dataverse dataverse) {
        this.dataverse = dataverse;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (this.id != null ? this.id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof DataverseTheme)) {
            return false;
        }
        DataverseTheme other = (DataverseTheme) object;
        return !(!Objects.equals(this.id, other.id) && (this.id == null || !this.id.equals(other.id)));
    }

    @Override
    public String toString() {
        return "edu.harvard.iq.dataverse.DataverseFacet[ id=" + id + " ]";
    }

}
