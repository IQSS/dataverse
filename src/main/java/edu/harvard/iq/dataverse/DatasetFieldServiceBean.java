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
    private static final String FILEMETA_NAME_QUERY = "SELECT fmf from FileMetadataField fmf where fmf.name= :fieldName ";
    private static final String FILEMETA_NAME_FORMAT_QUERY = "SELECT fmf from FileMetadataField fmf where fmf.name= :fieldName and fmf.fileFormatName= :fileFormatName ";
 
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
