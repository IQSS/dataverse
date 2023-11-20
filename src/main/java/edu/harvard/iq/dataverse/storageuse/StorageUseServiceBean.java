/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package edu.harvard.iq.dataverse.storageuse;

import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.DvObjectContainer;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.logging.Logger;

/**
 *
 * @author landreev
 */
@Stateless
@Named
public class StorageUseServiceBean  implements java.io.Serializable {
    private static final Logger logger = Logger.getLogger(StorageUseServiceBean.class.getCanonicalName());
    @EJB
    DataverseServiceBean dataverseService;
    
    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    
    public StorageUse findByDvContainerId(Long dvObjectId) {
        return em.createNamedQuery("StorageUse.findByDvContainerId", StorageUse.class).setParameter("dvObjectId", dvObjectId).getSingleResult();
    }
    
    public Long findStorageSizeByDvContainerId(Long dvObjectId) {
        return em.createNamedQuery("StorageUse.findByteSizeByDvContainerId", Long.class).setParameter("dvObjectId", dvObjectId).getSingleResult();
    }
    
    public void incrementStorageSizeHierarchy(DvObjectContainer dvObject, Long filesize) {
        incrementStorageSize(dvObject, filesize); 
        DvObjectContainer parent = dvObject.getOwner();
        while (parent != null) {
            incrementStorageSize(parent, filesize);
            parent = parent.getOwner();
        }
    }
    
    /**
     * Should this be done in a new transaction?
     * @param dvObject
     * @param filesize 
     */
    public void incrementStorageSize(DvObjectContainer dvObject, Long filesize) {
        StorageUse dvContainerSU = findByDvContainerId(dvObject.getId());
        if (dvContainerSU != null) {
            // @todo: named query
            dvContainerSU.incrementSizeInBytes(filesize);
            em.merge(dvContainerSU);
        } else {
            dvContainerSU = new StorageUse(dvObject, filesize); 
            em.persist(dvContainerSU);
        }
    }
    
}
