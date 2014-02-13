/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author skraffmiller
 */
@Stateless
@Named
public class TemplateServiceBean {
    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
        
    public Template find(Long pk) {
        return (Template) em.find(Template.class, pk);
    }  
    
}
