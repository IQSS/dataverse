/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.api.imports;

import edu.harvard.iq.dataverse.EntityManagerBean;
import javax.ejb.Stateless;
import javax.inject.Inject;

/**
 *
 * @author ellenk
 */
@Stateless
public class CustomFieldServiceBean {

   
    @Inject
    EntityManagerBean emBean;
     
    public CustomFieldMap findByTemplateField(String template, String field) {
        try {
            CustomFieldMap map = (CustomFieldMap) emBean.getMasterEM().createNamedQuery("CustomFieldMap.findByTemplateField").setParameter("template", template).setParameter("field", field).getSingleResult();
            return map;
        } catch (Exception ex) {
            System.out.println("Exception "+ ex + "template: "+template+" field: "+ field);
            return null;
        }

    }

}
