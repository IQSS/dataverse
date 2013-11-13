/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.validation.constraints.Size;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;
//import org.springframework.format.annotation.DateTimeFormat;

/**
 *
 * @author skraffmiller
 */
@Entity
public class Dataset implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Please enter a title for your dataset.")
    private String title;

    @NotBlank(message = "Please enter an author for your dataset.")
    private String author;

    @NotBlank(message = "Please enter a distribution date for your dataset.")
   // @DateTimeFormat(pattern="YYYY/MM/DD")
    private String citationDate;

    @NotBlank(message = "Please enter a distributor for your dataset.")
    private String distributor;

    // #VALIDATION: page defines maxlength in input:textarea component
    @Size(max = 1000, message = "Description must be at most 1000 characters.")
    private String description;
    
    // sample metadata fields
    private String keyword;
    private String topicClassification;
    @URL
    private String topicClassificationUrl;
    private String geographicCoverage;
    
    @ManyToOne
    @JoinColumn(nullable=false)     
    private Dataverse owner;
    
    @OneToMany (mappedBy = "dataset", cascade = CascadeType.MERGE)
    private List<DataFile> files = new ArrayList();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getCitationDate() {
        return citationDate;
    }

    public void setCitationDate(String citationDate) {
        this.citationDate = citationDate;
    }

    public String getDistributor() {
        return distributor;
    }

    public void setDistributor(String distributor) {
        this.distributor = distributor;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getTopicClassification() {
        return topicClassification;
    }

    public void setTopicClassification(String topicClassification) {
        this.topicClassification = topicClassification;
    }

    public String getTopicClassificationUrl() {
        return topicClassificationUrl;
    }

    public void setTopicClassificationUrl(String topicClassificationUrl) {
        this.topicClassificationUrl = topicClassificationUrl;
    }

    public String getGeographicCoverage() {
        return geographicCoverage;
    }

    public void setGeographicCoverage(String geographicCoverage) {
        this.geographicCoverage = geographicCoverage;
    }

    public Dataverse getOwner() {
        return owner;
    }

    public void setOwner(Dataverse owner) {
        this.owner = owner;
    }

    public List<DataFile> getFiles() {
        return files;
    }

    public void setFiles(List<DataFile> files) {
        this.files = files;
    }

    
    public String getCitation() {
        return author + ", \"" + title + "\", " + citationDate + ", " + distributor + ", http://dx.doi.org/10.1234/dataverse/123456 V1 [Version]";
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Dataset)) {
            return false;
        }
        Dataset other = (Dataset) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "edu.harvard.iq.dataverse.Dataset[ id=" + id + " ]";
    }

}
