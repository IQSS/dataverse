/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import java.util.List;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

/**
 *
 * @author xyang
 */
@Stateless
@Named
public class DatasetFieldServiceBean {

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    
    private static final String NAME_QUERY = "SELECT dsfType from DatasetFieldType dsfType where dsfType.name= :fieldName ";
 
    public List<DatasetFieldType> findAllAdvancedSearchFieldTypes() {
        return em.createQuery("select object(o) from DatasetFieldType as o where o.advancedSearchFieldType = true and o.title != '' order by o.id").getResultList();
    }
    
    public List<DatasetFieldType> findAllFacetableFieldTypes() {
        return em.createQuery("select object(o) from DatasetFieldType as o where o.facetable = true and o.title != '' order by o.id").getResultList();
    } 

    public List<DatasetFieldType> findAllOrderedById() {
        return em.createQuery("select object(o) from DatasetFieldType as o order by o.id").getResultList();
    }

    public List<DatasetFieldType> findAllOrderedByName() {
        return em.createQuery("select object(o) from DatasetFieldType as o order by o.name").getResultList();
    }

    public DatasetFieldType find(Object pk) {
        return (DatasetFieldType) em.find(DatasetFieldType.class, pk);
    } 
    
    public DatasetFieldType findByName(String name) {
        DatasetFieldType dsfType = (DatasetFieldType) em.createQuery(NAME_QUERY).setParameter("fieldName",name).getSingleResult();
        return dsfType;
    }
    
    public ControlledVocabularyValue findControlledVocabularyValue(Object pk) {
        return (ControlledVocabularyValue) em.find(ControlledVocabularyValue.class, pk);
    }        
    
    public DatasetFieldType save(DatasetFieldType dsfType) {
       return em.merge(dsfType);
    }
    
    public MetadataBlock save(MetadataBlock mdb) {
       return em.merge(mdb);
    }   
    
    public ControlledVocabularyValue save(ControlledVocabularyValue cvv) {
       return em.merge(cvv);
    }       
    
}
