/*
   Copyright (C) 2005-2012, by the President and Fellows of Harvard College.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

   Dataverse Network - A web application to share, preserve and analyze research data.
   Developed at the Institute for Quantitative Social Science, Harvard University.
   Version 3.0.
*/

package edu.harvard.iq.dataverse.ingest;

import edu.harvard.iq.dataverse.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.datavariable.VariableServiceBean;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetFieldValue;
import edu.harvard.iq.dataverse.DatasetPage;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.FileMetadataField;
import edu.harvard.iq.dataverse.FileMetadataFieldValue;
import edu.harvard.iq.dataverse.MetadataBlock;
import edu.harvard.iq.dataverse.dataaccess.ImageThumbConverter;
import edu.harvard.iq.dataverse.dataaccess.TabularSubsetGenerator;
import edu.harvard.iq.dataverse.datavariable.SummaryStatistic;
import edu.harvard.iq.dataverse.datavariable.DataVariable;
import edu.harvard.iq.dataverse.ingest.metadataextraction.FileMetadataExtractor;
import edu.harvard.iq.dataverse.ingest.metadataextraction.FileMetadataIngest;
import edu.harvard.iq.dataverse.ingest.metadataextraction.impl.plugins.fits.FITSFileMetadataExtractor;
import edu.harvard.iq.dataverse.ingest.tabulardata.TabularDataFileReader;
import edu.harvard.iq.dataverse.ingest.tabulardata.TabularDataIngest;
import edu.harvard.iq.dataverse.ingest.tabulardata.impl.plugins.dta.DTAFileReader;
import edu.harvard.iq.dataverse.ingest.tabulardata.impl.plugins.dta.DTAFileReaderSpi;
import edu.harvard.iq.dataverse.ingest.tabulardata.impl.plugins.rdata.RDATAFileReader;
import edu.harvard.iq.dataverse.ingest.tabulardata.impl.plugins.rdata.RDATAFileReaderSpi;
import edu.harvard.iq.dataverse.ingest.tabulardata.impl.plugins.csv.CSVFileReader;
import edu.harvard.iq.dataverse.ingest.tabulardata.impl.plugins.csv.CSVFileReaderSpi;
import edu.harvard.iq.dataverse.ingest.tabulardata.impl.plugins.xlsx.XLSXFileReader;
import edu.harvard.iq.dataverse.ingest.tabulardata.impl.plugins.xlsx.XLSXFileReaderSpi;
import edu.harvard.iq.dataverse.ingest.tabulardata.impl.plugins.sav.SAVFileReader;
import edu.harvard.iq.dataverse.ingest.tabulardata.impl.plugins.sav.SAVFileReaderSpi;
import edu.harvard.iq.dataverse.ingest.tabulardata.impl.plugins.por.PORFileReader;
import edu.harvard.iq.dataverse.ingest.tabulardata.impl.plugins.por.PORFileReaderSpi;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.SumStatCalculator;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.primefaces.model.UploadedFile;
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;
import javax.annotation.Resource;
import javax.jms.JMSException;
import javax.jms.QueueConnection;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Message;
import javax.faces.bean.ManagedBean;
import org.primefaces.push.PushContext;
import org.primefaces.push.PushContextFactory;
import javax.faces.application.FacesMessage;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author Leonid Andreev
 * dataverse 4.0
 * New service for handling ingest tasks
 * 
 */
@ManagedBean
@Stateless
@Named
public class IngestServiceBean {
    private static final Logger logger = Logger.getLogger(IngestServiceBean.class.getCanonicalName());
    @EJB
    VariableServiceBean variableService;
    @EJB 
    DatasetServiceBean datasetService;
    @EJB
    DatasetFieldServiceBean fieldService;
    @EJB
    DataFileServiceBean fileService; 

    @Resource(mappedName = "jms/DataverseIngest")
    Queue queue;
    @Resource(mappedName = "jms/IngestQueueConnectionFactory")
    QueueConnectionFactory factory;
    
