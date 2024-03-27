package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.pidproviders.PidProvider;
import edu.harvard.iq.dataverse.pidproviders.PidProviderFactoryBean;
import edu.harvard.iq.dataverse.pidproviders.PidUtil;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import static jakarta.ejb.TransactionAttributeType.REQUIRES_NEW;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.StoredProcedureQuery;

import org.apache.commons.lang3.StringUtils;

/**
 * Your goto bean for everything {@link DvObject}, that's not tied to any
 * concrete subclass.
 *
 * @author michael
 */
@Stateless
@Named
public class DvObjectServiceBean implements java.io.Serializable {

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    
    @EJB
    PidProviderFactoryBean pidProviderFactoryBean;
    
    private static final Logger logger = Logger.getLogger(DvObjectServiceBean.class.getCanonicalName());
    /**
     * @param dvoc The object we check
     * @return {@code true} iff the passed object is the owner of any
     * {@link DvObject}.
     */
    public boolean hasData(DvObjectContainer dvoc) {
        return em.createNamedQuery("DvObject.ownedObjectsById", Long.class)
                .setParameter("id", dvoc.getId())
                .getSingleResult() > 0;
    }

    public DvObject findDvObject(Long id) {
        try {
            return em.createNamedQuery("DvObject.findById", DvObject.class)
                    .setParameter("id", id)
                    .getSingleResult();
        } catch (NoResultException | NonUniqueResultException ex) {
            return null;
        }
    }

    public List<DvObject> findAll() {
        return em.createNamedQuery("DvObject.findAll", DvObject.class).getResultList();
    }
    
    
    public List<DvObject> findByOwnerId(Long ownerId) {
        return em.createNamedQuery("DvObject.findByOwnerId").setParameter("ownerId", ownerId).getResultList();
    }
    
    public List<DvObject> findByAuthenticatedUserId(AuthenticatedUser user) {
        Query query = em.createNamedQuery("DvObject.findByAuthenticatedUserId"); 
        query.setParameter("ownerId", user.getId());
        query.setParameter("releaseUserId", user.getId());
        return query.getResultList();
    }
    
    public boolean checkExists(Long id) {
        Query query = em.createNamedQuery("DvObject.checkExists");
        query.setParameter("id", id);
        Long result =(Long)query.getSingleResult();
        return result > 0;
    }   

    public DvObject findByGlobalId(String globalIdString, DvObject.DType dtype) {
        try {
            GlobalId gid = PidUtil.parseAsGlobalID(globalIdString);
            return findByGlobalId(gid, dtype);
        } catch (IllegalArgumentException iae) {
            logger.fine("Invalid identifier: " + globalIdString);
            return null;
        }

    }
    
    public DvObject findByAltGlobalId(String globalIdString, DvObject.DType dtype) {
        try {
            GlobalId gid = PidUtil.parseAsGlobalID(globalIdString);
            return findByAltGlobalId(gid, dtype);
        } catch (IllegalArgumentException iae) {
            logger.fine("Invalid alternate identifier: " + globalIdString);
            return null;
        }

    }

    public DvObject findByGlobalId(GlobalId globalId, DvObject.DType dtype) {
        Query query = em.createNamedQuery("DvObject.findByGlobalId");
        return runFindByGlobalId(query, globalId, dtype);
    }

    public DvObject findByAltGlobalId(GlobalId globalId, DvObject.DType dtype) {
        Query query = em.createNamedQuery("DvObject.findByAlternativeGlobalId");
        return runFindByGlobalId(query, globalId, dtype);
    }

    public Long findIdByGlobalId(GlobalId globalId, DvObject.DType dtype) {
        Query query = em.createNamedQuery("DvObject.findIdByGlobalId");
        return runFindIdByGlobalId(query, globalId, dtype);
    }

    public Long findIdByAltGlobalId(GlobalId globalId, DvObject.DType dtype) {
        Query query = em.createNamedQuery("DvObject.findIdByAlternativeGlobalId");
        return runFindIdByGlobalId(query, globalId, dtype);
    }

    private DvObject runFindByGlobalId(Query query, GlobalId gid, DvObject.DType dtype) {
        DvObject foundDvObject = null;
        try {
            query.setParameter("identifier", gid.getIdentifier());
            query.setParameter("protocol", gid.getProtocol());
            query.setParameter("authority", gid.getAuthority());
            query.setParameter("dtype", dtype.getDType());
            foundDvObject = (DvObject) query.getSingleResult();
        } catch (NoResultException e) {
            // (set to .info, this can fill the log file with thousands of
            // these messages during a large harvest run)
            logger.fine("no dvObject found: " + gid.asString());
            // DO nothing, just return null.
            return null;
        } catch (Exception ex) {
            logger.info("Exception caught in findByGlobalId: " + ex.getLocalizedMessage());
            return null;
        }
        return foundDvObject;
    }

