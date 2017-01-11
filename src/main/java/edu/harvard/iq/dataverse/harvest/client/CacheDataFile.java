/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.harvest.client;

import javax.ejb.Stateless;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import edu.harvard.iq.dataverse.dataaccess.DataFileIO;
import edu.harvard.iq.dataverse.harvest.client.datafiletransfer.DataFileDownload;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;

/**
 *
 * @author anuj
 */

public class CacheDataFile extends Thread{
    
    
    private static final Logger logger = Logger.getLogger("edu.harvard.iq.dataverse.harvest.client.HarvesterServiceBean");
    
    
    @EJB
    Dataset harvestedDataset;
    
    @EJB
    DataFile harvestedFile;
    
    Semaphore available;
    
    @EJB
    DataFileServiceBean fileService;

    public CacheDataFile() {
        
    }
    
    public CacheDataFile(Dataset harvestedDataset, DataFile harvestedFile, DataFileServiceBean fileService, Semaphore available) {
        this.harvestedDataset = harvestedDataset;
        this.harvestedFile = harvestedFile;
        this.fileService = fileService;
        this.available = available;
    }
    
    public void harvestDataFile() {
        
        try {
        String dirName = "/tmp";
        
        //Adding semaphore to allow only 10 concurrent threads
        available.acquire();
        
        String fileUrl = harvestedFile.getStorageIdentifier();
        String fileName = harvestedFile.getFileMetadata().getLabel();
        DataFileIO dataAccess;
        Path tempFilePath;
        
        logger.info("Downloading "+fileUrl+" "+available.getQueueLength() + " " +available.availablePermits());
        
        try {
        
            DataFileDownload dfd = new DataFileDownload(fileUrl, dirName + "/" + fileName);
            dfd.saveDataFile(fileUrl, dirName + "/" + fileName);

            synchronized (this) {
                fileService.generateStorageIdentifier(harvestedFile);
            
            
                dataAccess = DataAccess.createNewDataFileIO(harvestedFile, harvestedFile.getStorageIdentifier());
                tempFilePath = Paths.get(dirName, fileName);
                
                try {
                    if (harvestedDataset.getFileSystemDirectory() != null
                            && !Files.exists(harvestedDataset.getFileSystemDirectory())) {
                        /* Note that "createDirectories()" must be used - not 
                                 * "createDirectory()", to make sure all the parent 
                                 * directories that may not yet exist are created as well. 
                         */
                        Files.createDirectories(harvestedDataset.getFileSystemDirectory());
                    }
                } catch (IOException dirEx) {
                    logger.severe("Failed to create study directory "
                            + harvestedDataset.getFileSystemDirectory().toString());
                    return;
                    // TODO:
                    // Decide how we are communicating failure information back to 
                    // the page, and what the page should be doing to communicate
                    // it to the user - if anything. 
                    // -- L.A. 
                }
            
            }
            // Copies the file from tmp location to the permanent 
            // directory i.e swift service endpoint.
            dataAccess.copyPath(tempFilePath);
            //logger.info("Dataaccess object: "+dataAccess);
            
            available.release();
        
        // Deleting the Data File from the /tmp directory.
        //if(harvestedFile.getStorageIdentifier().startsWith(DataAccess.DEFAULT_SWIFT_ENDPOINT_START_CHARACTERS))
        //Files.delete(tempFilePath);
        } catch(IOException IO) {
            logger.warning("Could not complete "+fileUrl);
        }
        } catch(InterruptedException ex) {
            Logger.getLogger(CacheDataFile.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    public void run() {
        this.harvestDataFile();
    }
}