    private static final String MIME_TYPE_STATA = "application/x-stata";
    private static final String MIME_TYPE_RDATA = "application/x-rlang-transport";
    private static final String MIME_TYPE_CSV   = "text/csv";
    private static final String MIME_TYPE_XLSX  = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String MIME_TYPE_SPSS_SAV = "application/x-spss-sav";
    private static final String MIME_TYPE_SPSS_POR = "application/x-spss-por";
    
    private static final String MIME_TYPE_TAB   = "text/tab-separated-values";
    
    private static final String MIME_TYPE_FITS  = "application/fits";
      
    // TODO: this constant should be provided by the Ingest Service Provder Registry;
    private static final String METADATA_SUMMARY = "FILE_METADATA_SUMMARY_INFO";
    
    public void produceSummaryStatistics(DataFile dataFile) throws IOException {
        //produceDiscreteNumericSummaryStatistics(dataFile); 
        produceContinuousSummaryStatistics(dataFile);
        //produceCharacterSummaryStatistics(dataFile);
    }
    
    public void produceContinuousSummaryStatistics(DataFile dataFile) throws IOException {

        Double[][] variableVectors = subsetContinuousVectors(dataFile);

        calculateContinuousSummaryStatistics(dataFile, variableVectors);

    }
    
    public boolean asyncIngestAsTabular(DataFile dataFile) {
        boolean ingestSuccessful = true;

        QueueConnection conn = null;
        QueueSession session = null;
        QueueSender sender = null;
        try {
            conn = factory.createQueueConnection();
            session = conn.createQueueSession(false, 0);
            sender = session.createSender(queue);

            IngestMessage ingestMessage = new IngestMessage(IngestMessage.INGEST_MESAGE_LEVEL_INFO);
            //ingestMessage.addFile(new File(tempFileLocation));
            ingestMessage.addFile(dataFile);

            Message message = session.createObjectMessage(ingestMessage);

            try {
                sender.send(message);
            } catch (Exception ex) {
                ingestSuccessful = false; 
                ex.printStackTrace();
            }

        } catch (JMSException ex) {
            ingestSuccessful = false;
            ex.printStackTrace();
        } finally {
            try {

                if (sender != null) {
                    sender.close();
                }
                if (session != null) {
                    session.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (JMSException ex) {
                ingestSuccessful = false;
                ex.printStackTrace();
            }
        }

        return ingestSuccessful;
    }
    
    public boolean ingestAsTabular(DataFile dataFile) throws IOException {
        return ingestAsTabular(dataFile.getFileSystemLocation().toString(), dataFile); 
    }
    
    public boolean ingestAsTabular(String tempFileLocation, DataFile dataFile) throws IOException {
        boolean ingestSuccessful = false;

        PushContext pushContext = PushContextFactory.getDefault().getPushContext();
        if (pushContext != null) {
            Logger.getLogger(DatasetPage.class.getName()).log(Level.FINE, "Ingest: Obtained push context "
                    + pushContext.toString());
        } else {
            Logger.getLogger(DatasetPage.class.getName()).log(Level.SEVERE, "Warning! Could not obtain push context.");
        }      
        
        // Locate ingest plugin for the file format by looking
        // it up with the Ingest Service Provider Registry:
        //TabularDataFileReader ingestPlugin = IngestSP.getTabDataReaderByMIMEType(dFile.getContentType());
        //TabularDataFileReader ingestPlugin = new DTAFileReader(new DTAFileReaderSpi());
        String fileName = dataFile.getFileMetadata().getLabel();
        TabularDataFileReader ingestPlugin = getTabDataReaderByMimeType(dataFile);

        if (ingestPlugin == null) {
            dataFile.SetIngestProblem();
            dataFile = fileService.save(dataFile);
            FacesMessage facesMessage = new FacesMessage("ingest failed");
            pushContext.push("/ingest"+dataFile.getOwner().getId(), facesMessage);
            Logger.getLogger(DatasetPage.class.getName()).log(Level.INFO, "Ingest failure: Sent push notification to the page.");
            throw new IOException("Could not find ingest plugin for the file " + fileName);
        }

        FileInputStream tempFileInputStream = null; 
        
        try {
            tempFileInputStream = new FileInputStream(new File(tempFileLocation));
        } catch (FileNotFoundException notfoundEx) {
            dataFile.SetIngestProblem();
            dataFile = fileService.save(dataFile);
            FacesMessage facesMessage = new FacesMessage("ingest failed");
            pushContext.push("/ingest"+dataFile.getOwner().getId(), facesMessage);
            Logger.getLogger(DatasetPage.class.getName()).log(Level.INFO, "Ingest failure: Sent push notification to the page.");
            throw new IOException("Could not open temp file "+tempFileLocation);
        }
        
        TabularDataIngest tabDataIngest = ingestPlugin.read(new BufferedInputStream(tempFileInputStream), null);

        if (tabDataIngest != null) {
            File tabFile = tabDataIngest.getTabDelimitedFile();

            if (tabDataIngest.getDataTable() != null
                    && tabFile != null
                    && tabFile.exists()) {

                Logger.getLogger(DatasetPage.class.getName()).log(Level.INFO, "Tabular data successfully ingested; DataTable with "
                        + tabDataIngest.getDataTable().getVarQuantity() + " variables produced.");

                Logger.getLogger(DatasetPage.class.getName()).log(Level.INFO, "Tab-delimited file produced: " + tabFile.getAbsolutePath());

                tabDataIngest.getDataTable().setOriginalFileFormat(dataFile.getContentType());
                
                // and we want to save the original of the ingested file: 
                try {
                    saveIngestedOriginal(dataFile, new FileInputStream(new File(tempFileLocation)));
                } catch (IOException iox) {
                    Logger.getLogger(DatasetPage.class.getName()).log(Level.INFO, "Failed to save the ingested original! " + iox.getMessage());
                }
                
                Files.copy(Paths.get(tabFile.getAbsolutePath()), dataFile.getFileSystemLocation(), StandardCopyOption.REPLACE_EXISTING);
                
                // and change the mime type to "tabular" on the final datafile, 
                // and replace (or add) the extension ".tab" to the filename: 
                
                dataFile.setContentType(MIME_TYPE_TAB);
                dataFile.getFileMetadata().setLabel(FileUtil.replaceExtension(fileName, "tab"));  

                dataFile.setDataTable(tabDataIngest.getDataTable());
                tabDataIngest.getDataTable().setDataFile(dataFile);
                
                dataFile.setIngestDone();
                dataFile = fileService.save(dataFile);
                
                try {
                    produceSummaryStatistics(dataFile);
                } catch (IOException sumStatEx) {
                    dataFile.SetIngestProblem();
                    dataFile = fileService.save(dataFile);
                    FacesMessage facesMessage = new FacesMessage("ingest failed");
                    pushContext.push("/ingest"+dataFile.getOwner().getId(), facesMessage);
                    Logger.getLogger(DatasetPage.class.getName()).log(Level.INFO, "Ingest failure: Sent push notification to the page.");
                    throw new IOException ("Ingest: failed to calculate summary statistics. "+sumStatEx.getMessage());
                }
                
                ingestSuccessful = true;                
            }
        }
        
        FacesMessage facesMessage = new FacesMessage("ingest done");
        pushContext.push("/ingest"+dataFile.getOwner().getId(), facesMessage);
        Logger.getLogger(DatasetPage.class.getName()).log(Level.INFO, "Ingest: Sent push notification to the page.");
        return ingestSuccessful;
    }

    public boolean ingestableAsTabular(DataFile dataFile) {
        /* 
         * In the final 4.0 we'll be doing real-time checks, going through the 
         * available plugins and verifying the lists of mime types that they 
         * can handle. In 4.0 beta, the ingest plugins are still built into the 
         * main code base, so we can just go through a hard-coded list of mime 
         * types. -- L.A. 
         */
        
        String mimeType = dataFile.getContentType();
        
        if (mimeType == null) {
            return false;
        }
        
        if (mimeType.equals(MIME_TYPE_STATA)) {
            return true;
        } else if (mimeType.equals(MIME_TYPE_RDATA)) {
            return true;
        } else if (mimeType.equals(MIME_TYPE_CSV)) {
            return true;
        } else if (mimeType.equals(MIME_TYPE_XLSX)) {
            return true;
        } else if (mimeType.equals(MIME_TYPE_SPSS_SAV)) {
            return true;
        } else if (mimeType.equals(MIME_TYPE_SPSS_POR)) {
            return true;
        }

        return false;
    }
    
    private TabularDataFileReader getTabDataReaderByMimeType(DataFile dataFile) {
        /* 
         * Same as the comment above; since we don't have any ingest plugins loadable 
         * in real times yet, we can select them by a fixed list of mime types. 
         * -- L.A. 4.0 beta.
         */

        String mimeType = dataFile.getContentType();
        
        if (mimeType == null) {
            return null;
        }

        TabularDataFileReader ingestPlugin = null;

        if (mimeType.equals(MIME_TYPE_STATA)) {
            ingestPlugin = new DTAFileReader(new DTAFileReaderSpi());
        } else if (mimeType.equals(MIME_TYPE_RDATA)) {
            ingestPlugin = new RDATAFileReader(new RDATAFileReaderSpi());
        } else if (mimeType.equals(MIME_TYPE_CSV)) {
            ingestPlugin = new CSVFileReader(new CSVFileReaderSpi());
        } else if (mimeType.equals(MIME_TYPE_XLSX)) {
            ingestPlugin = new XLSXFileReader(new XLSXFileReaderSpi());
        } else if (mimeType.equals(MIME_TYPE_SPSS_SAV)) {
            ingestPlugin = new DTAFileReader(new SAVFileReaderSpi());
        } else if (mimeType.equals(MIME_TYPE_SPSS_POR)) {
            ingestPlugin = new DTAFileReader(new PORFileReaderSpi());
        }

        return ingestPlugin;
    }
    
    public boolean fileMetadataExtractable(DataFile dataFile) {
        /* 
         * Eventually we'll be consulting the Ingest Service Provider Registry
         * to see if there is a plugin for this type of file;
         * for now - just a hardcoded list of mime types:
         *  -- L.A. 4.0 beta
         */
        if (dataFile.getContentType() != null && dataFile.getContentType().equals(MIME_TYPE_FITS)) {
            return true;
        }
        return false;
    }
    
    public boolean extractIndexableMetadata(String tempFileLocation, DataFile dataFile, DatasetVersion editVersion) throws IOException {
        boolean ingestSuccessful = false;

        FileInputStream tempFileInputStream = null; 
        
        try {
            tempFileInputStream = new FileInputStream(new File(tempFileLocation));
        } catch (FileNotFoundException notfoundEx) {
            throw new IOException("Could not open temp file "+tempFileLocation);
        }
        
        // Locate metadata extraction plugin for the file format by looking
        // it up with the Ingest Service Provider Registry:
        //FileMetadataExtractor extractorPlugin = IngestSP.getMetadataExtractorByMIMEType(dfile.getContentType());
        FileMetadataExtractor extractorPlugin = new FITSFileMetadataExtractor();

        FileMetadataIngest extractedMetadata = extractorPlugin.ingest(new BufferedInputStream(tempFileInputStream));
        Map<String, Set<String>> extractedMetadataMap = extractedMetadata.getMetadataMap();

        // Store the fields and values we've gathered for safe-keeping:
        // from 3.6:
        // attempt to ingest the extracted metadata into the database; 
        // TODO: this should throw an exception if anything goes wrong.
        FileMetadata fileMetadata = dataFile.getFileMetadata();

        if (extractedMetadataMap != null) {
            logger.fine("Ingest Service: Processing extracted metadata;");
            if (extractedMetadata.getMetadataBlockName() != null) {
                logger.fine("Ingest Service: This metadata belongs to the "+extractedMetadata.getMetadataBlockName()+" metadata block."); 
                ingestDatasetMetadata(extractedMetadata, editVersion);
            }
            
            ingestFileLevelMetadata(extractedMetadata, dataFile, fileMetadata, extractorPlugin.getFormatName());

        }

        ingestSuccessful = true;

        return ingestSuccessful;
    }

    
    private void ingestDatasetMetadata(FileMetadataIngest fileMetadataIngest, DatasetVersion editVersion) throws IOException {
        
        
        for (MetadataBlock mdb : editVersion.getDataset().getOwner().getMetadataBlocks()) {  
            if (mdb.getName().equals(fileMetadataIngest.getMetadataBlockName())) {
                logger.fine("Ingest Service: dataset version has "+mdb.getName()+" metadata block enabled.");
                
                editVersion.setDatasetFields(editVersion.initDatasetFields());
                
                Map<String, Set<String>> fileMetadataMap = fileMetadataIngest.getMetadataMap();
                for (DatasetFieldType dsft : mdb.getDatasetFieldTypes()) {
                    if (dsft.isPrimitive()) {
                        String dsfName = dsft.getName();
                        // See if the plugin has found anything for this field: 
                        if (fileMetadataMap.get(dsfName) != null && !fileMetadataMap.get(dsfName).isEmpty()) {
                            logger.fine("Ingest Service: found extracted metadata for field " + dsfName);
                            // go through the existing fields:
                            for (DatasetField dsf : editVersion.getFlatDatasetFields()) {
                                String fName = dsf.getDatasetFieldType().getName();
                                if (dsf.getDatasetFieldType().equals(dsft)) {
                                    // yep, this is our field!
                                    // let's go through the values that the ingest 
                                    // plugin found in the file for this field: 
                                    Set<String> mValues = fileMetadataMap.get(dsfName);
                                    for (String fValue : mValues) {
                                        if (!dsft.isControlledVocabulary()) {
                                            // Need to only add the values not yet present!
                                            // (the method below may be inefficient - ?)
                                            boolean valueExists = false;

                                            Iterator<DatasetFieldValue> dsfvIt = dsf.getDatasetFieldValues().iterator();

                                            while (dsfvIt.hasNext()) {
                                                DatasetFieldValue dsfv = dsfvIt.next();
                                                if (fValue.equals(dsfv.getValue())) {
                                                    logger.fine("Value " + fValue + " already exists for field " + dsfName);
                                                    valueExists = true;
                                                    break;
                                                }
                                            }

                                            if (!valueExists) {
                                                logger.fine("Creating a new value for field " + dsfName + ": " + fValue);
                                                DatasetFieldValue newDsfv = new DatasetFieldValue(dsf);
                                                newDsfv.setValue(fValue);
                                                dsf.getDatasetFieldValues().add(newDsfv);
                                            }
                                        } else {
                                            // A controlled vocabulary entry: 
                                            // first, let's see if it's a legit control vocab. entry: 
                                            ControlledVocabularyValue legitControlledVocabularyValue = null;
                                            Collection<ControlledVocabularyValue> definedVocabularyValues = dsft.getControlledVocabularyValues();
                                            if (definedVocabularyValues != null) {
                                                for (ControlledVocabularyValue definedVocabValue : definedVocabularyValues) {
                                                    if (fValue.equals(definedVocabValue.getStrValue())) {
                                                        logger.fine("Yes, " + fValue + " is a valid controlled vocabulary value for the field " + dsfName);
                                                        legitControlledVocabularyValue = definedVocabValue;
                                                        break;
                                                    }
                                                }
                                            }
                                            if (legitControlledVocabularyValue != null) {
                                                // Only need to add the value if it is new, 
                                                // i.e. if it does not exist yet: 
                                                boolean valueExists = false;

                                                List<ControlledVocabularyValue> existingControlledVocabValues = dsf.getControlledVocabularyValues();
                                                if (existingControlledVocabValues != null) {
                                                    Iterator<ControlledVocabularyValue> cvvIt = existingControlledVocabValues.iterator();
                                                    while (cvvIt.hasNext()) {
                                                        ControlledVocabularyValue cvv = cvvIt.next();
                                                        if (fValue.equals(cvv.getStrValue())) {
                                                            // or should I use if (legitControlledVocabularyValue.equals(cvv)) ?
                                                            logger.fine("Controlled vocab. value " + fValue + " already exists for field " + dsfName);
                                                            valueExists = true;
                                                            break;
                                                        }
                                                    }
                                                }

                                                if (!valueExists) {
                                                    logger.fine("Adding controlled vocabulary value " + fValue + " to field " + dsfName);
                                                    dsf.getControlledVocabularyValues().add(legitControlledVocabularyValue);
                                                }
                                            }
                                        }
                                    }

                                }
                            }
                        }
                    }
                } //else {
                    // A compound field: 
                    // - but that's not going to happen!
                    // because ... (TODO: add explanation! -- L.A. 4.0 alpha
                //}
            }
        }  
    }
    
    
    private void ingestFileLevelMetadata(FileMetadataIngest fileLevelMetadata, DataFile dataFile, FileMetadata fileMetadata, String fileFormatName) {
        // First, add the "metadata summary" generated by the file reader/ingester
        // to the fileMetadata object, as the "description":
        String metadataSummary = fileLevelMetadata.getMetadataSummary();
        if (metadataSummary != null) {
            if (!metadataSummary.equals("")) {
                // The AddFiles page allows a user to enter file description 
                // on ingest. We don't want to overwrite whatever they may 
                // have entered. Rather, we'll append our metadata summary 
                // to the existing value. 
                String userEnteredFileDescription = fileMetadata.getDescription();
                if (userEnteredFileDescription != null
                        && !(userEnteredFileDescription.equals(""))) {

                    metadataSummary = userEnteredFileDescription.concat("\n" + metadataSummary);
                }
                fileMetadata.setDescription(metadataSummary);
            }
        }

        Map<String, Set<String>> fileMetadataMap = fileLevelMetadata.getMetadataMap();

        // And now we can go through the remaining key/value pairs in the 
        // metadata maps and process the metadata elements that were found in the 
        // file: 
        for (String mKey : fileMetadataMap.keySet()) {

            Set<String> mValues = fileMetadataMap.get(mKey);

            Logger.getLogger(DatasetPage.class.getName()).log(Level.FINE, "Looking up file meta field " + mKey + ", file format " + fileFormatName);
            FileMetadataField fileMetaField = fieldService.findFileMetadataFieldByNameAndFormat(mKey, fileFormatName);

            if (fileMetaField == null) {
                //fileMetaField = studyFieldService.createFileMetadataField(mKey, fileFormatName); 
                fileMetaField = new FileMetadataField();

                if (fileMetaField == null) {
                    Logger.getLogger(DatasetPage.class.getName()).log(Level.WARNING, "Failed to create a new File Metadata Field; skipping.");
                    continue;
                }

                fileMetaField.setName(mKey);
                fileMetaField.setFileFormatName(fileFormatName);
                // TODO: provide meaningful descriptions and labels:
                fileMetaField.setDescription(mKey);
                fileMetaField.setTitle(mKey);

                try {
                    fieldService.saveFileMetadataField(fileMetaField);
                } catch (Exception ex) {
                    Logger.getLogger(DatasetPage.class.getName()).log(Level.WARNING, "Failed to save new file metadata field (" + mKey + "); skipping values.");
                    continue;
                }

                Logger.getLogger(DatasetPage.class.getName()).log(Level.FINE, "Created file meta field " + mKey);
            }

            String fieldValueText = null;

            if (mValues != null) {
                for (String mValue : mValues) {
                    if (mValue != null) {
                        if (fieldValueText == null) {
                            fieldValueText = mValue;
                        } else {
                            fieldValueText = fieldValueText.concat(" ".concat(mValue));
                        }
                    }
                }
            }

            FileMetadataFieldValue fileMetaFieldValue = null;

            if (!"".equals(fieldValueText)) {
                Logger.getLogger(DatasetPage.class.getName()).log(Level.FINE, "Attempting to create a file meta value for study file " + dataFile.getId() + ", value " + fieldValueText);
                if (dataFile != null) {
                    fileMetaFieldValue
                            = new FileMetadataFieldValue(fileMetaField, dataFile, fieldValueText);
                }
            }
            if (fileMetaFieldValue == null) {
                Logger.getLogger(DatasetPage.class.getName()).log(Level.WARNING, "Failed to create a new File Metadata Field value; skipping");
                continue;
            } else {
                if (dataFile.getFileMetadataFieldValues() == null) {
                    dataFile.setFileMetadataFieldValues(new ArrayList<FileMetadataFieldValue>());
                }
                dataFile.getFileMetadataFieldValues().add(fileMetaFieldValue);
            }
        }
    }
    
    public void performPostProcessingTasks(DataFile dataFile) {
        /*
         * At this point (4.0 alpha 1) the only ingest "post-processing task" performed 
         * is pre-generation of image thumbnails in a couple of popular sizes. 
         * -- L.A. 
         */
        if (dataFile != null && dataFile.isImage()) {
            ImageThumbConverter.generateImageThumb(dataFile.getFileSystemLocation().toString(), ImageThumbConverter.DEFAULT_THUMBNAIL_SIZE);
            ImageThumbConverter.generateImageThumb(dataFile.getFileSystemLocation().toString(), ImageThumbConverter.DEFAULT_PREVIEW_SIZE);
        }
    }
    
    private void saveIngestedOriginal(DataFile dataFile, InputStream originalFileStream) throws IOException {
        String ingestedFileName = dataFile.getFileSystemName();

        if (ingestedFileName != null && !ingestedFileName.equals("")) {
            Path savedOriginalPath = Paths.get(dataFile.getOwner().getFileSystemDirectory().toString(), "_" + ingestedFileName);
            Files.copy(originalFileStream, savedOriginalPath);
        } else {
            throw new IOException("Ingested tabular data file: no filesystem name.");
        }
    }
 
    private Set<Integer> selectContinuousVariableColumns(DataFile dataFile) {
        Set<Integer> contVarFields = new LinkedHashSet<Integer>();

        for (int i = 0; i < dataFile.getDataTable().getVarQuantity(); i++) {
            if ("continuous".equals(dataFile.getDataTable().getDataVariables().get(i).getVariableIntervalType().getName())) {
                contVarFields.add(i);
            }
        }

        return contVarFields;
    }
    
    private Double[][] subsetContinuousVectors(DataFile dataFile) throws IOException {
        Set<Integer> contVarFields = selectContinuousVariableColumns(dataFile);
        
        FileInputStream tabFileStream = null; 
        
        try {
            tabFileStream = new FileInputStream(dataFile.getFileSystemLocation().toFile());
        } catch (FileNotFoundException notfoundEx) {
            throw new IOException ("Could not open file "+dataFile.getFileSystemLocation());
        }
        
        Double[][] variableVectors = null; 
        
        variableVectors = TabularSubsetGenerator.subsetDoubleVectors(tabFileStream, contVarFields, dataFile.getDataTable().getCaseQuantity().intValue());
        
        return variableVectors;
    }
    
    private void calculateContinuousSummaryStatistics(DataFile dataFile, Double[][] dataVectors) throws IOException {
        int k = 0;
        for (int i = 0; i < dataFile.getDataTable().getVarQuantity(); i++) {
            if ("continuous".equals(dataFile.getDataTable().getDataVariables().get(i).getVariableIntervalType().getName())) {
                double[] sumStats = SumStatCalculator.calculateSummaryStatistics(dataVectors[k++]);

                assignContinuousSummaryStatistics(dataFile.getDataTable().getDataVariables().get(i), sumStats);
            }
        }
    }
    
    private void assignContinuousSummaryStatistics(DataVariable variable, double[] sumStats) throws IOException {
        if (sumStats == null || sumStats.length != variableService.summaryStatisticTypes.length) {
            throw new IOException ("Wrong number of summary statistics types calculated! ("+sumStats.length+")");
        }
        
        for (int j = 0; j < variableService.summaryStatisticTypes.length; j++) {
            SummaryStatistic ss = new SummaryStatistic();
            ss.setType(variableService.findSummaryStatisticTypeByName(variableService.summaryStatisticTypes[j]));
            if (!"mode".equals(variableService.summaryStatisticTypes[j])) {
                ss.setValue((new Double(sumStats[j])).toString());
            } else {
                ss.setValue(".");
            }
            ss.setDataVariable(variable);
            variable.getSummaryStatistics().add(ss);
        }

    }
}