    private Long runFindIdByGlobalId(Query query, GlobalId gid, DvObject.DType dtype) {
        Long foundDvObject = null;
        try {
            query.setParameter("identifier", gid.getIdentifier());
            query.setParameter("protocol", gid.getProtocol());
            query.setParameter("authority", gid.getAuthority());
            query.setParameter("dtype", dtype.getDType());
            foundDvObject = (Long) query.getSingleResult();
        } catch (NoResultException e) {
            // (set to .info, this can fill the log file with thousands of
            // these messages during a large harvest run)
            logger.fine("no dvObject found: " + gid.asString());
            // DO nothing, just return null.
            return null;
        } catch (Exception ex) {
            logger.info("Exception caught in findByGlobalId: " + ex.getLocalizedMessage());
            return null;
        }
        return foundDvObject;
    }
    
    public DvObject findByGlobalId(GlobalId globalId) {
        try {
            return (DvObject) em.createNamedQuery("DvObject.findByProtocolIdentifierAuthority")
                    .setParameter("identifier", globalId.getIdentifier())
                    .setParameter("authority", globalId.getAuthority()).setParameter("protocol", globalId.getProtocol())
                    .getSingleResult();
        } catch (NoResultException nre) {
            return null;
        }
    }
    
    public boolean isGlobalIdLocallyUnique(GlobalId globalId) {
        return em.createNamedQuery("DvObject.findByProtocolIdentifierAuthority")
            .setParameter("identifier", globalId.getIdentifier())
            .setParameter("authority", globalId.getAuthority())
            .setParameter("protocol", globalId.getProtocol())
            .getResultList().isEmpty();
    }

    public DvObject updateContentIndexTime(DvObject dvObject) {
        /**
         * @todo to avoid a possible OptimisticLockException, should we merge
         * dvObject before we try to setIndexTime? See
         * https://github.com/IQSS/dataverse/commit/6ad0ebb272c8cb46368cb76784b55dbf33eea947
         */
        DvObject dvObjectToModify = findDvObject(dvObject.getId());
        dvObjectToModify.setIndexTime(new Timestamp(new Date().getTime()));
        DvObject savedDvObject = em.merge(dvObjectToModify);
        return savedDvObject;
    }

    /**
     * @param dvObject
     * @return 
     * @todo DRY! Perhaps we should merge this with the older
     * updateContentIndexTime method.
     */
    public DvObject updatePermissionIndexTime(DvObject dvObject) {
        /**
         * @todo to avoid a possible OptimisticLockException, should we merge
         * dvObject before we try to set this timestamp? See
         * https://github.com/IQSS/dataverse/commit/6ad0ebb272c8cb46368cb76784b55dbf33eea947
         */
        Long dvObjectId = dvObject.getId();
        DvObject dvObjectToModify = findDvObject(dvObjectId);
        if (dvObjectToModify == null) {
            logger.log(Level.FINE, "Unable to update permission index time on DvObject with id of {0}", dvObjectId);
            return dvObject;
        }
        dvObjectToModify.setPermissionIndexTime(new Timestamp(new Date().getTime()));
        DvObject savedDvObject = em.merge(dvObjectToModify);
        logger.log(Level.FINE, "Updated permission index time for DvObject id {0}", dvObjectId);
        return savedDvObject;
    }

    @TransactionAttribute(REQUIRES_NEW)
    public int clearAllIndexTimes() {
        Query clearIndexTimes = em.createQuery("UPDATE DvObject o SET o.indexTime = NULL, o.permissionIndexTime = NULL");
        int numRowsUpdated = clearIndexTimes.executeUpdate();
        return numRowsUpdated;
    }

    public int clearIndexTimes(long dvObjectId) {
        Query clearIndexTimes = em.createQuery("UPDATE DvObject o SET o.indexTime = NULL, o.permissionIndexTime = NULL WHERE o.id =:dvObjectId");
        clearIndexTimes.setParameter("dvObjectId", dvObjectId);
        int numRowsUpdated = clearIndexTimes.executeUpdate();
        return numRowsUpdated;
    }
    
    private String getDvObjectIdListClause(List<Long> dvObjectIdList){
        if (dvObjectIdList == null){
            return null;
        }
        List<String> outputList = new ArrayList<>();
        
        for(Long id : dvObjectIdList){
            if (id != null){
                outputList.add(id.toString());
            }
        }
        if (outputList.isEmpty()){
            return null;
        }        
        return " (" + StringUtils.join(outputList, ",") + ")";        
    }
    
    public List<Object[]> getDvObjectInfoForMyData(List<Long> dvObjectIdList){
        //msgt("getAssigneeAndRoleIdListFor");

        String dvObjectClause = getDvObjectIdListClause(dvObjectIdList);
        if (dvObjectClause==null){
            return null;
        }
        
        String qstr = "SELECT dv.id, dv.dtype, dv.owner_id"; // dv.modificationtime,
        qstr += " FROM dvobject dv";
        qstr += " WHERE  dv.id IN " + dvObjectClause;
        qstr += ";";

        return em.createNativeQuery(qstr).getResultList();
        
    }
    
