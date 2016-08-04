/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.harvest.client.datafiletransfer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

/**
 *
 * @author anuj
 */
public class DataFileHTTPTransfer {
    
    //@PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManagerFactory entityManagerFactory;
    private EntityManagerFactory entityManagerFactory2;
        
    private static final Logger logger = Logger.getLogger("edu.harvard.iq."
            + "dataverse.harvest.client.datafiletransfer.DataFileHTTPParser");
   
    public DataFileHTTPTransfer(){
        entityManagerFactory = Persistence.createEntityManagerFactory( "VDCNet-ejbPU" );
        entityManagerFactory2 = Persistence.createEntityManagerFactory( "VDCNet-ejbPU" );   
    }
   
    public List<String> getListOfDataFileURL(){
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            logger.log(Level.INFO, "Retrieving List Of Data File URLs");
            return em.createQuery("SELECT df.fileSystemName "
                    + "from DataFile as df order by df.id").getResultList();
        }
        catch (Exception e) {
             
            logger.warning("Could not retrieve List of DataFile URLs");
            return null;
        }
    }
    
    public List<String> getListOfDataFileName(){
        EntityManager em = entityManagerFactory2.createEntityManager();
        try {
            logger.log(Level.INFO, "Retrieving List of Data File Names");
            return em.createQuery("SELECT fmd.label "
                    + "from FileMetadata as fmd order by fmd.dataFile.id").getResultList();
        }
        catch (Exception e){
            logger.warning("Could not retrieve List of DataFile Names");
            return null;
        }
    }
}
