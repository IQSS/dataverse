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
         Query query = em.createQuery("select object(o) from DataVariable as o where o.datatable.datafile.id =:fileId order by o.fileOrder");
         query.setParameter("fileId", fileId);
         return query.getResultList();
    }
    
}
