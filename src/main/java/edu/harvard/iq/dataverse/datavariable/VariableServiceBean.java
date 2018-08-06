/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.datavariable;

import edu.harvard.iq.dataverse.EntityManagerBean;
import java.util.List;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TypedQuery;

/**
 *
 * @author Leonid Andreev
 * 
 * Basic skeleton of the new DataVariable service for DVN 4.0
 */

@Stateless
@Named
public class VariableServiceBean {
    public static final String[] summaryStatisticTypes = {"mean", "medn", "mode", "vald", "invd", "min", "max", "stdev"};
    
    //private static final Logger logger = Logger.getLogger(VariableServiceBean.class.getCanonicalName());
    
    @Inject
    EntityManagerBean emBean;

    public DataVariable save(DataVariable variable) {
        DataVariable savedVariable = emBean.getMasterEM().merge(variable);
        return savedVariable;
    }

    public DataVariable find(Object pk) {
        return emBean.getEntityManager().find(DataVariable.class, pk);
    }

    public List<DataVariable> findByDataFileId(Long fileId) {
         TypedQuery<DataVariable> query = emBean.getMasterEM().createQuery("select object(o) from DataVariable as o where o.dataTable.dataFile.id =:fileId order by o.fileOrder", DataVariable.class);
         query.setParameter("fileId", fileId);
         return query.getResultList();
    }
    
    public List<DataVariable> findByDataTableId(Long dtId) {
         TypedQuery<DataVariable> query = emBean.getMasterEM().createQuery("select object(o) from DataVariable as o where o.dataTable.id =:dtId order by o.fileOrder", DataVariable.class);
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
     *
    public VariableFormatType findVariableFormatTypeByName(String name) {
        Query query = emBean.getMasterEM().createQuery("SELECT t from VariableFormatType t where t.name = :name");
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
            type=(VariableIntervalType)emBean.getMasterEM().createQuery(query).getSingleResult();
        } catch (javax.persistence.NoResultException e) {
            // DO nothing, just return null.
        }
        return type;
    }
    
    public SummaryStatisticType findSummaryStatisticTypeByName(String name) {
        String query = "SELECT t from SummaryStatisticType t where t.name = '" + name + "'";
        SummaryStatisticType type = null;
        try {
            type = (SummaryStatisticType) emBean.getMasterEM().createQuery(query).getSingleResult();
        } catch (javax.persistence.NoResultException e) {
            // DO nothing, just return null.
        }
        return type;
    }

    public List<SummaryStatisticType> findAllSummaryStatisticType() {
        String query = "SELECT t from SummaryStatisticType t ";
        return emBean.getMasterEM().createQuery(query).getResultList();

    }

   
    public SummaryStatisticType findSummaryStatisticTypeByName(List<SummaryStatisticType> typeList, String name) {
        SummaryStatisticType type = null;
        for (Iterator<SummaryStatisticType> it = typeList.iterator(); it.hasNext();) {
            SummaryStatisticType elem = it.next();
            if (elem.getName().equals(name)) {
                type = elem;
                break;
            }
        }
        return type;
    }
    */
    
}
