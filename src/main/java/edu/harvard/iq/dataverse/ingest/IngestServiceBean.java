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
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetFieldValue;
import edu.harvard.iq.dataverse.DatasetFieldCompoundValue;
import edu.harvard.iq.dataverse.DatasetPage;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.FileMetadata;
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
import edu.harvard.iq.dataverse.util.MD5Checksum;
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
    private static final String MIME_TYPE_CSV_ALT = "text/comma-separated-values";
    private static final String MIME_TYPE_XLSX  = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String MIME_TYPE_SPSS_SAV = "application/x-spss-sav";
    private static final String MIME_TYPE_SPSS_POR = "application/x-spss-por";
    
    private static final String MIME_TYPE_TAB   = "text/tab-separated-values";
    
    private static final String MIME_TYPE_FITS  = "application/fits";
      
    // TODO: this constant should be provided by the Ingest Service Provder Registry;
    private static final String METADATA_SUMMARY = "FILE_METADATA_SUMMARY_INFO";
    
    
    public DataFile createDataFile(DatasetVersion version, InputStream inputStream, String fileName, String contentType) throws IOException {
        Dataset dataset = version.getDataset();
        DataFile datafile;
        
        FileMetadata fmd = new FileMetadata();

        if (contentType != null && !contentType.equals("")) {
            datafile = new DataFile(contentType);
            fmd.setCategory(contentType);
        } else {
            datafile = new DataFile("application/octet-stream"); 
        }

        fmd.setLabel(fileName);

        datafile.setOwner(dataset);
        fmd.setDataFile(datafile);

        datafile.getFileMetadatas().add(fmd);

        if (version.getFileMetadatas() == null) {
            version.setFileMetadatas(new ArrayList());
        }
        version.getFileMetadatas().add(fmd);
        fmd.setDatasetVersion(version);
        dataset.getFiles().add(datafile);

        datasetService.generateFileSystemName(datafile);

        // save the file, in the temporary location for now: 
        String tempFilesDirectory = getFilesTempDirectory();
        if (tempFilesDirectory != null) {
            //try {

                logger.fine("Will attempt to save the file as: " + tempFilesDirectory + "/" + datafile.getFileSystemName());
                Files.copy(inputStream, Paths.get(tempFilesDirectory, datafile.getFileSystemName()), StandardCopyOption.REPLACE_EXISTING);
            //} catch (IOException ioex) {
            //    logger.warning("Failed to save the file  " + datafile.getFileSystemName());
            //    return;
            //}
        }

        // Let's try our own utilities (Jhove, etc.) to determine the file type 
        // of the uploaded file. (We may already have a mime type supplied for this
        // file - maybe the type that the browser recognized on upload; or, if 
        // it's a harvest, maybe the remote server has already given us the type
        // for this file... with our own type utility we may or may not do better 
        // than the type supplied:
        //  -- L.A. 
        String recognizedType = null;
        try {
            recognizedType = FileUtil.determineFileType(Paths.get(tempFilesDirectory, datafile.getFileSystemName()).toFile(), fmd.getLabel());
            logger.fine("File utility recognized the file as " + recognizedType);
            if (recognizedType != null && !recognizedType.equals("")) {
                // is it any better than the type that was supplied to us,
                // if any?

                if (contentType == null || contentType.equals("") || contentType.equalsIgnoreCase("application/octet-stream")) {
                    datafile.setContentType(recognizedType);
                }
            }
        } catch (IOException ex) {
            logger.warning("Failed to run the file utility mime type check on file " + fmd.getLabel());
        }
        
        return datafile;
    }
    
    public void addFiles (DatasetVersion version, List<DataFile> newFiles) {
        if (newFiles != null && newFiles.size() > 0) {
            Dataset dataset = version.getDataset();
            
            try {
                if (dataset.getFileSystemDirectory() != null && !Files.exists(dataset.getFileSystemDirectory())) {
                    /* Note that "createDirectories()" must be used - not 
                     * "createDirectory()", to make sure all the parent 
                     * directories that may not yet exist are created as well. 
                     */

                    Files.createDirectories(dataset.getFileSystemDirectory());
                }
            } catch (IOException dirEx) {
                logger.severe("Failed to create study directory " + dataset.getFileSystemDirectory().toString());
                return; 
                // TODO:
                // Decide how we are communicating failure information back to 
                // the page, and what the page should be doing to communicate
                // it to the user - if anything. 
                // -- L.A. 
            }

            if (dataset.getFileSystemDirectory() != null && Files.exists(dataset.getFileSystemDirectory())) {
                for (DataFile dataFile : newFiles) {
                    String tempFileLocation = getFilesTempDirectory() + "/" + dataFile.getFileSystemName();

                    FileMetadata fileMetadata = dataFile.getFileMetadatas().get(0);
                    String fileName = fileMetadata.getLabel();
                    // These are all brand new files, so they should all have 
                    // one filemetadata total. -- L.A. 
                    boolean metadataExtracted = false;

                    datasetService.generateFileSystemName(dataFile);

                    if (ingestableAsTabular(dataFile)) {
                        /*
                         * Note that we don't try to ingest the file right away - 
                         * instead we mark it as "scheduled for ingest", then at 
                         * the end of the save process it will be queued for async. 
                         * ingest in the background. In the meantime, the file 
                         * will be ingested as a regular, non-tabular file, and 
                         * appear as such to the user, until the ingest job is
                         * finished with the Ingest Service.
                         */
                        dataFile.SetIngestScheduled();
                    } else if (fileMetadataExtractable(dataFile)) {

                        try {
                            // FITS is the only type supported for metadata 
                            // extraction, as of now. -- L.A. 4.0 
                            dataFile.setContentType("application/fits");
                            metadataExtracted = extractMetadata(tempFileLocation, dataFile, version);
                        } catch (IOException mex) {
                            logger.severe("Caught exception trying to extract indexable metadata from file " + fileName + ",  " + mex.getMessage());
                        }
                        if (metadataExtracted) {
                            logger.info("Successfully extracted indexable metadata from file " + fileName);
                        } else {
                            logger.info("Failed to extract indexable metadata from file " + fileName);
                        }
                    }

                    // Try to save the file in its permanent location: 
                    try {

                        logger.info("Will attempt to save the file as: " + dataFile.getFileSystemLocation().toString());
                        Files.copy(new FileInputStream(new File(tempFileLocation)), dataFile.getFileSystemLocation(), StandardCopyOption.REPLACE_EXISTING);

                        MD5Checksum md5Checksum = new MD5Checksum();
                        try {
                            dataFile.setmd5(md5Checksum.CalculateMD5(dataFile.getFileSystemLocation().toString()));
                        } catch (Exception md5ex) {
                            logger.warning("Could not calculate MD5 signature for the new file " + fileName);
                        }

                    } catch (IOException ioex) {
                        logger.warning("Failed to save the file  " + dataFile.getFileSystemLocation());
                    }

                    // Any necessary post-processing: 
                    performPostProcessingTasks(dataFile);
                }
            }
        }
    }
    
    public String getFilesTempDirectory() {
        String filesRootDirectory = System.getProperty("dataverse.files.directory");
        if (filesRootDirectory == null || filesRootDirectory.equals("")) {
            filesRootDirectory = "/tmp/files";
        }

        String filesTempDirectory = filesRootDirectory + "/temp";

        if (!Files.exists(Paths.get(filesTempDirectory))) {
            /* Note that "createDirectories()" must be used - not 
             * "createDirectory()", to make sure all the parent 
             * directories that may not yet exist are created as well. 
             */
            try {
                Files.createDirectories(Paths.get(filesTempDirectory));
            } catch (IOException ex) {
                return null;
            }
        }

        return filesTempDirectory;
    }
    
    public void startIngestJobs (Dataset dataset) {
        for (DataFile dataFile : dataset.getFiles()) {
            if (dataFile.isIngestScheduled()) {
                dataFile.SetIngestInProgress();
                logger.info("Attempting to queue the file " + dataFile.getFileMetadata().getLabel() + " for ingest.");
                asyncIngestAsTabular(dataFile);
            }
        }
    }
    
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

                if (MIME_TYPE_CSV_ALT.equals(dataFile.getContentType())) {
                    tabDataIngest.getDataTable().setOriginalFileFormat(MIME_TYPE_CSV);
                } else {
                    tabDataIngest.getDataTable().setOriginalFileFormat(dataFile.getContentType());
                }
                
                
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
        } else if (mimeType.equals(MIME_TYPE_CSV) || mimeType.equals(MIME_TYPE_CSV_ALT)) {
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
        } else if (mimeType.equals(MIME_TYPE_CSV) || mimeType.equals(MIME_TYPE_CSV_ALT)) {
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
    
    /* 
     * extractMetadata: 
     * framework for extracting metadata from uploaded files. The results will 
     * be used to populate the metadata of the Dataset to which the file belongs. 
    */
    public boolean extractMetadata(String tempFileLocation, DataFile dataFile, DatasetVersion editVersion) throws IOException {
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
                processDatasetMetadata(extractedMetadata, editVersion);
            }
            
            processFileLevelMetadata(extractedMetadata, fileMetadata);

        }

        ingestSuccessful = true;

        return ingestSuccessful;
    }

    
    private void processDatasetMetadata(FileMetadataIngest fileMetadataIngest, DatasetVersion editVersion) throws IOException {
        
        
        for (MetadataBlock mdb : editVersion.getDataset().getOwner().getMetadataBlocks()) {  
            if (mdb.getName().equals(fileMetadataIngest.getMetadataBlockName())) {
                logger.fine("Ingest Service: dataset version has "+mdb.getName()+" metadata block enabled.");
                
                editVersion.setDatasetFields(editVersion.initDatasetFields());
                
                Map<String, Set<String>> fileMetadataMap = fileMetadataIngest.getMetadataMap();
                for (DatasetFieldType dsft : mdb.getDatasetFieldTypes()) {
                    if (dsft.isPrimitive()) {
                        if (!dsft.isHasParent()) {
                        String dsfName = dsft.getName();
                        // See if the plugin has found anything for this field: 
                        if (fileMetadataMap.get(dsfName) != null && !fileMetadataMap.get(dsfName).isEmpty()) {
                            
                            logger.fine("Ingest Service: found extracted metadata for field " + dsfName);
                            // go through the existing fields:
                            for (DatasetField dsf : editVersion.getFlatDatasetFields()) {
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
                    } else {
                        // A compound field: 
                        // See if the plugin has found anything for the fields that 
                        // make up this compound field; if we find at least one 
                        // of the child values in the map of extracted values, we'll 
                        // create a new compound field value and its child 
                        // 
                        DatasetFieldCompoundValue compoundDsfv = new DatasetFieldCompoundValue();
                        int nonEmptyFields = 0; 
                        for (DatasetFieldType cdsft : dsft.getChildDatasetFieldTypes()) {
                            String dsfName = cdsft.getName();
                            if (fileMetadataMap.get(dsfName) != null && !fileMetadataMap.get(dsfName).isEmpty()) {  
                                logger.fine("Ingest Service: found extracted metadata for field " + dsfName + ", part of the compound field "+dsft.getName());
                                
                                if (cdsft.isPrimitive()) {
                                    // probably an unnecessary check - child fields
                                    // of compound fields are always primitive... 
                                    // but maybe it'll change in the future. 
                                    if (!cdsft.isControlledVocabulary()) {
                                        // TODO: can we have controlled vocabulary
                                        // sub-fields inside compound fields?
                                        
                                        DatasetField childDsf = new DatasetField();
                                        childDsf.setDatasetFieldType(cdsft);
                                        
                                        DatasetFieldValue newDsfv = new DatasetFieldValue(childDsf);
                                        newDsfv.setValue((String)fileMetadataMap.get(dsfName).toArray()[0]);
                                        childDsf.getDatasetFieldValues().add(newDsfv);
                                        
                                        childDsf.setParentDatasetFieldCompoundValue(compoundDsfv);
                                        compoundDsfv.getChildDatasetFields().add(childDsf);
                                        
                                        nonEmptyFields++;
                                    }
                                } 
                            }
                        }
                        
                        if (nonEmptyFields > 0) {
                            // let's go through this dataset's fields and find the 
                            // actual parent for this sub-field: 
                            for (DatasetField dsf : editVersion.getFlatDatasetFields()) {
                                if (dsf.getDatasetFieldType().equals(dsft)) {
                                    
                                    // Now let's check that the dataset version doesn't already have
                                    // this compound value - we are only interested in aggregating 
                                    // unique values. Note that we need to compare compound values 
                                    // as sets! -- i.e. all the sub fields in 2 compound fields 
                                    // must match in order for these 2 compounds to be recognized 
                                    // as "the same":
                                    
                                    boolean alreadyExists = false; 
                                    for (DatasetFieldCompoundValue dsfcv : dsf.getDatasetFieldCompoundValues()) {
                                        int matches = 0; 

                                        for (DatasetField cdsf : dsfcv.getChildDatasetFields()) {
                                            String cdsfName = cdsf.getDatasetFieldType().getName();
                                            String cdsfValue = cdsf.getDatasetFieldValues().get(0).getValue();
                                            if (cdsfValue != null && !cdsfValue.equals("")) {
                                                String extractedValue = (String)fileMetadataMap.get(cdsfName).toArray()[0];
                                                logger.info("values: existing: "+cdsfValue+", extracted: "+extractedValue);
                                                if (cdsfValue.equals(extractedValue)) {
                                                    matches++;
                                                }
                                            }
                                        }
                                        if (matches == nonEmptyFields) {
                                            alreadyExists = true; 
                                            break;
                                        }
                                    }
                                                                        
                                    if (!alreadyExists) {
                                        // save this compound value, by attaching it to the 
                                        // version for proper cascading:
                                        compoundDsfv.setParentDatasetField(dsf);
                                        dsf.getDatasetFieldCompoundValues().add(compoundDsfv);
                                    }
                                }
                            }
                        }
                    }
                } 
            }
        }  
    }
    
    
    private void processFileLevelMetadata(FileMetadataIngest fileLevelMetadata, FileMetadata fileMetadata) {
        // The only type of metadata that ingest plugins can extract from ingested
        // files (as of 4.0 beta) that *stay* on the file-level is the automatically
        // generated "metadata summary" note. We attach it to the "description" 
        // field of the fileMetadata object. -- L.A. 
        
        String metadataSummary = fileLevelMetadata.getMetadataSummary();
        if (metadataSummary != null) {
            if (!metadataSummary.equals("")) {
                // The file upload page allows a user to enter file description 
                // on ingest. We don't want to overwrite whatever they may 
                // have entered. Rather, we'll append this generated metadata summary 
                // to the existing value. 
                String userEnteredFileDescription = fileMetadata.getDescription();
                if (userEnteredFileDescription != null
                        && !(userEnteredFileDescription.equals(""))) {

                    metadataSummary = userEnteredFileDescription.concat("\n" + metadataSummary);
                }
                fileMetadata.setDescription(metadataSummary);
            }
        }
    }
    
    public void performPostProcessingTasks(DataFile dataFile) {
        /*
         * At this point (4.0 beta) the only ingest "post-processing task" performed 
         * is pre-generation of image thumbnails in a couple of popular sizes. 
         * -- L.A. 
         */
        if (dataFile != null) {
            // These separate methods for generating thumbnails, for PDF files and 
            // and for regular images, will eventually go away. We'll have a unified 
            // system of generating "previews" for datafiles of all kinds; the 
            // differentiation between different types of content and different 
            // methods for generating these previews will be hidden inside that 
            // subsystem (could be as simple as a type-specific icon, or even a 
            // special "content unknown" icon, for some types of files). 
            // -- L.A. 4.0 beta
            if ("application/pdf".equalsIgnoreCase(dataFile.getContentType())) {
                ImageThumbConverter.generatePDFThumb(dataFile.getFileSystemLocation().toString(), ImageThumbConverter.DEFAULT_THUMBNAIL_SIZE);
                ImageThumbConverter.generatePDFThumb(dataFile.getFileSystemLocation().toString(), ImageThumbConverter.DEFAULT_PREVIEW_SIZE);
            } else if (dataFile.isImage()) {
                ImageThumbConverter.generateImageThumb(dataFile.getFileSystemLocation().toString(), ImageThumbConverter.DEFAULT_THUMBNAIL_SIZE);
                ImageThumbConverter.generateImageThumb(dataFile.getFileSystemLocation().toString(), ImageThumbConverter.DEFAULT_PREVIEW_SIZE);
            }
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
