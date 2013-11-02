/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import javax.ejb.EJB;
import javax.faces.event.ActionEvent;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

/**
 *
 * @author gdurand
 */
@ViewScoped
@Named("DataversePage")
public class DataversePage implements java.io.Serializable{
    @EJB 
    DataverseServiceBean dataverseService;
     
    private Dataverse dataverse = new Dataverse();
    private boolean editMode; 
    private Long ownerId;
            
    public Dataverse getDataverse() {return dataverse;}
    public void setDataverse(Dataverse dataverse) {this.dataverse = dataverse;}

    public boolean isEditMode() {return editMode;}
    public void setEditMode(boolean editMode) {this.editMode = editMode;}

    public Long getOwnerId() {return ownerId;}
    public void setOwnerId(Long ownerId) {this.ownerId = ownerId;}
    

    public void init() {
        if (dataverse.getId() != null) {
            editMode = false;            
            dataverse = dataverseService.find(dataverse.getId());
        } else {
            editMode = true;
            if (ownerId != null) {
                dataverse.setOwner( dataverseService.find( ownerId ) );
            }
        }
    }
    
    public void edit(ActionEvent e) { 
     editMode = true;
    }  
    
    public void save(ActionEvent e) { 
     dataverseService.save(dataverse);
     editMode = false;
    }  
    
     public void cancel(ActionEvent e) { 
     dataverse = dataverseService.find(dataverse.getId()); // reset dv values
     editMode = false;
    }    
}
