/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.io.Serializable;
import java.util.Objects;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import edu.harvard.iq.dataverse.validation.ValidateEmail;
import org.hibernate.validator.constraints.NotBlank;

/**
 *
 * @author gdurand
 */
@Entity
@Table(indexes = {@Index(columnList="dataverse_id")
		, @Index(columnList="contactemail")
		, @Index(columnList="displayorder")})
public class DataverseContact implements Serializable {

    private static final long serialVersionUID = 1L;

    public DataverseContact() {
    }      
    
    public DataverseContact(Dataverse dv) {
        this.dataverse = dv; 
    }
    
    public DataverseContact(Dataverse dv, String contactEmail) {
        this.dataverse = dv;        
        this.contactEmail = contactEmail;
    }      
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @ManyToOne
    @JoinColumn(name = "dataverse_id")
    private Dataverse dataverse;

    @NotBlank(message = "{user.invalidEmail}")
    @ValidateEmail(message = "{user.invalidEmail}")
    @Column( nullable = false )
    private String contactEmail;
    private int displayOrder;

    public Dataverse getDataverse() {
        return dataverse;
    }

    public void setDataverse(Dataverse dataverse) {
        this.dataverse = dataverse;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (this.id != null ? this.id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof DataverseContact)) {
            return false;
        }
        DataverseContact other = (DataverseContact) object;
        return !(!Objects.equals(this.id, other.id) && (this.id == null || !this.id.equals(other.id)));
    }

    @Override
    public String toString() {
        return "edu.harvard.iq.dataverse.DataverseContact[ id=" + id + " ]";
    }

}
