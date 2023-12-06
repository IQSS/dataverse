package edu.harvard.iq.dataverse.storageuse;

import edu.harvard.iq.dataverse.settings.JvmSettings;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Optional;
import java.util.logging.Logger;

/**
 *
 * @author landreev
 */
@Stateless
@Named
public class StorageUseServiceBean  implements java.io.Serializable {
    private static final Logger logger = Logger.getLogger(StorageUseServiceBean.class.getCanonicalName());
    
    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    
    public StorageUse findByDvContainerId(Long dvObjectId) {
        return em.createNamedQuery("StorageUse.findByDvContainerId", StorageUse.class).setParameter("dvObjectId", dvObjectId).getSingleResult();
    }
    
    /**
     * Looks up the current storage use size, using a named query in a new 
     * transaction
     * @param dvObjectId
     * @return 
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Long findStorageSizeByDvContainerId(Long dvObjectId) {
        Long res = em.createNamedQuery("StorageUse.findByteSizeByDvContainerId", Long.class).setParameter("dvObjectId", dvObjectId).getSingleResult();
        return res == null ? 0L : res;
    }
    
    /**
     * Increments the recorded storage size for all the dvobject parents of a
     * datafile, recursively. 
     * @param dvObjectContainerId database id of the immediate parent (dataset)
     * @param increment size in bytes of the file(s) being added 
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void incrementStorageSizeRecursively(Long dvObjectContainerId, Long increment) {
        if (dvObjectContainerId != null && increment != null) {
            Optional<Boolean> allow = JvmSettings.STORAGEUSE_DISABLE_UPDATES.lookupOptional(Boolean.class);
            if (!(allow.isPresent() && allow.get())) {
                String queryString = "WITH RECURSIVE uptree (id, owner_id) AS\n"
                        + "("
                        + "    SELECT id, owner_id\n"
                        + "    FROM dvobject\n"
                        + "    WHERE id=" + dvObjectContainerId + "\n"
                        + "    UNION ALL\n"
                        + "    SELECT dvobject.id, dvobject.owner_id\n"
                        + "    FROM dvobject\n"
                        + "    JOIN uptree ON dvobject.id = uptree.owner_id)\n"
                        + "UPDATE storageuse SET sizeinbytes=COALESCE(sizeinbytes,0)+" + increment + "\n"
                        + "FROM uptree\n"
                        + "WHERE dvobjectcontainer_id = uptree.id;";

                int parentsUpdated = em.createNativeQuery(queryString).executeUpdate();
            }
        }
        // @todo throw an exception if the number of parent dvobjects updated by
        // the query is < 2 - ? 
    }
    
}