    /**
     * Used for retrieving DvObject based on a list of parent Ids
     *  MyData use case: The Dataverse has file permissions and we want to know 
     *  the Datasets under that Dataverse (and subsequently query files by
     *  their parent id--but in solr)
     * 
     * @param dvObjectParentIdList
     * @return 
     */
    public List<Object[]> getDvObjectInfoByParentIdForMyData(List<Long> dvObjectParentIdList){
        //msgt("getAssigneeAndRoleIdListFor");

        String dvObjectClause = getDvObjectIdListClause(dvObjectParentIdList);
        if (dvObjectClause==null){
            return null;
        }
        
        String qstr = "SELECT dv.id, dv.dtype, dv.owner_id"; // dv.modificationtime,
        qstr += " FROM dvobject dv";
        qstr += " WHERE  dv.owner_id IN " + dvObjectClause;
        qstr += ";";

        return em.createNativeQuery(qstr).getResultList();
        
    }
    
    /**
     * Used to exclude Harvested Data from the Mydata page
     * 
     * @return 
     */
    public List<Long> getAllHarvestedDataverseIds(){
        
        String qstr = "SELECT h.dataverse_id FROM harvestingclient h;";

        return em.createNativeQuery(qstr).getResultList();
        
    }
    
    /**
     * Used to calculate the dvObject tree paths for the search results on the
     * dataverse page. (In order to determine if "linked" or not).
     * *done in recursive 1 query!*
     * 
     * @param objectIds
     * @return 
     */
    public Map<Long, String> getObjectPathsByIds(Set<Long> objectIds){
        if (objectIds == null || objectIds.size() < 1) {
            return null;
        }
        
        String datasetIdStr = StringUtils.join(objectIds, ", ");
        
        String qstr = "WITH RECURSIVE path_elements AS ((" +
            " SELECT id, owner_id FROM dvobject WHERE id in (" + datasetIdStr + "))" +
            " UNION\n" +
            " SELECT o.id, o.owner_id FROM path_elements p, dvobject o WHERE o.id = p.owner_id) " +
            "SELECT id, owner_id FROM path_elements WHERE owner_id IS NOT NULL;"; // ORDER by id ASC;";
        
        List<Object[]> searchResults;
        
        try {
            searchResults = em.createNativeQuery(qstr).getResultList();
        } catch (Exception ex) {
            searchResults = null;
        }
        
        if (searchResults == null || searchResults.size() < 1) {
            return null;
        }
        
        Map<Long, Long> treeMap = new HashMap<>();
        
        for (Object[] result : searchResults) {
            Long objectId;
            Long ownerId;
            if (result[0] != null) {
                try {
                    objectId = ((Integer) result[0]).longValue();
                } catch (Exception ex) {
                    logger.warning("OBJECT PATH: could not cast result[0] (dvobject id) to Integer!");
                    objectId = null;
                }
                if (objectId == null) {
                    continue;
                }
                
                ownerId = (Long)result[1];
                logger.fine("OBJECT PATH: id: "+objectId+", owner: "+ownerId);
                if (ownerId != null && (ownerId != 1L)) {
                    treeMap.put(objectId, ownerId);
                }
            }
        }
        
        Map<Long, String> ret = new HashMap<>();
        
        for (Long objectId : objectIds) {
            String treePath = "/" + objectId;
            Long treePosition = treeMap.get(objectId);
            
            while (treePosition != null) {
                treePath = "/" + treePosition + treePath;
                treePosition = treeMap.get(treePosition);
            }
            
            logger.fine("OBJECT PATH: returning "+treePath+" for "+objectId);
            ret.put(objectId, treePath);
        }
        return ret;        
    }
    
    public String generateNewIdentifierByStoredProcedure() {
        StoredProcedureQuery query = this.em.createNamedStoredProcedureQuery("Dataset.generateIdentifierFromStoredProcedure");
        query.execute();
        return (String) query.getOutputParameterValue(1);
    }
    
    /** @deprecated Backward-compatibility method to get the effective pid generator for a DvObjectContainer.
     * If the dvObjectContainer method fails, this method will check for the old global default settings.
     * If/when those are no longer supported, this method can be removed and replaced with calls directly 
     * to dvObjectContainer.getEffectivePidGenerator();
     * 
     */
    @Deprecated(forRemoval = true, since = "2024-02-09")
    public PidProvider getEffectivePidGenerator(DvObjectContainer dvObjectContainer) {
        PidProvider pidGenerator = dvObjectContainer.getEffectivePidGenerator();
        if (pidGenerator == null) {
            pidGenerator = pidProviderFactoryBean.getDefaultPidGenerator();
        }
        return pidGenerator;
    }
    
}
