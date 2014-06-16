/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

/**
 *
 * @author skraffmiller
 */
@Stateless
@Named
public class DatasetServiceBean {

    private static final Logger logger = Logger.getLogger(DatasetServiceBean.class.getCanonicalName());
    @EJB
    IndexServiceBean indexService;

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
 
    public Dataset find(Object pk) {
        return em.find(Dataset.class, pk);
    }
    
    public List<Dataset> findByOwnerId(Long ownerId) {
        Query query = em.createQuery("select object(o) from Dataset as o where o.owner.id =:ownerId order by o.id");
        query.setParameter("ownerId", ownerId);
        return query.getResultList();
    }

    public List<Dataset> findAll() {
        return em.createQuery("select object(o) from Dataset as o order by o.id").getResultList();
    }

    public void generateFileSystemName(DataFile dataFile) {
        String fileSystemName = null;
        Long result = (Long) em.createNativeQuery("select nextval('filesystemname_seq')").getSingleResult();
        dataFile.setFileSystemName(result.toString());

    }

    /**
     * @todo write this method for real. Don't just iterate through every single
     * dataset! See https://redmine.hmdc.harvard.edu/issues/3988
     */
    public Dataset findByGlobalId(String globalId) {
        Dataset foundDataset = null;
        if (globalId != null) {
            Query query = em.createQuery("select object(o) from Dataset as o order by o.id");
            List<Dataset> datasets = query.getResultList();
            for (Dataset dataset : datasets) {
                if (globalId.equals(dataset.getGlobalId())) {
                    foundDataset = dataset;
                }
            }
        }
        return foundDataset;
    }
    
    public String generateIdentifierSequence(String protocol, String authority) {
     
        String identifier=null;
        do {
            identifier = ((Long) em.createNativeQuery("select nextval('dvobject_id_seq')").getSingleResult()).toString();

        } while (!isUniqueIdentifier(identifier, protocol, authority));
        
        
        return identifier;
        
    }
    
        /**
     *  Check that a studyId entered by the user is unique (not currently used for any other study in this Dataverse Network)
     */
   private boolean isUniqueIdentifier(String userIdentifier, String protocol,String authority) {
       String query = "SELECT d FROM Dataset d WHERE d.identifier = '" + userIdentifier +"'";
       query += " and d.protocol ='"+protocol+"'";
       query += " and d.authority = '"+authority+"'";
       boolean u = em.createQuery(query).getResultList().size()==0;
       return u;
    }
   
   public DatasetVersionDatasetUser getDatasetVersionDatasetUser(DatasetVersion version, DataverseUser user){        
       
        DatasetVersionDatasetUser  ddu = null;       
        Query query = em.createQuery("select object(o) from DatasetVersionDatasetUser as o "
                + "where o.datasetversionid =:versionId and o.dataverseuserid =:userId");
        query.setParameter("versionId", version.getId());
        query.setParameter("userId", user.getId());
        System.out.print("versionId: " +  version.getId());
        System.out.print("userId: " +  user.getId());
        System.out.print(query.toString());
        try {
            ddu = (DatasetVersionDatasetUser) query.getSingleResult();
        } catch (javax.persistence.NoResultException e) {
            // DO nothing, just return null.
        }
        return ddu;
   }
    
    /*
    public Study getStudyByGlobalId(String identifier) {
        String protocol = null;
        String authority = null;
        String studyId = null;
        int index1 = identifier.indexOf(':');
        int index2 = identifier.indexOf('/');
        int index3 = 0;
        if (index1 == -1) {
            throw new EJBException("Error parsing identifier: " + identifier + ". ':' not found in string");
        } else {
            protocol = identifier.substring(0, index1);
        }
        if (index2 == -1) {
            throw new EJBException("Error parsing identifier: " + identifier + ". '/' not found in string");

        } else {
            authority = identifier.substring(index1 + 1, index2);
        }
        if (protocol.equals("doi")){
           index3 = identifier.indexOf('/', index2 + 1 );
           if (index3== -1){
              studyId = identifier.substring(index2 + 1).toUpperCase();  
           } else {
              authority = identifier.substring(index1 + 1, index3);
              studyId = identifier.substring(index3 + 1).toUpperCase();  
           }
        }  else {
           studyId = identifier.substring(index2 + 1).toUpperCase(); 
        }      

        String queryStr = "SELECT s from Study s where s.studyId = :studyId  and s.protocol= :protocol and s.authority= :authority";

        Study study = null;
        try {
            Query query = em.createQuery(queryStr);
            query.setParameter("studyId", studyId);
            query.setParameter("protocol", protocol);
            query.setParameter("authority", authority);
            study = (Study) query.getSingleResult();
        } catch (javax.persistence.NoResultException e) {
            // DO nothing, just return null.
        }
        return study;
    }
*/
    
}
