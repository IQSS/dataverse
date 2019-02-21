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
import edu.harvard.iq.dataverse.datavariable.VariableCategory;
import edu.harvard.iq.dataverse.datavariable.VariableServiceBean;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileCategory;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.DataTable;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetFieldValue;
import edu.harvard.iq.dataverse.DatasetFieldCompoundValue;
import edu.harvard.iq.dataverse.DatasetLock;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.MetadataBlock;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import edu.harvard.iq.dataverse.dataaccess.StorageIO;
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
import edu.harvard.iq.dataverse.ingest.tabulardata.impl.plugins.dta.NewDTAFileReader;
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
import edu.harvard.iq.dataverse.util.*;
//import edu.harvard.iq.dvn.unf.*;
import org.dataverse.unf.*;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.Arrays;
import java.util.Comparator;
import java.util.ListIterator;
import java.util.logging.Logger;
import java.util.Hashtable;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;
import javax.annotation.Resource;
import javax.ejb.Asynchronous;
import javax.jms.JMSException;
import javax.jms.QueueConnection;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Message;
import javax.faces.bean.ManagedBean;
import javax.faces.application.FacesMessage;

/**
 *
 * @author Leonid Andreev
 * dataverse 4.0
 * New service for handling ingest tasks
 * 
 */
