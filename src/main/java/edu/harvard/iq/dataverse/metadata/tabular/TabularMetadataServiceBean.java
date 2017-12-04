/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.metadata.tabular;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataTable;
import edu.harvard.iq.dataverse.dataaccess.DataAccessOption;
import edu.harvard.iq.dataverse.dataaccess.StorageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.json.Json;
import javax.json.JsonReader;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

@Stateless
@Named
public class TabularMetadataServiceBean implements Serializable {

    private static final Logger logger = Logger.getLogger(TabularMetadataServiceBean.class.getCanonicalName());

    /*@PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;*/
    
    public static final String DATASUMMARY_AUX_EXTENSION = "prep";
    public static final String DATASUMMARY_AUX_DIFFPRIVATE = "_dp";
    
    public boolean processDataSummary(String metadataIn, DataFile dataFile, boolean diffPrivate, String formatVersion) {
        try {
            JsonReader jsonReader = Json.createReader(new StringReader(metadataIn));
            // The "Data Summary" is a JSON format previously known as the 
            // "preprocessed metadata" format. For example:
            // https://dataverse.harvard.edu/api/access/datafile/3040230/metadata/preprocessed'
            // The JSON format should be an object, not an array.
            // (this is only validation we are doing, for now).
            jsonReader.readObject();
        } catch (Exception ex) {
            logger.info("Exception parsing DataSummary JSON: " + ex);
            return false;
        }
        
        try {
            saveDataSummary(metadataIn, dataFile, diffPrivate, formatVersion);
        } catch (IOException ioex) {
            logger.info("IO Exception trying to save DataSummary metadata");
            return false;
        }
        return true;
    }
    
    public boolean isDataSummaryAvailable(DataFile dataFile, boolean diffPrivate, String formatVersion) {
        String auxExtension = getDataSummaryCacheExtension(diffPrivate);
        try {
            StorageIO<DataFile> storageIO = dataFile.getStorageIO();
            return storageIO.isAuxObjectCached(auxExtension);
        } catch (IOException ioex) {
            ioex.printStackTrace();
            return false;
        }
    }
    
    private void saveDataSummary(String metadataIn, DataFile dataFile, boolean diffPrivate, String formatVersion) throws IOException {
        
        ByteArrayInputStream metadataInputStream = new ByteArrayInputStream(metadataIn.getBytes());
        StorageIO<DataFile> storageIO = dataFile.getStorageIO();
        
        String auxExtension = getDataSummaryCacheExtension(diffPrivate);
        
        storageIO.saveInputStreamAsAux(metadataInputStream, auxExtension);
    }
    
    private String getDataSummaryCacheExtension(boolean diffPrivate) {
        String auxExtension = DATASUMMARY_AUX_EXTENSION; 
        if (diffPrivate) {
            auxExtension += DATASUMMARY_AUX_DIFFPRIVATE;
        }
        return auxExtension; 
    }

}
