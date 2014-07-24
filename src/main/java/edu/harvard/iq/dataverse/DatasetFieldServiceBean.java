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
import javax.persistence.NoResultException;
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
    
    private static final String NAME_QUERY = "SELECT dsfType from DatasetFieldType dsfType where dsfType.name= :fieldName";
 
    public List<DatasetFieldType> findAllAdvancedSearchFieldTypes() {
        return em.createQuery("select object(o) from DatasetFieldType as o where o.advancedSearchFieldType = true and o.title != '' order by o.id").getResultList();
    }
    
    public List<DatasetFieldType> findAllFacetableFieldTypes() {
        return em.createQuery("select object(o) from DatasetFieldType as o where o.facetable = true and o.title != '' order by o.id").getResultList();
    } 

    public List<DatasetFieldType> findAllRequiredFields() {
        return em.createQuery("select object(o) from DatasetFieldType as o where o.required = true order by o.id").getResultList();
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
    
    /**
     * Gets the dataset field type, or returns {@code null}. Does not throw exceptions.
     * @param name the name do the field type
     * @return the field type, or {@code null}
     * @see #findByName(java.lang.String) 
     */
    public DatasetFieldType findByNameOpt( String name ) {
        try {
            return em.createNamedQuery("DatasetFieldType.findByName", DatasetFieldType.class)
                    .setParameter("name", name)
                    .getSingleResult();
        } catch ( NoResultException nre ) {
            return null;
        }
    }
    
    /* 
     * Similar method for looking up foreign metadata field mappings, for metadata
     * imports. for these the uniquness of names isn't guaranteed (i.e., there 
     * can be a field "author" in many different formats that we want to support), 
     * so these have to be looked up by both the field name and the name of the 
     * foreign format.
     */
    public ForeignMetadataFieldMapping findFieldMapping(String formatName, String pathName) {
        try {
            return em.createNamedQuery("ForeignMetadataFieldMapping.findByPath", ForeignMetadataFieldMapping.class)
                    .setParameter("formatName", formatName)
                    .setParameter("xPath", pathName)
                    .getSingleResult();
        } catch ( NoResultException nre ) {
            return null;
        }
        
        // TODO: 
        // cache looked up results.
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
