package edu.harvard.iq.dataverse;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.apache.commons.lang.StringUtils;

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
     * @todo DRY! Perhaps we should merge this with the older
     * updateContentIndexTime method.
     */
    public DvObject updatePermissionIndexTime(DvObject dvObject) {
        /**
         * @todo to avoid a possible OptimisticLockException, should we merge
         * dvObject before we try to set this timestamp? See
         * https://github.com/IQSS/dataverse/commit/6ad0ebb272c8cb46368cb76784b55dbf33eea947
         */
        DvObject dvObjectToModify = findDvObject(dvObject.getId());
        dvObjectToModify.setPermissionIndexTime(new Timestamp(new Date().getTime()));
        DvObject savedDvObject = em.merge(dvObjectToModify);
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
        
        String qstr = "SELECT h.dataverse_id FROM harvestingdataverseconfig h;";

        return em.createNativeQuery(qstr)
                        .getResultList();
        
    }
}
