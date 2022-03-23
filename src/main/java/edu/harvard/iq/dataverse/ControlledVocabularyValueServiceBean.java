/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.util.List;
import jakarta.ejb.Stateless;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

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
        TypedQuery<ControlledVocabularyValue> query = em.createQuery(queryString, ControlledVocabularyValue.class);
        return query.getResultList();
        
    }
    
}
