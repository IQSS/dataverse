/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import org.primefaces.context.RequestContext;

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
    public Dataverse getDataverse() {return dataverse;}
    public void setDataverse(Dataverse dataverse) {this.dataverse = dataverse;}
    

    public void init() {
        if (dataverse.getId() != null) {
            dataverse = dataverseService.find(dataverse.getId());
        }
    }
    
     
    public String save() {
     dataverse.setName( dataverse.getName().trim() );   
     dataverseService.save(dataverse);
     return "dataverses.xhtml?faces-redirect=true";
}
    /*
    public void save(ActionListener e) { 
     dataverse.setName( dataverse.getName().trim() );   
     dataverseService.save(dataverse);
    } */   
}
