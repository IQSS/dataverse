/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.api.imports;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 *
 * @author ellenk
 */
@Stateless
public class CustomFieldServiceBean {

   
     @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
     
    public CustomFieldMap findByTemplateField(String template, String field) {
        try {
            CustomFieldMap map = (CustomFieldMap) em.createNamedQuery("CustomFieldMap.findByTemplateField").setParameter("template", template).setParameter("field", field).getSingleResult();
            return map;
        } catch (Exception ex) {
            System.out.println("Exception "+ ex + "template: "+template+" field: "+ field);
            return null;
        }

    }

}