@Stateless
@Named
@ManagedBean
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
    @EJB
    SystemConfig systemConfig;

    @Resource(mappedName = "jms/DataverseIngest")
    Queue queue;
    @Resource(mappedName = "jms/IngestQueueConnectionFactory")
    QueueConnectionFactory factory;
    

    private static String timeFormat_hmsS = "HH:mm:ss.SSS";
    private static String dateTimeFormat_ymdhmsS = "yyyy-MM-dd HH:mm:ss.SSS";
    private static String dateFormat_ymd = "yyyy-MM-dd";
    
    // This method tries to permanently store new files on the filesystem. 
    // Then it adds the files that *have been successfully saved* to the 
    // dataset (by attaching the DataFiles to the Dataset, and the corresponding
    // FileMetadatas to the DatasetVersion). It also tries to ensure that none 
    // of the parts of the DataFiles that failed to be saved (if any) are still
    // attached to the Dataset via some cascade path (for example, via 
    // DataFileCategory objects, if any were already assigned to the files). 
    // It must be called before we attempt to permanently save the files in 
    // the database by calling the Save command on the dataset and/or version. 
    public List<DataFile> saveAndAddFilesToDataset(DatasetVersion version, List<DataFile> newFiles) {
        List<DataFile> ret = new ArrayList<>();
        
        if (newFiles != null && newFiles.size() > 0) {
            //ret = new ArrayList<>();
            // final check for duplicate file names; 
            // we tried to make the file names unique on upload, but then 
            // the user may have edited them on the "add files" page, and 
            // renamed FOOBAR-1.txt back to FOOBAR.txt...

            IngestUtil.checkForDuplicateFileNamesFinal(version, newFiles);

            Dataset dataset = version.getDataset();

            for (DataFile dataFile : newFiles) {
                String tempFileLocation = FileUtil.getFilesTempDirectory() + "/" + dataFile.getStorageIdentifier();

                // Try to save the file in its permanent location: 
                String storageId = dataFile.getStorageIdentifier().replaceFirst("^tmp://", "");

                Path tempLocationPath = Paths.get(FileUtil.getFilesTempDirectory() + "/" + storageId);
                WritableByteChannel writeChannel = null;
                FileChannel readChannel = null;

                boolean unattached = false;
                boolean savedSuccess = false;
                StorageIO<DataFile> dataAccess = null;

                try {
                    logger.fine("Attempting to create a new storageIO object for " + storageId);
                    if (dataFile.getOwner() == null) {
                        unattached = true; 
                        dataFile.setOwner(dataset);
                    }
                    dataAccess = DataAccess.createNewStorageIO(dataFile, storageId);

                    logger.fine("Successfully created a new storageIO object.");
                    /* 
                         This commented-out code demonstrates how to copy bytes
                         from a local InputStream (or a readChannel) into the
                         writable byte channel of a Dataverse DataAccessIO object:
                     */
                    
                    /*
                        storageIO.open(DataAccessOption.WRITE_ACCESS);
                                                
                        writeChannel = storageIO.getWriteChannel();
                        readChannel = new FileInputStream(tempLocationPath.toFile()).getChannel();
                                                
                        long bytesPerIteration = 16 * 1024; // 16K bytes
                        long start = 0;
                        while ( start < readChannel.size() ) {
                            readChannel.transferTo(start, bytesPerIteration, writeChannel);
                            start += bytesPerIteration;
                        }
                     */

                    /* 
                            But it's easier to use this convenience method from the
                            DataAccessIO: 
                            
                            (if the underlying storage method for this file is 
                            local filesystem, the DataAccessIO will simply copy 
                            the file using Files.copy, like this:
                        
                            Files.copy(tempLocationPath, storageIO.getFileSystemLocation(), StandardCopyOption.REPLACE_EXISTING);
                     */
                    dataAccess.savePath(tempLocationPath);

                    // Set filesize in bytes
                    // 
                    dataFile.setFilesize(dataAccess.getSize());
                    savedSuccess = true;
                    logger.fine("Success: permanently saved file " + dataFile.getFileMetadata().getLabel());

                } catch (IOException ioex) {
                    logger.warning("Failed to save the file, storage id " + dataFile.getStorageIdentifier() + " (" + ioex.getMessage() + ")");
                } finally {
                    if (readChannel != null) {
                        try {
                            readChannel.close();
                        } catch (IOException e) {
                        }
                    }
                    if (writeChannel != null) {
                        try {
                            writeChannel.close();
                        } catch (IOException e) {
                        }
                    }
                }
                
                // Since we may have already spent some CPU cycles scaling down image thumbnails, 
                // we may as well save them, by moving these generated images to the permanent 
                // dataset directory. We should also remember to delete any such files in the
                // temp directory:
                List<Path> generatedTempFiles = listGeneratedTempFiles(Paths.get(FileUtil.getFilesTempDirectory()), storageId);
                if (generatedTempFiles != null) {
                    for (Path generated : generatedTempFiles) {
                        if (savedSuccess) { // no need to try to save this aux file permanently, if we've failed to save the main file!
                            logger.fine("(Will also try to permanently save generated thumbnail file " + generated.toString() + ")");
                            try {
                                //Files.copy(generated, Paths.get(dataset.getFileSystemDirectory().toString(), generated.getFileName().toString()));
                                int i = generated.toString().lastIndexOf("thumb");
                                if (i > 1) {
                                    String extensionTag = generated.toString().substring(i);
                                    dataAccess.savePathAsAux(generated, extensionTag);
                                    logger.fine("Saved generated thumbnail as aux object. \"preview available\" status: " + dataFile.isPreviewImageAvailable());
                                } else {
                                    logger.warning("Generated thumbnail file name does not match the expected pattern: " + generated.toString());
                                }

                            } catch (IOException ioex) {
                                logger.warning("Failed to save generated file " + generated.toString());
                            }
                        }

                        // ... but we definitely want to delete it:
                        try {
                            Files.delete(generated);
                        } catch (IOException ioex) {
                            logger.warning("Failed to delete generated file " + generated.toString());
                        }
                    }
                }

                // ... and let's delete the main temp file:
                try {
                    logger.fine("Will attempt to delete the temp file " + tempLocationPath.toString());
                    Files.delete(tempLocationPath);
                } catch (IOException ex) {
                    // (non-fatal - it's just a temp file.)
                    logger.warning("Failed to delete temp file " + tempLocationPath.toString());
                }

                if (unattached) {
                    dataFile.setOwner(null);
                }
                // Any necessary post-processing: 
                //performPostProcessingTasks(dataFile);
                
                if (savedSuccess) {
                    // These are all brand new files, so they should all have 
                    // one filemetadata total. -- L.A. 
                    FileMetadata fileMetadata = dataFile.getFileMetadatas().get(0);
                    String fileName = fileMetadata.getLabel();

                    boolean metadataExtracted = false;
                    if (FileUtil.canIngestAsTabular(dataFile)) {
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
                            logger.fine("Successfully extracted indexable metadata from file " + fileName);
                        } else {
                            logger.fine("Failed to extract indexable metadata from file " + fileName);
                        }
                    }
                    // temp dbug line
                    //System.out.println("ADDING FILE: " + fileName + "; for dataset: " + dataset.getGlobalId());                    
                    // Make sure the file is attached to the dataset and to the version, if this 
                    // hasn't been done yet:
                    if (dataFile.getOwner() == null) {
                        dataFile.setOwner(dataset);

                        version.getFileMetadatas().add(dataFile.getFileMetadata());
                        dataFile.getFileMetadata().setDatasetVersion(version);
                        dataset.getFiles().add(dataFile);
                        
                        if (dataFile.getFileMetadata().getCategories() != null) {
                            ListIterator<DataFileCategory> dfcIt = dataFile.getFileMetadata().getCategories().listIterator();

                            while (dfcIt.hasNext()) {
                                DataFileCategory dataFileCategory = dfcIt.next();
                                
                                if (dataFileCategory.getDataset() == null) {
                                    DataFileCategory newCategory = dataset.getCategoryByName(dataFileCategory.getName());
                                    if (newCategory != null) {
                                        newCategory.addFileMetadata(dataFile.getFileMetadata());
                                        //dataFileCategory = newCategory;
                                        dfcIt.set(newCategory);
                                    } else { 
                                        dfcIt.remove();
                                    }
                                }
                            }
                        }
                    }
                    
                    ret.add(dataFile);
                }
            }

            logger.fine("Done! Finished saving new files in permanent storage and adding them to the dataset.");
        }
        
        return ret;
    }
    
    public List<Path> listGeneratedTempFiles(Path tempDirectory, String baseName) {
        List<Path> generatedFiles = new ArrayList<>();

        // for example, <filename>.thumb64 or <filename>.thumb400.

        if (baseName == null || baseName.equals("")) {
            return null;
        }

        DirectoryStream.Filter<Path> filter = new DirectoryStream.Filter<Path>() {
            @Override
            public boolean accept(Path file) throws IOException {
                return (file.getFileName() != null
                        && file.getFileName().toString().startsWith(baseName + ".thumb"));
            }
        };

        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(tempDirectory, filter)) {
            for (Path filePath : dirStream) {
                generatedFiles.add(filePath);
            }
        } catch (IOException ex) {
        }

        return generatedFiles;
    }
    
    
   
    
    // TODO: consider creating a version of this method that would take 
    // datasetversion as the argument. 
    // -- L.A. 4.6
    public void startIngestJobsForDataset(Dataset dataset, AuthenticatedUser user) {
        List<DataFile> scheduledFiles = new ArrayList<>();
                
        for (DataFile dataFile : dataset.getFiles()) {
            if (dataFile.isIngestScheduled()) {
                // todo: investigate why when calling save with the file object
                // gotten from the loop, the roles assignment added at create is removed
                // (switching to refinding via id resolves that)                
                dataFile = fileService.find(dataFile.getId());
                scheduledFiles.add(dataFile);
            }
        }

        startIngestJobs(scheduledFiles, user);
    }
    
    public String startIngestJobs(List<DataFile> dataFiles, AuthenticatedUser user) {

        IngestMessage ingestMessage = null;
        StringBuilder sb = new StringBuilder();

        List<DataFile> scheduledFiles = new ArrayList<>();
        for (DataFile dataFile : dataFiles) {
            if (dataFile.isIngestScheduled()) {

                // refresh the copy of the DataFile:
                dataFile = fileService.find(dataFile.getId());

                long ingestSizeLimit = -1;
                try {
                    ingestSizeLimit = systemConfig.getTabularIngestSizeLimit(getTabDataReaderByMimeType(dataFile.getContentType()).getFormatName());
                } catch (IOException ioex) {
                    logger.warning("IO Exception trying to retrieve the ingestable format identifier from the plugin for type " + dataFile.getContentType() + " (non-fatal);");
                }

                if (ingestSizeLimit == -1 || dataFile.getFilesize() < ingestSizeLimit) {
                    dataFile.SetIngestInProgress();
                    scheduledFiles.add(dataFile);
                } else {
                    dataFile.setIngestDone();
                    String message = "Skipping tabular ingest of the file " + dataFile.getFileMetadata().getLabel() + ", because of the size limit (set to " + ingestSizeLimit + " bytes); ";
                    logger.info(message);
                    sb.append(message);
                }
                dataFile = fileService.save(dataFile);
            } else {
                String message = "(Re)ingest queueing request submitted on a file not scheduled for ingest! (" + dataFile.getFileMetadata().getLabel() + "); ";
                logger.warning(message);
                sb.append(message);
            }
        }

        int count = scheduledFiles.size();
        
        if (count > 0) {
            String info = "Ingest of " + count + " tabular data file(s) is in progress.";
            logger.info(info);
            datasetService.addDatasetLock(scheduledFiles.get(0).getOwner().getId(),
                    DatasetLock.Reason.Ingest,
                    (user != null) ? user.getId() : null,
                    info);
            
            // Sort ingest jobs by file size: 
            DataFile[] scheduledFilesArray = (DataFile[])scheduledFiles.toArray(new DataFile[count]);
            scheduledFiles = null; 
            
            Arrays.sort(scheduledFilesArray, new Comparator<DataFile>() {
                @Override
                public int compare(DataFile d1, DataFile d2) {
                    long a = d1.getFilesize();
                    long b = d2.getFilesize();
                    return Long.valueOf(a).compareTo(b);
                }
            });

            ingestMessage = new IngestMessage(IngestMessage.INGEST_MESAGE_LEVEL_INFO);
            for (int i = 0; i < count; i++) {
                ingestMessage.addFileId(scheduledFilesArray[i].getId());
            }

            QueueConnection conn = null;
            QueueSession session = null;
            QueueSender sender = null;

            try {
                conn = factory.createQueueConnection();
                session = conn.createQueueSession(false, 0);
                sender = session.createSender(queue);

                Message queueMessage = session.createObjectMessage(ingestMessage);

                sender.send(queueMessage);

            } catch (JMSException ex) {
                ex.printStackTrace();
                logger.warning("Caught exception trying to close connections after starting a (re)ingest job in the JMS queue! Stack trace below.");
                sb.append("Failed to queue the (re)ingest job for DataFile (JMS Exception)" + (ex.getMessage() != null ? ex.getMessage() : ""));
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
                } catch (Exception ex) {
                    logger.warning("Caught exception trying to close connections after starting a (re)ingest job in the JMS queue! Stack trace below.");
                    ex.printStackTrace();
                }
            }
        }
        
        return sb.toString();
    }

    
    public void produceSummaryStatistics(DataFile dataFile, File generatedTabularFile) throws IOException {
        /*
        logger.info("Skipping summary statistics and UNF.");
         */
        produceDiscreteNumericSummaryStatistics(dataFile, generatedTabularFile); 
        produceContinuousSummaryStatistics(dataFile, generatedTabularFile);
        produceCharacterSummaryStatistics(dataFile, generatedTabularFile);
        
        recalculateDataFileUNF(dataFile);
        recalculateDatasetVersionUNF(dataFile.getFileMetadata().getDatasetVersion());
    }
    
    public void produceContinuousSummaryStatistics(DataFile dataFile, File generatedTabularFile) throws IOException {

        /* 
        // quick, but memory-inefficient way:
        // - this method just loads the entire file-worth of continuous vectors 
        // into a Double[][] matrix. 
        //Double[][] variableVectors = subsetContinuousVectors(dataFile);
        //calculateContinuousSummaryStatistics(dataFile, variableVectors);
        
        // A more sophisticated way: this subsets one column at a time, using 
        // the new optimized subsetting that does not have to read any extra 
        // bytes from the file to extract the column:
        
        TabularSubsetGenerator subsetGenerator = new TabularSubsetGenerator();
        */
        
        for (int i = 0; i < dataFile.getDataTable().getVarQuantity(); i++) {
            if (dataFile.getDataTable().getDataVariables().get(i).isIntervalContinuous()) {
                logger.fine("subsetting continuous vector");

                if ("float".equals(dataFile.getDataTable().getDataVariables().get(i).getFormat())) {
                    Float[] variableVector = TabularSubsetGenerator.subsetFloatVector(new FileInputStream(generatedTabularFile), i, dataFile.getDataTable().getCaseQuantity().intValue());
                    logger.fine("Calculating summary statistics on a Float vector;");
                    calculateContinuousSummaryStatistics(dataFile, i, variableVector);
                    // calculate the UNF while we are at it:
                    logger.fine("Calculating UNF on a Float vector;");
                    calculateUNF(dataFile, i, variableVector);
                    variableVector = null; 
                } else {
                    Double[] variableVector = TabularSubsetGenerator.subsetDoubleVector(new FileInputStream(generatedTabularFile), i, dataFile.getDataTable().getCaseQuantity().intValue());
                    logger.fine("Calculating summary statistics on a Double vector;");
                    calculateContinuousSummaryStatistics(dataFile, i, variableVector);
                    // calculate the UNF while we are at it:
                    logger.fine("Calculating UNF on a Double vector;");
                    calculateUNF(dataFile, i, variableVector);
                    variableVector = null; 
                }
                logger.fine("Done! (continuous);");
            }
        }
    }
    
    public void produceDiscreteNumericSummaryStatistics(DataFile dataFile, File generatedTabularFile) throws IOException {
        
        //TabularSubsetGenerator subsetGenerator = new TabularSubsetGenerator();
        
        for (int i = 0; i < dataFile.getDataTable().getVarQuantity(); i++) {
            if (dataFile.getDataTable().getDataVariables().get(i).isIntervalDiscrete()
                    && dataFile.getDataTable().getDataVariables().get(i).isTypeNumeric()) {
                logger.fine("subsetting discrete-numeric vector");

                Long[] variableVector = TabularSubsetGenerator.subsetLongVector(new FileInputStream(generatedTabularFile), i, dataFile.getDataTable().getCaseQuantity().intValue());
                // We are discussing calculating the same summary stats for 
                // all numerics (the same kind of sumstats that we've been calculating
                // for numeric continuous type)  -- L.A. Jul. 2014
                calculateContinuousSummaryStatistics(dataFile, i, variableVector);
                // calculate the UNF while we are at it:
                logger.fine("Calculating UNF on a Long vector");
                calculateUNF(dataFile, i, variableVector);
                logger.fine("Done! (discrete numeric)");
                variableVector = null; 
            }
        }
    }
    
    public void produceCharacterSummaryStatistics(DataFile dataFile, File generatedTabularFile) throws IOException {

        /* 
            At this point it's still not clear what kinds of summary stats we
            want for character types. Though we are pretty confident we don't 
            want to keep doing what we used to do in the past, i.e. simply 
            store the total counts for all the unique values; even if it's a 
            very long vector, and *every* value in it is unique. (As a result 
            of this, our Categorical Variable Value table is the single 
            largest in the production database. With no evidence whatsoever, 
            that this information is at all useful. 
                -- L.A. Jul. 2014 
        
        TabularSubsetGenerator subsetGenerator = new TabularSubsetGenerator();
        */
        
        for (int i = 0; i < dataFile.getDataTable().getVarQuantity(); i++) {
            if (dataFile.getDataTable().getDataVariables().get(i).isTypeCharacter()) {

                logger.fine("subsetting character vector");
                String[] variableVector = TabularSubsetGenerator.subsetStringVector(new FileInputStream(generatedTabularFile), i, dataFile.getDataTable().getCaseQuantity().intValue());
                //calculateCharacterSummaryStatistics(dataFile, i, variableVector);
                // calculate the UNF while we are at it:
                logger.fine("Calculating UNF on a String vector");
                calculateUNF(dataFile, i, variableVector);
                logger.fine("Done! (character)");
                variableVector = null; 
            }
        }
    }

    public static void produceFrequencyStatistics(DataFile dataFile, File generatedTabularFile) throws IOException {

        List<DataVariable> vars = dataFile.getDataTable().getDataVariables();

        produceFrequencies(generatedTabularFile, vars);
    }

    public static void produceFrequencies( File generatedTabularFile, List<DataVariable> vars) throws IOException {

        for (int i = 0; i < vars.size(); i++) {

            Collection<VariableCategory> cats = vars.get(i).getCategories();
            int caseQuantity = vars.get(i).getDataTable().getCaseQuantity().intValue();
            boolean isNumeric = vars.get(i).isTypeNumeric();
            Object[] variableVector = null;
            if (cats.size() > 0) {
                if (isNumeric) {
                    variableVector = TabularSubsetGenerator.subsetFloatVector(new FileInputStream(generatedTabularFile), i, caseQuantity);
                }
                else {
                    variableVector = TabularSubsetGenerator.subsetStringVector(new FileInputStream(generatedTabularFile), i, caseQuantity);
                }
                if (variableVector != null) {
                    Hashtable<Object, Double> freq = calculateFrequency(variableVector);
                    for (VariableCategory cat : cats) {
                        Object catValue;
                        if (isNumeric) {
                            catValue = new Float(cat.getValue());
                        } else {
                            catValue = cat.getValue();
                        }
                        Double numberFreq = freq.get(catValue);
                        if (numberFreq != null) {
                            cat.setFrequency(numberFreq);
                        } else {
                            cat.setFrequency(0D);
                        }
                    }
                } else {
                    logger.fine("variableVector is null for variable " + vars.get(i).getName());
                }
            }
        }
    }

    public static Hashtable<Object, Double> calculateFrequency( Object[] variableVector) {
        Hashtable<Object, Double> freq = new Hashtable<Object, Double>();

        for (int j = 0; j < variableVector.length; j++) {
            if (variableVector[j] != null) {
                Double freqNum = freq.get(variableVector[j]);
                if (freqNum != null) {
                    freq.put(variableVector[j], freqNum + 1);
                } else {
                    freq.put(variableVector[j], 1D);
                }
            }
        }

        return freq;

    }
    
    public void recalculateDataFileUNF(DataFile dataFile) {
        String[] unfValues = new String[dataFile.getDataTable().getVarQuantity().intValue()];
        String fileUnfValue = null; 
        
        for (int i = 0; i < dataFile.getDataTable().getVarQuantity(); i++) {
            String varunf = dataFile.getDataTable().getDataVariables().get(i).getUnf();
            unfValues[i] = varunf; 
        }
        
        try {
            fileUnfValue = UNFUtil.calculateUNF(unfValues);
        } catch (IOException ex) {
            logger.warning("Failed to recalculate the UNF for the datafile id="+dataFile.getId());
        } catch (UnfException uex) {
                logger.warning("UNF Exception: Failed to recalculate the UNF for the dataset version id="+dataFile.getId());
        }
        
        if (fileUnfValue != null) {
            dataFile.getDataTable().setUnf(fileUnfValue);
        }
    }

    public void recalculateDatasetVersionUNF(DatasetVersion version) {
        IngestUtil.recalculateDatasetVersionUNF(version);
    }

    public void sendFailNotification(Long dataset_id) {
        FacesMessage facesMessage = new FacesMessage(BundleUtil.getStringFromBundle("ingest.failed"));
        /* commented out push channel message:
            PushContext pushContext = PushContextFactory.getDefault().getPushContext();
            pushContext.push("/ingest" + dataset_id, facesMessage);
        */
    }
    
    
    public boolean ingestAsTabular(Long datafile_id) {
        DataFile dataFile = fileService.find(datafile_id);
        boolean ingestSuccessful = false;
        boolean forceTypeCheck = false;
        
        IngestRequest ingestRequest = dataFile.getIngestRequest();
        if (ingestRequest != null) {
            forceTypeCheck = ingestRequest.isForceTypeCheck();
        }

        // Locate ingest plugin for the file format by looking
        // it up with the Ingest Service Provider Registry:
        String fileName = dataFile.getFileMetadata().getLabel();
        TabularDataFileReader ingestPlugin = getTabDataReaderByMimeType(dataFile.getContentType());
        logger.fine("Found ingest plugin " + ingestPlugin.getClass());
        
        if (!forceTypeCheck && ingestPlugin == null) {
            // If this is a reingest request, we'll still have a chance
            // to find an ingest plugin for this file, once we try
            // to identify the file type again.
            // Otherwise, we can give up - there is no point in proceeding to 
            // the next step if no ingest plugin is available. 
            
            dataFile.SetIngestProblem();
            FileUtil.createIngestFailureReport(dataFile, "No ingest plugin found for file type "+dataFile.getContentType());
            dataFile = fileService.save(dataFile);
            logger.warning("Ingest failure.");
            return false; 
        }

        BufferedInputStream inputStream = null; 
        File additionalData = null;
        File localFile = null;
        StorageIO<DataFile> storageIO = null;
                
        try {
            storageIO = dataFile.getStorageIO();
            storageIO.open();
             
            if (storageIO.isLocalFile()) {
                localFile = storageIO.getFileSystemPath().toFile();
                inputStream = new BufferedInputStream(storageIO.getInputStream());
            } else {
                ReadableByteChannel dataFileChannel = storageIO.getReadChannel();
                localFile = File.createTempFile("tempIngestSourceFile", ".tmp");
                FileChannel tempIngestSourceChannel = new FileOutputStream(localFile).getChannel();

                tempIngestSourceChannel.transferFrom(dataFileChannel, 0, storageIO.getSize());
                
                inputStream = new BufferedInputStream(new FileInputStream(localFile));
                logger.fine("Saved "+storageIO.getSize()+" bytes in a local temp file.");
            }
        } catch (IOException ioEx) {
            dataFile.SetIngestProblem();
            
            FileUtil.createIngestFailureReport(dataFile, "IO Exception occured while trying to open the file for reading.");
            dataFile = fileService.save(dataFile);
            
            logger.warning("Ingest failure (No file produced).");
            return false; 
        }
        
        if (ingestRequest != null) {
            if (ingestRequest.getTextEncoding() != null 
                    && !ingestRequest.getTextEncoding().equals("") ) {
                logger.fine("Setting language encoding to "+ingestRequest.getTextEncoding());
                ingestPlugin.setDataLanguageEncoding(ingestRequest.getTextEncoding());
            }
            if (ingestRequest.getLabelsFile() != null) {
                additionalData = new File(ingestRequest.getLabelsFile());
            }
        }
        
        if (forceTypeCheck) {
            String newType = FileUtil.retestIngestableFileType(localFile, dataFile.getContentType());
            
            ingestPlugin = getTabDataReaderByMimeType(newType);
            logger.fine("Re-tested file type: " + newType + "; Using ingest plugin " + ingestPlugin.getClass());

            // check again:
            if (ingestPlugin == null) {
                // If it's still null - give up!
            
                dataFile.SetIngestProblem();
                FileUtil.createIngestFailureReport(dataFile, "No ingest plugin found for file type "+dataFile.getContentType());
                dataFile = fileService.save(dataFile);
                logger.warning("Ingest failure: failed to detect ingest plugin (file type check forced)");
                return false; 
            }
            
            dataFile.setContentType(newType);
        }
        
        TabularDataIngest tabDataIngest = null; 
        try {
            if (additionalData != null) {
                tabDataIngest = ingestPlugin.read(inputStream, additionalData);
            } else {
                tabDataIngest = ingestPlugin.read(inputStream, null);
            }
        } catch (IOException ingestEx) {
            dataFile.SetIngestProblem();
            FileUtil.createIngestFailureReport(dataFile, ingestEx.getMessage());
            dataFile = fileService.save(dataFile);
            
            logger.warning("Ingest failure (IO Exception): " + ingestEx.getMessage() + ".");
            return false;
        } catch (Exception unknownEx) {
            dataFile.SetIngestProblem();
            FileUtil.createIngestFailureReport(dataFile, unknownEx.getMessage());
            dataFile = fileService.save(dataFile);
            
            logger.warning("Ingest failure (Exception " + unknownEx.getClass() + "): "+unknownEx.getMessage()+".");
            return false;
            
        }

        String originalContentType = dataFile.getContentType();
        String originalFileName = dataFile.getFileMetadata().getLabel();
        long originalFileSize = dataFile.getFilesize();
        boolean postIngestTasksSuccessful = false;
        boolean databaseSaveSuccessful = false;

        if (tabDataIngest != null) {
            File tabFile = tabDataIngest.getTabDelimitedFile();

            if (tabDataIngest.getDataTable() != null
                    && tabFile != null
                    && tabFile.exists()) {
                logger.info("Tabular data successfully ingested; DataTable with "
                        + tabDataIngest.getDataTable().getVarQuantity() + " variables produced.");
                logger.info("Tab-delimited file produced: " + tabFile.getAbsolutePath());

                dataFile.setFilesize(tabFile.length());

                // and change the mime type to "Tabular Data" on the final datafile, 
                // and replace (or add) the extension ".tab" to the filename: 
                dataFile.setContentType(FileUtil.MIME_TYPE_INGESTED_FILE);
                IngestUtil.modifyExistingFilename(dataFile.getOwner().getLatestVersion(), dataFile.getFileMetadata(), FileUtil.replaceExtension(fileName, "tab"));

                if (FileUtil.MIME_TYPE_CSV_ALT.equals(dataFile.getContentType())) {
                    tabDataIngest.getDataTable().setOriginalFileFormat(FileUtil.MIME_TYPE_CSV);
                } else {
                    tabDataIngest.getDataTable().setOriginalFileFormat(originalContentType);
                }
                tabDataIngest.getDataTable().setOriginalFileSize(originalFileSize);

                dataFile.setDataTable(tabDataIngest.getDataTable());
                tabDataIngest.getDataTable().setDataFile(dataFile);

                try {
                    produceSummaryStatistics(dataFile, tabFile);
                    produceFrequencyStatistics(dataFile, tabFile);
                    postIngestTasksSuccessful = true;
                } catch (IOException postIngestEx) {

                    dataFile.SetIngestProblem();
                    FileUtil.createIngestFailureReport(dataFile, "Ingest failed to produce Summary Statistics and/or UNF signatures; " + postIngestEx.getMessage());

                    restoreIngestedDataFile(dataFile, tabDataIngest, originalFileSize, originalFileName, originalContentType);
                    dataFile = fileService.save(dataFile);

                    logger.warning("Ingest failure: post-ingest tasks.");
                }

                if (!postIngestTasksSuccessful) {
                    logger.warning("Ingest failure (!postIngestTasksSuccessful).");
                    return false;
                }

                dataFile.setIngestDone();
                // delete the ingest request, if exists:
                if (dataFile.getIngestRequest() != null) {
                    dataFile.getIngestRequest().setDataFile(null);
                    dataFile.setIngestRequest(null);
                }

                try {
                    /* 
                         In order to test a database save failure, uncomment this:
                        
                        if (true) {
                            throw new EJBException("Deliberate database save failure");
                        }
                     */
                    dataFile = fileService.save(dataFile);
                    databaseSaveSuccessful = true;

                    logger.fine("Ingest (" + dataFile.getFileMetadata().getLabel() + ".");

                    if (additionalData != null) {
                        // remove the extra tempfile, if there was one:
                        additionalData.delete();
                    }
                } catch (Exception unknownEx) {
                    // this means that an error occurred while saving the datafile
                    // in the database. 
                    logger.warning("Ingest failure: Failed to save tabular metadata (datatable, datavariables, etc.) in the database. Clearing the datafile object.");

                    dataFile = fileService.find(datafile_id);

                    if (dataFile != null) {
                        dataFile.SetIngestProblem();
                        FileUtil.createIngestFailureReport(dataFile, "Ingest produced tabular data, but failed to save it in the database; " + unknownEx.getMessage() + " No further information is available.");

                        restoreIngestedDataFile(dataFile, tabDataIngest, originalFileSize, originalFileName, originalContentType);

                        dataFile = fileService.save(dataFile);
                    }
                }

                if (!databaseSaveSuccessful) {
                    logger.warning("Ingest failure (!databaseSaveSuccessful).");
                    return false;
                }

                // Finally, let's swap the original and the tabular files: 
                try {
                    /* Start of save as backup */

                    StorageIO<DataFile> dataAccess = dataFile.getStorageIO();
                    dataAccess.open();

                    // and we want to save the original of the ingested file: 
                    try {
                        dataAccess.backupAsAux(FileUtil.SAVED_ORIGINAL_FILENAME_EXTENSION);
                        logger.fine("Saved the ingested original as a backup aux file "+FileUtil.SAVED_ORIGINAL_FILENAME_EXTENSION);
                    } catch (IOException iox) {
                        logger.warning("Failed to save the ingested original! " + iox.getMessage());
                    }

                    // Replace contents of the file with the tab-delimited data produced:
                    dataAccess.savePath(Paths.get(tabFile.getAbsolutePath()));
                    // Reset the file size: 
                    dataFile.setFilesize(dataAccess.getSize());

                    // delete the temp tab-file:
                    tabFile.delete();
                    /*end of save as backup */

                } catch (Exception e) {
                    // this probably means that an error occurred while saving the file to the file system
                    logger.warning("Failed to save the tabular file produced by the ingest (resetting the ingested DataFile back to its original state)");

                    dataFile = fileService.find(datafile_id);

                    if (dataFile != null) {
                        dataFile.SetIngestProblem();
                        FileUtil.createIngestFailureReport(dataFile, "Failed to save the tabular file produced by the ingest.");

                        restoreIngestedDataFile(dataFile, tabDataIngest, originalFileSize, originalFileName, originalContentType);

                        dataFile = fileService.save(dataFile);
                    }
                }

                ingestSuccessful = true;
            }
        } else {
            logger.warning("Ingest failed to produce data obect.");
        }

        return ingestSuccessful;
    }

    private BufferedInputStream openFile(DataFile dataFile) throws IOException {
        BufferedInputStream inputStream;
        StorageIO<DataFile> storageIO = dataFile.getStorageIO();
        storageIO.open();
        if (storageIO.isLocalFile()) {
            inputStream = new BufferedInputStream(storageIO.getInputStream());
        } else {
            ReadableByteChannel dataFileChannel = storageIO.getReadChannel();
            File tempFile = File.createTempFile("tempIngestSourceFile", ".tmp");
            FileChannel tempIngestSourceChannel = new FileOutputStream(tempFile).getChannel();
            
            tempIngestSourceChannel.transferFrom(dataFileChannel, 0, storageIO.getSize());
            
            inputStream = new BufferedInputStream(new FileInputStream(tempFile));
            logger.fine("Saved "+storageIO.getSize()+" bytes in a local temp file.");
        }
        return inputStream;
    }

    private void restoreIngestedDataFile(DataFile dataFile, TabularDataIngest tabDataIngest, long originalSize, String originalFileName, String originalContentType) {
        dataFile.setDataTables(null);
        if (tabDataIngest != null && tabDataIngest.getDataTable() != null) {
            tabDataIngest.getDataTable().setDataFile(null);
        }
        dataFile.getFileMetadata().setLabel(originalFileName);
        dataFile.setContentType(originalContentType);
        dataFile.setFilesize(originalSize);
    }
    
    // TODO: Further move the code that doesn't really need to be in an EJB bean
    // (i.e., the code that doesn't need to persist anything in the database) into
    // outside static utilities and/or helpers; so that unit tests could be written
    // easily. -- L.A. 4.6
    
    /* not needed anymore, but keeping it around, as a demo of how Push 
       notifications work
    private void sendStatusNotification(Long datasetId, FacesMessage message) {*/
        /*
        logger.fine("attempting to send push notification to channel /ingest/dataset/"+datasetId+"; "+message.getDetail());
        EventBus eventBus = EventBusFactory.getDefault().eventBus();
        if (eventBus == null) {
            logger.warning("Failed to obtain eventBus!");
            return;
        }
        // TODO: 
        // add more diagnostics here! 4.2.3 -- L.A. 
        eventBus.publish("/ingest/dataset/" + datasetId, message);
        */
   /* }*/
    
    public static TabularDataFileReader getTabDataReaderByMimeType(String mimeType) { //DataFile dataFile) {
        /* 
         * Same as the comment above; since we don't have any ingest plugins loadable 
         * in real times yet, we can select them by a fixed list of mime types. 
         * -- L.A. 4.0 beta.
         */

        //String mimeType = dataFile.getContentType();
        
        if (mimeType == null) {
            return null;
        }

        TabularDataFileReader ingestPlugin = null;

        if (mimeType.equals(FileUtil.MIME_TYPE_STATA)) {
            ingestPlugin = new DTAFileReader(new DTAFileReaderSpi());
        } else if (mimeType.equals(FileUtil.MIME_TYPE_STATA13)) {
            ingestPlugin = new NewDTAFileReader(new DTAFileReaderSpi(), 117);
        } else if (mimeType.equals(FileUtil.MIME_TYPE_STATA14)) {
            ingestPlugin = new NewDTAFileReader(new DTAFileReaderSpi(), 118);
        } else if (mimeType.equals(FileUtil.MIME_TYPE_STATA15)) {
            ingestPlugin = new NewDTAFileReader(new DTAFileReaderSpi(), 119);
        } else if (mimeType.equals(FileUtil.MIME_TYPE_RDATA)) {
            ingestPlugin = new RDATAFileReader(new RDATAFileReaderSpi());
        } else if (mimeType.equals(FileUtil.MIME_TYPE_CSV) || mimeType.equals(FileUtil.MIME_TYPE_CSV_ALT)) {
            ingestPlugin = new CSVFileReader(new CSVFileReaderSpi(), ',');
        } else if (mimeType.equals(FileUtil.MIME_TYPE_TSV) || mimeType.equals(FileUtil.MIME_TYPE_TSV_ALT)) {
            ingestPlugin = new CSVFileReader(new CSVFileReaderSpi(), '\t');
        }  else if (mimeType.equals(FileUtil.MIME_TYPE_XLSX)) {
            ingestPlugin = new XLSXFileReader(new XLSXFileReaderSpi());
        } else if (mimeType.equals(FileUtil.MIME_TYPE_SPSS_SAV)) {
            ingestPlugin = new SAVFileReader(new SAVFileReaderSpi());
        } else if (mimeType.equals(FileUtil.MIME_TYPE_SPSS_POR)) {
            ingestPlugin = new PORFileReader(new PORFileReaderSpi());
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
        if (dataFile.getContentType() != null && dataFile.getContentType().equals(FileUtil.MIME_TYPE_FITS)) {
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

                                        // Special rules apply to aggregation of values for 
                                        // some specific fields - namely, the resolution.* 
                                        // fields from the Astronomy Metadata block. 
                                        // TODO: rather than hard-coded, this needs to be
                                        // programmatically defined. -- L.A. 4.0
                                        if (dsfName.equals("resolution.Temporal")
                                                || dsfName.equals("resolution.Spatial")
                                                || dsfName.equals("resolution.Spectral")) {
                                            // For these values, we aggregate the minimum-maximum 
                                            // pair, for the entire set. 
                                            // So first, we need to go through the values found by 
                                            // the plugin and select the min. and max. values of 
                                            // these: 
                                            // (note that we are assuming that they all must
                                            // validate as doubles!)

                                            Double minValue = null;
                                            Double maxValue = null;

                                            for (String fValue : mValues) {

                                                try {
                                                    double thisValue = Double.parseDouble(fValue);

                                                    if (minValue == null || Double.compare(thisValue, minValue) < 0) {
                                                        minValue = thisValue;
                                                    }
                                                    if (maxValue == null || Double.compare(thisValue, maxValue) > 0) {
                                                        maxValue = thisValue;
                                                    }
                                                } catch (NumberFormatException e) {
                                                }
                                            }

                                            // Now let's see what aggregated values we 
                                            // have stored already: 
                                            
                                            // (all of these resolution.* fields have allowedMultiple set to FALSE, 
                                            // so there can be only one!)
                                            //logger.fine("Min value: "+minValue+", Max value: "+maxValue);
                                            if (minValue != null && maxValue != null) {
                                                Double storedMinValue = null; 
                                                Double storedMaxValue = null;
                                            
                                                String storedValue = "";
                                                
                                                if (dsf.getDatasetFieldValues() != null && dsf.getDatasetFieldValues().get(0) != null) {
                                                    storedValue = dsf.getDatasetFieldValues().get(0).getValue();
                                                
                                                    if (storedValue != null && !storedValue.equals("")) {
                                                        try {

                                                            if (storedValue.indexOf(" - ") > -1) {
                                                                storedMinValue = Double.parseDouble(storedValue.substring(0, storedValue.indexOf(" - ")));
                                                                storedMaxValue = Double.parseDouble(storedValue.substring(storedValue.indexOf(" - ") + 3));
                                                            } else {
                                                                storedMinValue = Double.parseDouble(storedValue);
                                                                storedMaxValue = storedMinValue;
                                                            }
                                                            if (storedMinValue != null && storedMinValue.compareTo(minValue) < 0) {
                                                                minValue = storedMinValue;
                                                            }
                                                            if (storedMaxValue != null && storedMaxValue.compareTo(maxValue) > 0) {
                                                                maxValue = storedMaxValue;
                                                            }
                                                        } catch (NumberFormatException e) {}
                                                    } else {
                                                        storedValue = "";
                                                    }
                                                }
                                            
                                                //logger.fine("Stored min value: "+storedMinValue+", Stored max value: "+storedMaxValue);
                                                
                                                String newAggregateValue = "";
                                                
                                                if (minValue.equals(maxValue)) {
                                                    newAggregateValue = minValue.toString();
                                                } else {
                                                    newAggregateValue = minValue.toString() + " - " + maxValue.toString();
                                                }
                                                
                                                // finally, compare it to the value we have now:
                                                if (!storedValue.equals(newAggregateValue)) {
                                                    if (dsf.getDatasetFieldValues() == null) {
                                                        dsf.setDatasetFieldValues(new ArrayList<DatasetFieldValue>());
                                                    }
                                                    if (dsf.getDatasetFieldValues().get(0) == null) {
                                                        DatasetFieldValue newDsfv = new DatasetFieldValue(dsf);
                                                        dsf.getDatasetFieldValues().add(newDsfv);
                                                    }
                                                    dsf.getDatasetFieldValues().get(0).setValue(newAggregateValue);
                                                }
                                            }
                                            // Ouch. 
                                        } else {
                                            // Other fields are aggregated simply by 
                                            // collecting a list of *unique* values encountered 
                                            // for this Field throughout the dataset. 
                                            // This means we need to only add the values *not yet present*.
                                            // (the implementation below may be inefficient - ?)

                                            for (String fValue : mValues) {
                                                if (!dsft.isControlledVocabulary()) {
                                                    Iterator<DatasetFieldValue> dsfvIt = dsf.getDatasetFieldValues().iterator();

                                                    boolean valueExists = false;

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
                                                logger.fine("values: existing: "+cdsfValue+", extracted: "+extractedValue);
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

                    metadataSummary = userEnteredFileDescription.concat(";\n" + metadataSummary);
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
        if (dataFile != null && dataFile.isImage()) {
            try {
                StorageIO<DataFile> dataAccess = dataFile.getStorageIO();
                if (dataAccess != null) { // && storageIO.isLocalFile()) {

                    if (ImageThumbConverter.isThumbnailAvailable(dataFile, ImageThumbConverter.DEFAULT_PREVIEW_SIZE)) {
                        dataFile.setPreviewImageAvailable(true);
                    }
                }
            } catch (IOException ioEx) {
            }
        }
    }
 
    private Set<Integer> selectContinuousVariableColumns(DataFile dataFile) {
        Set<Integer> contVarFields = new LinkedHashSet<Integer>();

        for (int i = 0; i < dataFile.getDataTable().getVarQuantity(); i++) {
            if (dataFile.getDataTable().getDataVariables().get(i).isIntervalContinuous()) {
                contVarFields.add(i);
            }
        }

        return contVarFields;
    }
    
    private void calculateContinuousSummaryStatistics(DataFile dataFile, int varnum, Number[] dataVector) throws IOException {
        double[] sumStats = SumStatCalculator.calculateSummaryStatistics(dataVector);
        assignContinuousSummaryStatistics(dataFile.getDataTable().getDataVariables().get(varnum), sumStats);
    }
    
    private void assignContinuousSummaryStatistics(DataVariable variable, double[] sumStats) throws IOException {
        if (sumStats == null || sumStats.length != variableService.summaryStatisticTypes.length) {
            throw new IOException ("Wrong number of summary statistics types calculated! ("+sumStats.length+")");
        }
        
        for (int j = 0; j < variableService.summaryStatisticTypes.length; j++) {
            SummaryStatistic ss = new SummaryStatistic();
            ss.setTypeByLabel(variableService.summaryStatisticTypes[j]);
            if (!ss.isTypeMode()) {
                ss.setValue((new Double(sumStats[j])).toString());
            } else {
                ss.setValue(".");
            }
            ss.setDataVariable(variable);
            variable.getSummaryStatistics().add(ss);
        }

    }
    
    private void calculateUNF(DataFile dataFile, int varnum, Double[] dataVector) {
        String unf = null;
        try {
            unf = UNFUtil.calculateUNF(dataVector);
        } catch (IOException iex) {
            logger.warning("exception thrown when attempted to calculate UNF signature for (numeric, continuous) variable " + varnum);
        } catch (UnfException uex) {
            logger.warning("UNF Exception: thrown when attempted to calculate UNF signature for (numeric, continuous) variable " + varnum);
        }
        
        if (unf != null) {
            dataFile.getDataTable().getDataVariables().get(varnum).setUnf(unf);
        } else {
            logger.warning("failed to calculate UNF signature for variable " + varnum);
        }
    }
    
    private void calculateUNF(DataFile dataFile, int varnum, Long[] dataVector) {
        String unf = null;
        try {
            unf = UNFUtil.calculateUNF(dataVector);
        } catch (IOException iex) {
            logger.warning("exception thrown when attempted to calculate UNF signature for (numeric, discrete) variable " + varnum);
        }  catch (UnfException uex) {
            logger.warning("UNF Exception: thrown when attempted to calculate UNF signature for (numeric, discrete) variable " + varnum);
        }
        
        if (unf != null) {
            dataFile.getDataTable().getDataVariables().get(varnum).setUnf(unf);
        } else {
            logger.warning("failed to calculate UNF signature for variable " + varnum);
        }
    }
    
    private void calculateUNF(DataFile dataFile, int varnum, String[] dataVector) throws IOException {
        String unf = null;
        
        String[] dateFormats = null; 
        
        // Special handling for Character strings that encode dates and times:
        
        if ("time".equals(dataFile.getDataTable().getDataVariables().get(varnum).getFormatCategory())) {
            dateFormats = new String[dataVector.length];
            String savedDateTimeFormat = dataFile.getDataTable().getDataVariables().get(varnum).getFormat();
            String timeFormat = null;
            if (savedDateTimeFormat != null && !savedDateTimeFormat.equals("")) {
                timeFormat = savedDateTimeFormat;
            } else {
                timeFormat = dateTimeFormat_ymdhmsS;
            }
            
            /* What follows is special handling of a special case of time values
             * non-uniform precision; specifically, when some have if some have 
             * milliseconds, and some don't. (and that in turn is only 
             * n issue when the timezone is present... without the timezone
             * the time string would still evaluate to the end, even if the 
             * format has the .SSS part and the string does not.
             * This case will be properly handled internally, once we permanently
             * switch to UNF6.
             * -- L.A. 4.0 beta 8
             */
            String simplifiedFormat = null;
            SimpleDateFormat fullFormatParser = null;
            SimpleDateFormat simplifiedFormatParser = null;
            
            if (timeFormat.matches(".*\\.SSS z$")) {
                simplifiedFormat = timeFormat.replace(".SSS", "");
                
                fullFormatParser = new SimpleDateFormat(timeFormat);
                simplifiedFormatParser = new SimpleDateFormat(simplifiedFormat);
            } 
            
            for (int i = 0; i < dataVector.length; i++) {
                if (dataVector[i] != null) {
                    
                    if (simplifiedFormatParser != null) {
                        // first, try to parse the value against the "full" 
                        // format (with the milliseconds part):
                        fullFormatParser.setLenient(false);
                    
                        try {
                            logger.fine("trying the \"full\" time format, with milliseconds: "+timeFormat+", "+dataVector[i]);
                            fullFormatParser.parse(dataVector[i]);
                         } catch (ParseException ex) {
                            // try the simplified (no time zone) format instead:
                            logger.fine("trying the simplified format: "+simplifiedFormat+", "+dataVector[i]);
                            simplifiedFormatParser.setLenient(false);
                            try {
                                simplifiedFormatParser.parse(dataVector[i]);
                                timeFormat = simplifiedFormat;
                            } catch (ParseException ex1) {
                                logger.warning("no parseable format found for time value "+i+" - "+dataVector[i]);
                                throw new IOException("no parseable format found for time value "+i+" - "+dataVector[i]);
                            }
                        }

                    } 
                    dateFormats[i] = timeFormat;
                }
            }
        } else if ("date".equals(dataFile.getDataTable().getDataVariables().get(varnum).getFormatCategory())) {
            dateFormats = new String[dataVector.length];
            String savedDateFormat = dataFile.getDataTable().getDataVariables().get(varnum).getFormat();
            for (int i = 0; i < dataVector.length; i++) {
                if (dataVector[i] != null) {
                    if (savedDateFormat != null && !savedDateFormat.equals("")) {
                        dateFormats[i] = savedDateFormat;
                    } else {
                        dateFormats[i] = dateFormat_ymd;
                    }
                }
            }
        }
                
        try {
            if (dateFormats == null) {
                logger.fine("calculating the UNF value for string vector; first value: "+dataVector[0]);
                unf = UNFUtil.calculateUNF(dataVector);
            } else {
                unf = UNFUtil.calculateUNF(dataVector, dateFormats);
            }
        } catch (IOException iex) {
            logger.warning("IO exception thrown when attempted to calculate UNF signature for (character) variable " + varnum);
        } catch (UnfException uex) {
            logger.warning("UNF Exception: thrown when attempted to calculate UNF signature for (character) variable " + varnum);
        }
        
        if (unf != null) {
            dataFile.getDataTable().getDataVariables().get(varnum).setUnf(unf);
        } else {
            logger.warning("failed to calculate UNF signature for variable " + varnum);
        }
    }
    
    // Calculating UNFs from *floats*, not *doubles* - this is to test dataverse
    // 4.0 Ingest against DVN 3.*; because of the nature of the UNF bug, reading
    // the tab file entry with 7+ digits of precision as a Double will result
    // in a UNF signature *different* from what was produced by the v. 3.* ingest,
    // from a STATA float value directly. 
    // TODO: remove this from the final production 4.0!
    // -- L.A., Jul 2014
    
    private void calculateUNF(DataFile dataFile, int varnum, Float[] dataVector) {
        String unf = null;
        try {
            unf = UNFUtil.calculateUNF(dataVector);
        } catch (IOException iex) {
            logger.warning("exception thrown when attempted to calculate UNF signature for numeric, \"continuous\" (float) variable " + varnum);
        } catch (UnfException uex) {
            logger.warning("UNF Exception: thrown when attempted to calculate UNF signature for numeric, \"continuous\" (float) variable" + varnum);
        }
        
        if (unf != null) {
            dataFile.getDataTable().getDataVariables().get(varnum).setUnf(unf);
        } else {
            logger.warning("failed to calculate UNF signature for variable " + varnum);
        }
    }
    
    // This method takes a list of file ids, checks the format type of the ingested 
    // original, and attempts to fix it if it's missing. 
    // Note the @Asynchronous attribute - this allows us to just kick off and run this 
    // (potentially large) job in the background. 
    // The method is called by the "fixmissingoriginaltypes" /admin api call. 
    @Asynchronous
    public void fixMissingOriginalTypes(List<Long> datafileIds) {
        for (Long fileId : datafileIds) {
            fixMissingOriginalType(fileId);
        }
        logger.info("Finished repairing tabular data files that were missing the original file format labels.");
    }
    
    // This method takes a list of file ids and tries to fix the size of the saved 
    // original, if present
    // Note the @Asynchronous attribute - this allows us to just kick off and run this 
    // (potentially large) job in the background. 
    // The method is called by the "fixmissingoriginalsizes" /admin api call. 
    @Asynchronous
    public void fixMissingOriginalSizes(List<Long> datafileIds) {
        for (Long fileId : datafileIds) {
            fixMissingOriginalSize(fileId);
            try {
                Thread.sleep(1000);
            } catch (Exception ex) {}
        }
        logger.info("Finished repairing tabular data files that were missing the original file sizes.");
    }
    
    // This method fixes a datatable object that's missing the format type of 
    // the ingested original. It will check the saved original file to 
    // determine the type. 
    private void fixMissingOriginalType(long fileId) {
        DataFile dataFile = fileService.find(fileId);

        if (dataFile != null && dataFile.isTabularData()) {
            String originalFormat = dataFile.getDataTable().getOriginalFileFormat();
            Long datatableId = dataFile.getDataTable().getId();
            if (StringUtil.isEmpty(originalFormat) || originalFormat.equals(FileUtil.MIME_TYPE_INGESTED_FILE)) {

                // We need to determine the mime type of the saved original
                // and save it in the database. 
                // 
                // First, we need access to the file. Note that the code below 
                // works with any supported StorageIO driver (although, as of now
                // all the production installations out there are only using filesystem
                // access; but just in case)
                // The FileUtil method that determines the type takes java.io.File 
                // as an argument. So for StorageIO drivers that provide local 
                // file access, we'll just go directly to the stored file. For 
                // swift and similar implementations, we'll read the saved aux 
                // channel and save it as a local temp file. 
                
                StorageIO<DataFile> storageIO;

                File savedOriginalFile = null;
                boolean tempFileRequired = false;
                
                try {
                    storageIO = dataFile.getStorageIO();
                    storageIO.open();


                    if (storageIO.isLocalFile()) {
                        try {
                            savedOriginalFile = storageIO.getAuxObjectAsPath(FileUtil.SAVED_ORIGINAL_FILENAME_EXTENSION).toFile();
                        } catch (IOException ioex) {
                            // do nothing, just make sure savedOriginalFile is still null:
                            savedOriginalFile = null;
                        }
                    }

                    if (savedOriginalFile == null) {
                        tempFileRequired = true;

                        ReadableByteChannel savedOriginalChannel = (ReadableByteChannel) storageIO.openAuxChannel(FileUtil.SAVED_ORIGINAL_FILENAME_EXTENSION);
                        savedOriginalFile = File.createTempFile("tempSavedOriginal", ".tmp");
                        FileChannel tempSavedOriginalChannel = new FileOutputStream(savedOriginalFile).getChannel();
                        tempSavedOriginalChannel.transferFrom(savedOriginalChannel, 0, storageIO.getAuxObjectSize(FileUtil.SAVED_ORIGINAL_FILENAME_EXTENSION));

                    }
                } catch (Exception ex) {
                    logger.warning("Exception "+ex.getClass()+" caught trying to open StorageIO channel for the saved original; (datafile id=" + fileId + ", datatable id=" + datatableId + "): " + ex.getMessage());
                    savedOriginalFile = null;
                }

                if (savedOriginalFile == null) {
                    logger.warning("Could not obtain the saved original file as a java.io.File! (datafile id=" + fileId + ", datatable id=" + datatableId + ")");
                    return;
                }

                String fileTypeDetermined = null;

                try {
                    fileTypeDetermined = FileUtil.determineFileType(savedOriginalFile, "");
                } catch (IOException ioex) {
                    logger.warning("Caught exception trying to determine original file type (datafile id=" + fileId + ", datatable id=" + datatableId + "): " + ioex.getMessage());
                }
                
                Long savedOriginalFileSize = savedOriginalFile.length(); 
                
                // If we had to create a temp file, delete it now: 
                if (tempFileRequired) {
                    savedOriginalFile.delete();
                }

                if (fileTypeDetermined == null) {
                    logger.warning("Failed to determine preserved original file type. (datafile id=" + fileId + ", datatable id=" + datatableId + ")");
                    return;
                }
                // adjust the final result:
                // we know that this file has been successfully ingested; 
                // so if the FileUtil is telling us it's a "plain text" file at this point,
                // it really means it must be a CSV file. 
                if (fileTypeDetermined.startsWith("text/plain")) {
                    fileTypeDetermined = FileUtil.MIME_TYPE_CSV;
                }
                // and, finally, if it is still "application/octet-stream", it must be Excel:
                if (FileUtil.MIME_TYPE_UNDETERMINED_DEFAULT.equals(fileTypeDetermined)) {
                    fileTypeDetermined = FileUtil.MIME_TYPE_XLSX;
                }
                logger.info("Original file type determined: " + fileTypeDetermined + " (file id=" + fileId + ", datatable id=" + datatableId + "; file path: " + savedOriginalFile.getAbsolutePath() + ")");

                // save permanently in the database:
                dataFile.getDataTable().setOriginalFileFormat(fileTypeDetermined);
                dataFile.getDataTable().setOriginalFileSize(savedOriginalFileSize);
                fileService.saveDataTable(dataFile.getDataTable());

            } else {
                logger.info("DataFile id=" + fileId + "; original type already present: " + originalFormat);
            }
        } else {
            logger.warning("DataFile id=" + fileId + ": No such DataFile!");
        }
    }
    
    // This method fixes a datatable object that's missing the size of the 
    // ingested original. 
    private void fixMissingOriginalSize(long fileId) {
        DataFile dataFile = fileService.find(fileId);

        if (dataFile != null && dataFile.isTabularData()) {
            Long savedOriginalFileSize = dataFile.getDataTable().getOriginalFileSize();
            Long datatableId = dataFile.getDataTable().getId();
            
            if (savedOriginalFileSize == null) {
                
                StorageIO<DataFile> storageIO;
                
                try {
                    storageIO = dataFile.getStorageIO();
                    storageIO.open();
                    savedOriginalFileSize = storageIO.getAuxObjectSize(FileUtil.SAVED_ORIGINAL_FILENAME_EXTENSION);

                } catch (Exception ex) {
                    logger.warning("Exception "+ex.getClass()+" caught trying to look up the size of the saved original; (datafile id=" + fileId + ", datatable id=" + datatableId + "): " + ex.getMessage());
                    return;
                }

                if (savedOriginalFileSize == null) {
                    logger.warning("Failed to look up the size of the saved original file! (datafile id=" + fileId + ", datatable id=" + datatableId + ")");
                    return;
                }

                // save permanently in the database:
                dataFile.getDataTable().setOriginalFileSize(savedOriginalFileSize);
                fileService.saveDataTable(dataFile.getDataTable());

            } else {
                logger.info("DataFile id=" + fileId + "; original file size already present: " + savedOriginalFileSize);
            }
        } else {
            logger.warning("DataFile id=" + fileId + ": No such DataFile!");
        }
    }
    
    public static void main(String[] args) {
        
        String file = args[0];
        String type = args[1]; 
        
        if (file == null || type == null || "".equals(file) || "".equals(type)) {
            System.err.println("Usage: java edu.harvard.iq.dataverse.ingest.IngestServiceBean <file> <type>.");
            System.exit(1);
        }
        
        BufferedInputStream fileInputStream = null; 
        
        try {
            fileInputStream = new BufferedInputStream(new FileInputStream(new File(file)));
        } catch (FileNotFoundException notfoundEx) {
            fileInputStream = null; 
        }
        
        if (fileInputStream == null) {
            System.err.println("Could not open file "+file+".");
            System.exit(1);
        }
        
        TabularDataFileReader ingestPlugin = getTabDataReaderByMimeType(type);

        if (ingestPlugin == null) {
            System.err.println("Could not locate an ingest plugin for type "+type+".");
            System.exit(1);
        }
        
        TabularDataIngest tabDataIngest = null;
        
        try {
            tabDataIngest = ingestPlugin.read(fileInputStream, null);
        } catch (IOException ingestEx) {
            System.err.println("Caught an exception trying to ingest file "+file+".");
            System.exit(1);
        }
        
        try {
            if (tabDataIngest != null) {
                File tabFile = tabDataIngest.getTabDelimitedFile();

                if (tabDataIngest.getDataTable() != null
                        && tabFile != null
                        && tabFile.exists()) {

                    String tabFilename = FileUtil.replaceExtension(file, "tab");
                    
                    Files.copy(Paths.get(tabFile.getAbsolutePath()), Paths.get(tabFilename), StandardCopyOption.REPLACE_EXISTING);
                    
                    DataTable dataTable = tabDataIngest.getDataTable();
                    
                    System.out.println ("NVARS: "+dataTable.getVarQuantity());
                    System.out.println ("NOBS: "+dataTable.getCaseQuantity());
                    System.out.println ("UNF: "+dataTable.getUnf());
                    
                    for (int i = 0; i < dataTable.getVarQuantity(); i++) {
                        String vartype = "";
                        
                        if (dataTable.getDataVariables().get(i).isIntervalContinuous()) {
                            vartype = "numeric-continuous";
                        } else {
                            if (dataTable.getDataVariables().get(i).isTypeNumeric()) {
                                vartype = "numeric-discrete";
                            } else {
                                vartype = "character";
                            }
                        }
                        
                        System.out.print ("VAR"+i+" ");
                        System.out.print (dataTable.getDataVariables().get(i).getName()+" ");
                        System.out.print (vartype+" ");
                        System.out.print (dataTable.getDataVariables().get(i).getUnf());
                        System.out.println(); 
                        
                    }
                
                } else {
                    System.err.println("Ingest failed to produce tab file or data table for file "+file+".");
                    System.exit(1);
                }
            } else {
                System.err.println("Ingest resulted in a null tabDataIngest object for file "+file+".");
                System.exit(1);
            }
        } catch (IOException ex) {
            System.err.println("Caught an exception trying to save ingested data for file "+file+".");
            System.exit(1);
        }
        
    }
    /*
    private class InternalIngestException extends Exception {
        
    }
    
    public class IngestServiceException extends Exception {
        
    }
    */
}
