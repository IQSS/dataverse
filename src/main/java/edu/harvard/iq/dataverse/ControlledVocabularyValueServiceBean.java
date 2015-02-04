/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.util.ArrayList;
import java.util.List;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

/**
 *
 * @author skraffmiller
 */
@Stateless
@Named
public class ControlledVocabularyValueServiceBean implements java.io.Serializable {

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    
    public List<ControlledVocabularyValue> findByDatasetFieldTypeId(Long dsftId) {

        String queryString = "select o from ControlledVocabularyValue as o where o.datasetFieldType.id = " + dsftId + " ";
        Query query = em.createQuery(queryString);
        return query.getResultList();
        
    }
    
}
