/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.datavariable;

import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

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
    
    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    public DataVariable save(DataVariable variable) {
        DataVariable savedVariable = em.merge(variable);
        return savedVariable;
    }

    public DataVariable find(Object pk) {
        return em.find(DataVariable.class, pk);
    }    
    
    public List<DataVariable> findByDataFileId(Long fileId) {
         TypedQuery<DataVariable> query = em.createQuery("select object(o) from DataVariable as o where o.dataTable.dataFile.id =:fileId order by o.fileOrder", DataVariable.class);
         query.setParameter("fileId", fileId);
         return query.getResultList();
    }
    
    public List<DataVariable> findByDataTableId(Long dtId) {
         TypedQuery<DataVariable> query = em.createQuery("select object(o) from DataVariable as o where o.dataTable.id =:dtId order by o.fileOrder", DataVariable.class);
         query.setParameter("dtId", dtId);
         return query.getResultList();
    }

    public List<VariableMetadata> findByDataVarIdAndFileMetaId(Long datVarId, Long metaId) {
        TypedQuery<VariableMetadata> query = em.createQuery("SELECT object(o) FROM VariableMetadata as o where o.dataVariable.id =:dvId and o.fileMetadata.id =:fmId", VariableMetadata.class);

        query.setParameter("dvId", datVarId);
        query.setParameter("fmId", metaId);
        return query.getResultList();

    }

    public List<VariableMetadata> findVarMetByFileMetaId(Long metaId) {
        TypedQuery<VariableMetadata> query = em.createQuery("SELECT object(o) FROM VariableMetadata as o where o.fileMetadata.id =:fmId", VariableMetadata.class);

        query.setParameter("fmId", metaId);
        return query.getResultList();

    }
    public List<VariableCategory> findCategory(Long varId, String catValue) {
        TypedQuery<VariableCategory> query = em.createQuery("SELECT object(o) FROM VariableCategory as o where o.dataVariable.id =:varId and o.value =:catValue", VariableCategory.class);

        query.setParameter("varId", varId);
        query.setParameter("catValue", catValue);
        return query.getResultList();
    }

    public List<CategoryMetadata> findCategoryMetadata(Long catId, Long varMetId) {
        TypedQuery<CategoryMetadata> query = em.createQuery("SELECT object(o) FROM CategoryMetadata as o where o.variableMetadata.id =:varMetId and o.category.id =:catId", CategoryMetadata.class);

        query.setParameter("catId", catId);
        query.setParameter("varMetId", varMetId);
        return query.getResultList();
    }

    public List<VarGroup> findAllGroupsByFileMetadata(Long fileMetaId) {
        TypedQuery<VarGroup> query = em.createQuery("SELECT object(o) FROM VarGroup as o where o.fileMetadata.id =:fileMetaId", VarGroup.class);
        query.setParameter("fileMetaId", fileMetaId);

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
        Query query = em.createQuery("SELECT t from VariableFormatType t where t.name = :name");
        query.setParameter("name", name);
        VariableFormatType type = null;
        try {
            type = (VariableFormatType)query.getSingleResult();
        } catch (jakarta.persistence.NoResultException e) {
            // DO nothing, just return null.
        }
        return type;
    }
    
    public VariableIntervalType findVariableIntervalTypeByName(String name) {
        String query="SELECT t from VariableIntervalType t where t.name = '"+name+"'";
        VariableIntervalType type = null;
        try {
            type=(VariableIntervalType)em.createQuery(query).getSingleResult();
        } catch (jakarta.persistence.NoResultException e) {
            // DO nothing, just return null.
        }
        return type;
    }
    
    public SummaryStatisticType findSummaryStatisticTypeByName(String name) {
        String query = "SELECT t from SummaryStatisticType t where t.name = '" + name + "'";
        SummaryStatisticType type = null;
        try {
            type = (SummaryStatisticType) em.createQuery(query).getSingleResult();
        } catch (jakarta.persistence.NoResultException e) {
            // DO nothing, just return null.
        }
        return type;
    }

    public List<SummaryStatisticType> findAllSummaryStatisticType() {
        String query = "SELECT t from SummaryStatisticType t ";
        return em.createQuery(query).getResultList();

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
