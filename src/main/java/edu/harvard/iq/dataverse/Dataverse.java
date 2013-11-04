/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotBlank;

/**
 *
 * @author gdurand
 */
@Entity
public class Dataverse implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    //@GeneratedValue(strategy = GenerationType.AUTO)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank (message = "The World Champion Red Sox DEMAND a name!")  
    private String name;
    
    @NotBlank (message = "Please enter an alias.") 
    @Size (max=16,message = "Alias must be at most 16 characters.")
    @Pattern(regexp = "[a-zA-Z0-9\\_\\-]*", message="Found an illegal character(s). Valid characters are a-Z, 0-9, '_', and '-'.")
    private String alias;

    @Size (max=1000, message = "Description must be at most 1000 characters.") 
    private String description;
    
    @NotBlank (message = "Please enter a valid email address.") 
    @Email(message = "Please enter a valid email address.") 
    private String contactEmail;

    private String affiliation;
    
    @ManyToOne  
    private Dataverse owner;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }    
    
    public String getName() {return name;}
    public void setName(String name) {this.name = name;}

    public String getAlias() {return alias;}
    public void setAlias(String alias) {this.alias = alias;}

    public String getDescription() {return description;}
    public void setDescription(String description) {this.description = description;}
    
    public String getContactEmail() {return contactEmail;}
    public void setContactEmail(String contactEmail) {this.contactEmail = contactEmail;}

    public String getAffiliation() {return affiliation;}
    public void setAffiliation(String affiliation) {this.affiliation = affiliation;}

    public Dataverse getOwner() {return owner;}
    public void setOwner(Dataverse owner) {this.owner = owner;}
    
    
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Dataverse)) {
            return false;
        }
        Dataverse other = (Dataverse) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "edu.harvard.iq.dataverse.Dataverse[ id=" + id + " ]";
    }
    
}
