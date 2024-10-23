/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

/**
 *
 * @author skraffmiller
 */

public class DatasetRelPublication {

    /**
     * The "text" is the citation of the related publication.
     */
    private String text;
    private String idType;
    private String idNumber;
    private String url;
    private String title;
    private String description;
    private boolean replicationData;
    private int displayOrder;
    private String relationType;

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public String getIdNumber() {
        return idNumber;
    }

    public void setIdNumber(String idNumber) {
        this.idNumber = idNumber;
    }

    public String getIdType() {
        return idType;
    }

    public void setIdType(String idType) {
        this.idType = idType;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setRelationType(String type) {
        relationType = type;

    }

    public String getRelationType() {
        return relationType;
    }

    public boolean isEmpty() {
        return ((text == null || text.trim().equals(""))
                && (!replicationData)
                && (idType == null || idType.trim().equals(""))
                && (idNumber == null || idNumber.trim().equals(""))
                && (url == null || url.trim().equals("")));
    }

}
