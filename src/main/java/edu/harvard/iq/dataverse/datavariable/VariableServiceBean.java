/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.datavariable;

import java.util.List;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

/**
 *
 * @author Leonid Andreev
 * 
 * Basic skeleton of the new DataVariable service for DVN 4.0
 */

@Stateless
@Named
public class VariableServiceBean {
    
    private static final Logger logger = Logger.getLogger(VariableServiceBean.class.getCanonicalName());
    
    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    public DataVariable save(DataVariable variable) {
        DataVariable savedVariable = em.merge(variable);
        return savedVariable;
    }

    public DataVariable find(Object pk) {
        return (DataVariable) em.find(DataVariable.class, pk);
    }    
    
    public List<DataVariable> findByDataFileId(Long fileId) {
         Query query = em.createQuery("select object(o) from DataVariable as o where o.dataTable.dataFile.id =:fileId order by o.fileOrder");
         query.setParameter("fileId", fileId);
         return query.getResultList();
    }
    
    public List<DataVariable> findByDataTableId(Long dtId) {
         Query query = em.createQuery("select object(o) from DataVariable as o where o.dataTable.id =:dtId order by o.fileOrder");
         query.setParameter("dtId", dtId);
         return query.getResultList();
    }
    
    /* 
     * This is awful!
     * TODO: stop keeping format types in the database!
     * Re-work VariableFormatType to just define constants for "numeric" and "character";
     * better yet, re-work the entire scheme of how variable types are stored and 
     * defined.
     * -- L.A. 4.0
     */
    public VariableFormatType findVariableFormatTypeByName(String name) {
        Query query = em.createQuery("SELECT t from VariableFormatType t where t.name = :name");
        query.setParameter("name", name);
        VariableFormatType type = null;
        try {
            type = (VariableFormatType)query.getSingleResult();
        } catch (javax.persistence.NoResultException e) {
            // DO nothing, just return null.
        }
        return type;
    }
    
    public VariableIntervalType findVariableIntervalTypeByName(String name) {
        String query="SELECT t from VariableIntervalType t where t.name = '"+name+"'";
        VariableIntervalType type = null;
        try {
            type=(VariableIntervalType)em.createQuery(query).getSingleResult();
        } catch (javax.persistence.NoResultException e) {
            // DO nothing, just return null.
        }
        return type;
    }
    
    
}
