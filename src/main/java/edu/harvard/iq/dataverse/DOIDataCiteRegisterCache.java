/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;


import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import org.hibernate.validator.constraints.NotBlank;

/**
 *
 * @author luopc
 */
@NamedQueries(
        @NamedQuery( name="DOIDataCiteRegisterCache.findByDoi",
                     query="SELECT d FROM DOIDataCiteRegisterCache d WHERE d.doi=:doi")
)
@Entity
public class DOIDataCiteRegisterCache implements Serializable{

    private static final long serialVersionUID = 8030143094734315681L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Column(unique=true)
    private String doi;
    
    @NotBlank
    private String url;
    
    @NotBlank
    private String status;
    
    @NotBlank
    @Lob
    private String xml;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDoi() {
        return doi;
    }

    public void setDoi(String doi) {
        this.doi = doi;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getXml() {
        return xml;
    }

    public void setXml(String xml) {
        this.xml = xml;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}