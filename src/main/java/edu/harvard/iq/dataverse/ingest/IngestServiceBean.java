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
import edu.harvard.iq.dataverse.DataTable;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetFieldValue;
import edu.harvard.iq.dataverse.DatasetFieldCompoundValue;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.MetadataBlock;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.dataaccess.DataAccessObject;
import edu.harvard.iq.dataverse.dataaccess.DataAccessOption;
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
import edu.harvard.iq.dataverse.util.ShapefileHandler;
import edu.harvard.iq.dataverse.util.SumStatCalculator;
import edu.harvard.iq.dvn.unf.*;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
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
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.io.FileUtils;

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
    
    private static final String MIME_TYPE_UNDETERMINED_DEFAULT = "application/octet-stream";
    private static String dateTimeFormat_ymdhmsS = "yyyy-MM-dd HH:mm:ss.SSS";
    private static String dateFormat_ymd = "yyyy-MM-dd";
      
    @Deprecated
    // All the parts of the app should use the createDataFiles() method instead, 
    // that returns a list of DataFiles. 
    public DataFile createDataFile(DatasetVersion version, InputStream inputStream, String fileName, String contentType) throws IOException {
        List<DataFile> fileList = createDataFiles(version, inputStream, fileName, contentType);
        
        if (fileList == null) {
            return null; 
        }
        
        return fileList.get(0);
    }
    
    public List<DataFile> createDataFiles(DatasetVersion version, InputStream inputStream, String fileName, String contentType) throws IOException {
        List<DataFile> datafiles = new ArrayList<DataFile>(); 
        
        // save the file, in the temporary location for now: 
        Path tempFile = null; 
        
        
        if (getFilesTempDirectory() != null) {
            tempFile = Files.createTempFile(Paths.get(getFilesTempDirectory()), "tmp", "upload");
            // "temporary" location is the key here; this is why we are not using 
            // the DataStore framework for this - the assumption is that 
            // temp files will always be stored on the local filesystem. 
            //          -- L.A. Jul. 2014
            logger.fine("Will attempt to save the file as: " + tempFile.toString());
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
        } else {
            throw new IOException ("Temp directory is not configured.");
        }
        
        // Let's try our own utilities (Jhove, etc.) to determine the file type 
        // of the uploaded file. (We may already have a mime type supplied for this
        // file - maybe the type that the browser recognized on upload; or, if 
        // it's a harvest, maybe the remote server has already given us the type
        // for this file... with our own type utility we may or may not do better 
        // than the type supplied:
        //  -- L.A. 
        String recognizedType = null;
        String finalType = null; 
        try {
            recognizedType = FileUtil.determineFileType(tempFile.toFile(), fileName);
            logger.fine("File utility recognized the file as " + recognizedType);
            if (recognizedType != null && !recognizedType.equals("")) {
                // is it any better than the type that was supplied to us,
                // if any?
                if (contentType == null
                        || contentType.equals("")
                        || contentType.equalsIgnoreCase(MIME_TYPE_UNDETERMINED_DEFAULT)
                        || recognizedType.equals("application/fits-gzipped")
                        || recognizedType.equals(ShapefileHandler.SHAPEFILE_FILE_TYPE)) {
                    finalType = recognizedType;
                }
            }
            
        } catch (IOException ex) {
            logger.warning("Failed to run the file utility mime type check on file " + fileName);
        }
        
        if (finalType == null) {
            finalType = (contentType == null || contentType.equals("")) 
                ? MIME_TYPE_UNDETERMINED_DEFAULT
                : contentType;
        }
                
        // A few special cases: 
        
        // if this is a gzipped FITS file, we'll uncompress it, and ingest it as
        // a regular FITS file:
        
        if (finalType.equals("application/fits-gzipped")) {

            InputStream uncompressedIn = null;
            String finalFileName = fileName;
            // if the file name had the ".gz" extension, remove it, 
            // since we are going to uncompress it:
            if (fileName != null && fileName.matches(".*\\.gz$")) {
                finalFileName = fileName.replaceAll("\\.gz$", "");
            }
            
            DataFile datafile = null; 
            try {                
                uncompressedIn = new GZIPInputStream(new FileInputStream(tempFile.toFile()));  
                datafile = createSingleDataFile(version, uncompressedIn, finalFileName, MIME_TYPE_UNDETERMINED_DEFAULT);
            } catch (IOException ioex) {
                datafile = null;
            } finally {
                if (uncompressedIn != null) {
                    try {uncompressedIn.close();} catch (IOException e) {}
                }
            }
            
            // If we were able to produce an uncompressed file, we'll use it 
            // to create and return a final DataFile; if not, we're not going
            // to do anything - and then a new DataFile will be created further 
            // down, from the original, uncompressed file.
            if (datafile != null) {
                // remove the compressed temp file: 
                try {
                    tempFile.toFile().delete();
                } catch (SecurityException ex) {
                    // (this is very non-fatal)
                    logger.warning("Failed to delete temporary file "+tempFile.toString());
                }
                
                datafiles.add(datafile);
                return datafiles;
            }
                
        // If it's a ZIP file, we are going to unpack it and create multiple 
        // DataFile objects from its contents:
       // } else if (( finalType.equals("application/zip")) || (finalType.equals(ShapefileHandler.SHAPEFILE_FILE_TYPE))) {   
          } else if (finalType.equals("application/zip")) {   
            
            ZipInputStream unZippedIn = null; 
            ZipEntry zipEntry = null; 
            try {
                unZippedIn = new ZipInputStream(new FileInputStream(tempFile.toFile()));

                if (unZippedIn == null) {
                    return null;
                }
                while ((zipEntry = unZippedIn.getNextEntry()) != null) {
                    // Note that some zip entries may be directories - we 
                    // simply skip them:
                    
                    if (!zipEntry.isDirectory()) {

                        String fileEntryName = zipEntry.getName();

                        if (fileEntryName != null && !fileEntryName.equals("")) {

                            fileEntryName = fileEntryName.replaceFirst("^.*[\\/]", "");

                            // Check if it's a "fake" file - a zip archive entry 
                            // created for a MacOS X filesystem element: (these 
                            // start with "._")
                            if (!fileEntryName.startsWith("._")) {
                                // OK, this seems like an OK file entry - we'll try 
                                // to read it and create a DataFile with it:

                                DataFile datafile = createSingleDataFile(version, unZippedIn, fileEntryName, MIME_TYPE_UNDETERMINED_DEFAULT);
                                
                                if (datafile != null) {
                                    // We have created this datafile with the mime type "unknown";
                                    // Now that we have it saved in a temporary location, 
                                    // let's try and determine its real type:
                                    
                                    String tempFileName = getFilesTempDirectory() + "/" + datafile.getFileSystemName();
                                    
                                    try {
                                        recognizedType = FileUtil.determineFileType(new File(tempFileName), fileEntryName);
                                        logger.fine("File utility recognized unzipped file as " + recognizedType);
                                        if (recognizedType != null && !recognizedType.equals("")) {
                                            datafile.setContentType(recognizedType);
                                        }
                                    } catch (IOException ex) {
                                        logger.warning("Failed to run the file utility mime type check on file " + fileName);
                                    }
                                    
                                    datafiles.add(datafile);
                                }
                            }
                        }
                    }
                    unZippedIn.closeEntry(); 
                }
                
            } catch (IOException ioex) {
                // just clear the datafiles list and let 
                // ingest default to creating a single DataFile out
                // of the unzipped file. 
                datafiles.clear();
            } finally {
                if (unZippedIn != null) {
                    try {unZippedIn.close();} catch (Exception zEx) {}
                }
            }
            if (datafiles.size() > 0) {
                return datafiles;
            }
            
        } else if (finalType.equals(ShapefileHandler.SHAPEFILE_FILE_TYPE)) {
            // Shape files may have to be split into multiple files, 
            // one zip archive per each complete set of shape files:
                       
            //File rezipFolder = new File(this.getFilesTempDirectory());
            File rezipFolder = this.getShapefileUnzipTempDirectory();
            
            IngestServiceShapefileHelper shpIngestHelper;
            shpIngestHelper = new IngestServiceShapefileHelper(tempFile.toFile(), rezipFolder);

            boolean didProcessWork = shpIngestHelper.processFile();
            if (!(didProcessWork)){            
                logger.severe("Processing of zipped shapefile failed.");
                return null;
            }
            for (File finalFile : shpIngestHelper.getFinalRezippedFiles()){
                FileInputStream finalFileInputStream = new FileInputStream(finalFile);
                finalType = this.getContentType(finalFile);
                if (finalType==null){
                    logger.warning("Content type is null; but should default to 'MIME_TYPE_UNDETERMINED_DEFAULT'");
                    continue; 
                }               
                DataFile new_datafile = createSingleDataFile(version, finalFileInputStream, finalFile.getName(), finalType);
                if (new_datafile != null) {
                  datafiles.add(new_datafile);
                }
                finalFileInputStream.close();                
             
            }
            
            // Delete the temp directory used for unzipping
            logger.fine("Delete temp shapefile unzip directory: " + rezipFolder.getAbsolutePath());
            FileUtils.deleteDirectory(rezipFolder);

            // Delete rezipped files
            for (File finalFile : shpIngestHelper.getFinalRezippedFiles()){
                if (finalFile.isFile()){
                    finalFile.delete();
                }
            }
            
            if (datafiles.size() > 0) {
                return datafiles;
            }
            return null;
           
        }

        
        // Finally, if none of the special cases above were applicable (or 
        // if we were unable to unpack an uploaded file, etc.), we'll just 
        // create and return a single DataFile:
        // (Note that we are passing null for the InputStream; that's because
        // we already have the file saved; we'll just need to rename it, below)
        
        DataFile datafile = createSingleDataFile(version, null, fileName, finalType);;
        
        if (datafile != null) {
            fileService.generateStorageIdentifier(datafile);
            if (!tempFile.toFile().renameTo(new File(getFilesTempDirectory() + "/" + datafile.getFileSystemName()))) {
                return null; 
            }
            
            // MD5:
            MD5Checksum md5Checksum = new MD5Checksum();
            try {
                datafile.setmd5(md5Checksum.CalculateMD5(getFilesTempDirectory() + "/" + datafile.getFileSystemName()));
            } catch (Exception md5ex) {
                logger.warning("Could not calculate MD5 signature for new file " + fileName);
            }
        
            datafiles.add(datafile);
            return datafiles;
        }
        
        return null;
    }   // end createDataFiles
    
    // TODO: 
    // add comments explaining what's going on in the 2 methods below. 
    // -- L.A. 4.0 beta
    private String checkForDuplicateFileNames(DatasetVersion version, String fileName) {
        Set<String> fileNamesExisting = new HashSet<String>();

        Iterator<FileMetadata> fmIt = version.getFileMetadatas().iterator();
        while (fmIt.hasNext()) {
            FileMetadata fm = fmIt.next();
            String existingName = fm.getLabel();
            
            if (existingName != null) {
                // if it's a tabular file, we need to restore the original file name; 
                // otherwise, we may miss a match. e.g. stata file foobar.dta becomes
                // foobar.tab once ingested! 
                if (fm.getDataFile().isTabularData()) {
                    String originalMimeType = fm.getDataFile().getDataTable().getOriginalFileFormat();
                    if ( originalMimeType != null) {
                        String origFileExtension = generateOriginalExtension(originalMimeType);
                        existingName = existingName.replaceAll(".tab$", origFileExtension);
                    } else {
                        existingName = existingName.replaceAll(".tab$", "");
                    }
                }
                fileNamesExisting.add(existingName);
            }
        }

        while (fileNamesExisting.contains(fileName)) {
            fileName = generateNewFileName(fileName);
        }

        return fileName;
    }
    
    // TODO: 
    // Move this method (duplicated in StoredOriginalFile.java) to 
    // FileUtil.java. 
    // -- L.A. 4.0 beta
    
    private static String generateOriginalExtension(String fileType) {

        if (fileType.equalsIgnoreCase("application/x-spss-sav")) {
            return ".sav";
        } else if (fileType.equalsIgnoreCase("application/x-spss-por")) {
            return ".por";
        } else if (fileType.equalsIgnoreCase("application/x-stata")) {
            return ".dta";
        } else if (fileType.equalsIgnoreCase( "application/x-rlang-transport")) {
            return ".RData";
        } else if (fileType.equalsIgnoreCase("text/csv")) {
            return ".csv";
        } else if (fileType.equalsIgnoreCase( "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
            return ".xlsx";
        }

       return "";
    }

    
    private String generateNewFileName(String fileName) {
        String newName = null;
        String baseName = null; 
        String extension = null; 
        
        int extensionIndex = fileName.lastIndexOf(".");
        if (extensionIndex != -1 ) {
            extension = fileName.substring(extensionIndex+1);
            baseName = fileName.substring(0, extensionIndex);
        } else {
            baseName = fileName; 
        }
        
        if (baseName.matches(".*-[0-9][0-9]*$")) {
            int dashIndex = baseName.lastIndexOf("-");
            String numSuffix = baseName.substring(dashIndex+1);
            String basePrefix = baseName.substring(0,dashIndex);
            int numSuffixValue = new Integer(numSuffix).intValue();
            numSuffixValue++; 
            baseName = basePrefix + "-" + numSuffixValue;
        } else {
            baseName = baseName + "-1"; 
        }
        
        newName = baseName; 
        if (extension != null) {
            newName = newName + "." + extension; 
        }
        
        return newName;
    }
    
    /**
     *  Returns a content type string for a FileObject
     * 
     */
    private String getContentType(File fileObject){
        if (fileObject==null){
            return null;
        }
        String contentType;
        try {
            contentType = FileUtil.determineFileType(fileObject, fileObject.getName());
        } catch (IOException ex) {
            logger.info("FileUtil.determineFileType failed for file with name: " + fileObject.getName());
            contentType = null;
        }

        if ((contentType==null)||(contentType=="")){
            contentType = "MIME_TYPE_UNDETERMINED_DEFAULT";
        }
        return contentType;
        
    }
    /* 
     * This method creates a DataFile, and also saves the bytes from the suppplied 
     * InputStream in the temporary location. 
     * This method should only be called by the upper-level methods that handle 
     * file upload and creation for individual use cases - a single file upload, 
     * an upload of a zip archive that needs to be unpacked and turned into 
     * individual files, etc., and once the file name and mime type have already 
     * been figured out. 
    */
    
    private DataFile createSingleDataFile(DatasetVersion version, InputStream inputStream, String fileName, String contentType) {

        DataFile datafile = new DataFile(contentType);
        FileMetadata fmd = new FileMetadata();
        
        fmd.setLabel(checkForDuplicateFileNames(version,fileName));

        datafile.setOwner(version.getDataset());
        fmd.setDataFile(datafile);
        datafile.getFileMetadatas().add(fmd);
        if (version.getFileMetadatas() == null) {
            version.setFileMetadatas(new ArrayList());
        }
        version.getFileMetadatas().add(fmd);
        fmd.setDatasetVersion(version);
        version.getDataset().getFiles().add(datafile);

        // And save the file - but only if the InputStream is not null; 
        // (the temp file may be saved already - if this is a single
        // file upload case - and in that case this method gets called 
        // with null for the inputStream)
        
        if (inputStream != null) {
        
            fileService.generateStorageIdentifier(datafile);
            BufferedOutputStream outputStream = null;

            // Once again, at this point we are dealing with *temp*
            // files only; these are always stored on the local filesystem, 
            // so we are using FileInput/Output Streams to read and write
            // these directly, instead of going through the Data Access 
            // framework. 
            //      -- L.A.
            
            try {
                outputStream = new BufferedOutputStream(new FileOutputStream(getFilesTempDirectory() + "/" + datafile.getFileSystemName()));

                byte[] dataBuffer = new byte[8192];
                int i = 0;

                while ((i = inputStream.read(dataBuffer)) > 0) {
                    outputStream.write(dataBuffer, 0, i);
                    outputStream.flush();
                }
            } catch (IOException ioex) {
                datafile = null; 
            } finally {
                try {
                    outputStream.close();
                } catch (IOException ioex) {}
            }
            
            // MD5:
            if (datafile != null) {
                MD5Checksum md5Checksum = new MD5Checksum();
                try {
                    datafile.setmd5(md5Checksum.CalculateMD5(getFilesTempDirectory() + "/" + datafile.getFileSystemName()));
                } catch (Exception md5ex) {
                    logger.warning("Could not calculate MD5 signature for new file " + fileName);
                }
            }
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

                    /*
                    // Hmm. Why exactly am I incrementing the filename sequence
                    // here? I could just save the file in the permanent location
                    // under the same name as the temp version... 
                    // Could be a moot point - this filename sequence has to 
                    // go away anyway... On the inside, it's too PostgresQL-specific;
                    // and it's too filesystem-specific architecturally. 
                    // -- L.A. Jul. 11 2014
                    datasetService.generateFileSystemName(dataFile);
                    */
                    
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
                            logger.fine("Successfully extracted indexable metadata from file " + fileName);
                        } else {
                            logger.fine("Failed to extract indexable metadata from file " + fileName);
                        }
                    }

                    // Try to save the file in its permanent location: 
                    Path tempLocationPath = Paths.get(getFilesTempDirectory() + "/" + dataFile.getFileSystemName());
                    WritableByteChannel writeChannel = null;
                    FileChannel readChannel = null;
                    
                    try {

                        DataAccessObject dataAccess = dataFile.getAccessObject();
                        
                        dataAccess.openChannel(DataAccessOption.WRITE_ACCESS);
                                                
                        writeChannel = dataAccess.getWriteChannel();
                        readChannel = new FileInputStream(tempLocationPath.toFile()).getChannel();
                                                
                        long bytesPerIteration = 16 * 1024; // 16K bytes
                        long start = 0;
                        while ( start < readChannel.size() ) {
                            readChannel.transferTo(start, bytesPerIteration, writeChannel);
                            start += bytesPerIteration;
                        }
                        /* 
                            TODO: - ?
                            May want to provide a simplified alternative clause for when the 
                            storage method is local filesystem; in that case DataAccess
                            can return a Path object, and then we can just do something 
                            like this: 
                            Files.copy(tempLocationPath, outputPath, StandardCopyOption.REPLACE_EXISTING);
                            -- L.A. Jul. 20 2014
                            Files.copy(tempLocationPath, dataFile.getFileSystemLocation(), StandardCopyOption.REPLACE_EXISTING);
                        */

                        // Set filesize in bytes
                        // 
                        try {
                            dataFile.setFilesize(readChannel.size());
                        } catch (IOException fsize_ex) {
                            logger.warning("Could not calculate the size of new file: " + fileName);
                        }
                        
                        
                    } catch (IOException ioex) {
                        logger.warning("Failed to save the file  " + dataFile.getFileSystemLocation());
                    } finally {
                        if (readChannel != null) {try{readChannel.close();}catch(IOException e){}}
                        if (writeChannel != null) {try{writeChannel.close();}catch(IOException e){}}
                    }

                    try {
                        logger.fine("Will attempt to delete the temp file "+tempLocationPath.toString());
                        Files.delete(tempLocationPath);
                    } catch (IOException ex) {
                        // (non-fatal)
                        logger.warning("Failed to delete temp file "+tempLocationPath.toString());
                    }
                    // Any necessary post-processing: 
                    performPostProcessingTasks(dataFile);
                }
            }
        }
    }
    
    /**
        For the restructuring of zipped shapefiles, create a timestamped directory.
        This directory is deleted after successful restructuring.
    
        Naming convention: getFilesTempDirectory() + "shp_" + "yyyy-MM-dd-hh-mm-ss-SSS"
    */
    private File getShapefileUnzipTempDirectory(){
        
        String tempDirectory = this.getFilesTempDirectory();
        if (tempDirectory == null){
            return null;
        }
        String datestampedFileName =  "shp_" + new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss-SSS").format(new Date());
        String datestampedFolderName = tempDirectory + "/" + datestampedFileName;
        
        File datestampedFolder = new File(datestampedFolderName);
        if (!datestampedFolder.isDirectory()) {
            /* Note that "createDirectories()" must be used - not 
             * "createDirectory()", to make sure all the parent 
             * directories that may not yet exist are created as well. 
             */
            try {
                Files.createDirectories(Paths.get(datestampedFolderName));
            } catch (IOException ex) {
                return null;
            }
        }
        return datestampedFolder;        
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
    
    // TODO: consider creating a version of this method that would take 
    // datasetversion as the argument. 
    // -- L.A. 4.0 post-beta. 
    public void startIngestJobs(Dataset dataset, AuthenticatedUser user) {
        int count = 0;
        IngestMessage ingestMessage = null;
        for (DataFile dataFile : dataset.getFiles()) {
            if (dataFile.isIngestScheduled()) {
                dataFile.SetIngestInProgress();
                dataFile = fileService.save(dataFile);

                if (ingestMessage == null) {
                    ingestMessage = new IngestMessage(IngestMessage.INGEST_MESAGE_LEVEL_INFO);
                }
                ingestMessage.addFileId(dataFile.getId());
                logger.info("Attempting to queue the file " + dataFile.getFileMetadata().getLabel() + "(" + dataFile.getFileMetadata().getDescription() + ") for ingest.");
                //asyncIngestAsTabular(dataFile);
                count++;
            }
        }

        if (count > 0) {
            String info = "Attempting to ingest " + count + " tabular data file(s).";
            if (user != null) {
                datasetService.addDatasetLock(dataset.getId(), user.getIdentifier(), info);
            } else {
                datasetService.addDatasetLock(dataset.getId(), null, info);
            }

            QueueConnection conn = null;
            QueueSession session = null;
            QueueSender sender = null;
            try {
                conn = factory.createQueueConnection();
                session = conn.createQueueSession(false, 0);
                sender = session.createSender(queue);

                //ingestMessage.addFile(new File(tempFileLocation));
                Message message = session.createObjectMessage(ingestMessage);

                //try {
                    sender.send(message);
                //} catch (JMSException ex) {
                //    ex.printStackTrace();
                //}

            } catch (JMSException ex) {
                ex.printStackTrace();
                //throw new IOException(ex.getMessage());
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
                    ex.printStackTrace();
                }
            }
        }
    }
    
    /*
    public void startIngestJobs (DatasetVersion datasetVersion) {
        for (FileMetadata fileMetadata : datasetVersion.getFileMetadatas()) {
            DataFile dataFile = fileMetadata.getDataFile();
            
            if (dataFile != null && dataFile.isIngestScheduled()) {
                dataFile.SetIngestInProgress();
                dataFile = fileService.save(dataFile);
                logger.info("Attempting to queue the file " + dataFile.getFileMetadata().getLabel() + "(" + dataFile.getFileMetadata().getDescription() + ") for ingest.");
                asyncIngestAsTabular(dataFile);
            }
        }

    }
    */
    
    public void produceSummaryStatistics(DataFile dataFile) throws IOException {
        /*
        logger.info("Skipping summary statistics and UNF.");
         */
        produceDiscreteNumericSummaryStatistics(dataFile); 
        produceContinuousSummaryStatistics(dataFile);
        produceCharacterSummaryStatistics(dataFile);
        
        recalculateDataFileUNF(dataFile);
    }
    
    public void produceContinuousSummaryStatistics(DataFile dataFile) throws IOException {

        // quick, but memory-inefficient way:
        // - this method just loads the entire file-worth of continuous vectors 
        // into a Double[][] matrix. 
        //Double[][] variableVectors = subsetContinuousVectors(dataFile);
        //calculateContinuousSummaryStatistics(dataFile, variableVectors);
        
        // A more sophisticated way: this subsets one column at a time, using 
        // the new optimized subsetting that does not have to read any extra 
        // bytes from the file to extract the column:
        
        TabularSubsetGenerator subsetGenerator = new TabularSubsetGenerator();
        
        for (int i = 0; i < dataFile.getDataTable().getVarQuantity(); i++) {
            if ("continuous".equals(dataFile.getDataTable().getDataVariables().get(i).getVariableIntervalType().getName())) {
                logger.fine("subsetting continuous vector");
                if ("float".equals(dataFile.getDataTable().getDataVariables().get(i).getFormatSchemaName())) {
                    Float[] variableVector = subsetGenerator.subsetFloatVector(dataFile, i);
                    logger.fine("Calculating summary statistics on a Float vector;");
                    calculateContinuousSummaryStatistics(dataFile, i, variableVector);
                    // calculate the UNF while we are at it:
                    logger.fine("Calculating UNF on a Float vector;");
                    calculateUNF(dataFile, i, variableVector);
                    variableVector = null; 
                } else {
                    Double[] variableVector = subsetGenerator.subsetDoubleVector(dataFile, i);
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
    
    public void produceDiscreteNumericSummaryStatistics(DataFile dataFile) throws IOException {
        
        TabularSubsetGenerator subsetGenerator = new TabularSubsetGenerator();
        
        for (int i = 0; i < dataFile.getDataTable().getVarQuantity(); i++) {
            if ("discrete".equals(dataFile.getDataTable().getDataVariables().get(i).getVariableIntervalType().getName()) 
                    && "numeric".equals(dataFile.getDataTable().getDataVariables().get(i).getVariableFormatType().getName())) {
                logger.fine("subsetting discrete-numeric vector");
                //Double[] variableVector = subsetGenerator.subsetDoubleVector(dataFile, i);
                Long[] variableVector = subsetGenerator.subsetLongVector(dataFile, i);
                // We are discussing calculating the same summary stats for 
                // all numerics (the same kind of sumstats that we've been calculating
                // for numeric continuous type)  -- L.A. Jul. 2014
                calculateContinuousSummaryStatistics(dataFile, i, variableVector);
                // calculate the UNF while we are at it:
                logger.fine("Calculating UNF on a Long (Double, really...) vector");
                calculateUNF(dataFile, i, variableVector);
                logger.fine("Done! (discrete numeric)");
                variableVector = null; 
            }
        }
    }
    
    public void produceCharacterSummaryStatistics(DataFile dataFile) throws IOException {

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
        */
        
        TabularSubsetGenerator subsetGenerator = new TabularSubsetGenerator();
        
        for (int i = 0; i < dataFile.getDataTable().getVarQuantity(); i++) {
            if ("character".equals(dataFile.getDataTable().getDataVariables().get(i).getVariableFormatType().getName())) {
                logger.fine("subsetting character vector");
                String[] variableVector = subsetGenerator.subsetStringVector(dataFile, i);
                //calculateCharacterSummaryStatistics(dataFile, i, variableVector);
                // calculate the UNF while we are at it:
                logger.fine("Calculating UNF on a String vector");
                calculateUNF(dataFile, i, variableVector);
                logger.fine("Done! (character)");
                variableVector = null; 
            }
        }
    }
    
    public void recalculateDataFileUNF(DataFile dataFile) {
        String[] unfValues = new String[dataFile.getDataTable().getVarQuantity().intValue()];
        String fileUnfValue = null; 
        
        for (int i = 0; i < dataFile.getDataTable().getVarQuantity(); i++) {
            String varunf = dataFile.getDataTable().getDataVariables().get(i).getUnf();
            unfValues[i] = varunf; 
        }
        
        try {
            fileUnfValue = UNF5Util.calculateUNF(unfValues);
        } catch (IOException ex) {
            logger.warning("Failed to recalculate the UNF for the datafile id="+dataFile.getId());
        }
        
        if (fileUnfValue != null) {
            dataFile.getDataTable().setUnf(fileUnfValue);
        }
    }
    
    /*
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
            ingestMessage.addFileId(dataFile.getId());
            
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
    */
    public boolean ingestAsTabular(Long datafile_id) { //DataFile dataFile) throws IOException {
        DataFile dataFile = fileService.find(datafile_id);
        if (dataFile != null) {
            return ingestAsTabular(dataFile.getFileSystemLocation().toString(), dataFile); 
        }
        return false;
    }
    
    public void sendFailNotification(Long dataset_id) {
        FacesMessage facesMessage = new FacesMessage("ingest failed");
        PushContext pushContext = PushContextFactory.getDefault().getPushContext();
        pushContext.push("/ingest" + dataset_id, facesMessage);
        logger.info("sent push (fail) notification to the page.");
    }
    
    public boolean ingestAsTabular(String tempFileLocation, DataFile dataFile) { //throws IOException {
        // TODO: 
        // (still work in progress)
        // Error reporting, with descriptions of specific problems, is 
        // being added here. 
        // -- L.A. 10 June 2014
        // TODO:
        // streamline this code; 
        // get rid of the duplicated fragments, add helper methods
        // for updaing ingest status reports/push notifications. 
        // -- L.A. 12 Aug. 2014
        
        boolean ingestSuccessful = false;
        IngestReport errorReport = null;
        Long datafile_id = dataFile == null ? null : dataFile.getId();

        PushContext pushContext = PushContextFactory.getDefault().getPushContext();
        if (pushContext != null) {
            Logger.getLogger(IngestServiceBean.class.getName()).log(Level.FINE, "Ingest: Obtained push context "
                    + pushContext.toString());
        } else {
            Logger.getLogger(IngestServiceBean.class.getName()).log(Level.SEVERE, "Warning! Could not obtain push context.");
        }      
        
        // Locate ingest plugin for the file format by looking
        // it up with the Ingest Service Provider Registry:
        //TabularDataFileReader ingestPlugin = IngestSP.getTabDataReaderByMIMEType(dFile.getContentType());
        //TabularDataFileReader ingestPlugin = new DTAFileReader(new DTAFileReaderSpi());
        String fileName = dataFile.getFileMetadata().getLabel();
        TabularDataFileReader ingestPlugin = getTabDataReaderByMimeType(dataFile.getContentType());

        if (ingestPlugin == null) {
            dataFile.SetIngestProblem();
            errorReport = new IngestReport();
            errorReport.setFailure();
            errorReport.setReport("No ingest plugin found for file type "+dataFile.getContentType());
            errorReport.setDataFile(dataFile);
            dataFile.setIngestReport(errorReport);
            dataFile = fileService.save(dataFile);
            
            FacesMessage facesMessage = new FacesMessage("ingest failed");
            pushContext.push("/ingest"+dataFile.getOwner().getId(), facesMessage);
            Logger.getLogger(IngestServiceBean.class.getName()).log(Level.INFO, "Ingest failure: Sent push notification to the page.");
            //throw new IOException("Could not find ingest plugin for the file " + fileName);
            return false; 
        }

        FileInputStream tempFileInputStream = null; 
        
        try {
            tempFileInputStream = new FileInputStream(new File(tempFileLocation));
        } catch (FileNotFoundException notfoundEx) {
            dataFile.SetIngestProblem();
            
            errorReport = new IngestReport();
            errorReport.setFailure();
            errorReport.setReport("IO Exception occured while trying to open the file for reading.");
            errorReport.setDataFile(dataFile);
            dataFile.setIngestReport(errorReport);
            dataFile = fileService.save(dataFile);
            
            dataFile = fileService.save(dataFile);
            FacesMessage facesMessage = new FacesMessage("ingest failed");
            pushContext.push("/ingest"+dataFile.getOwner().getId(), facesMessage);
            Logger.getLogger(IngestServiceBean.class.getName()).log(Level.INFO, "Ingest failure (No file produced): Sent push notification to the page.");
            return false; 
            //throw new IOException("Could not open temp file "+tempFileLocation);
        }
        
        TabularDataIngest tabDataIngest = null; 
        try {
            tabDataIngest = ingestPlugin.read(new BufferedInputStream(tempFileInputStream), null);
        } catch (IOException ingestEx) {
            dataFile.SetIngestProblem();
            errorReport = new IngestReport();
            errorReport.setFailure();
            errorReport.setReport(ingestEx.getMessage());
            errorReport.setDataFile(dataFile);
            dataFile.setIngestReport(errorReport);
            dataFile = fileService.save(dataFile);
            
            dataFile = fileService.save(dataFile);
            FacesMessage facesMessage = new FacesMessage("ingest failed");
            pushContext.push("/ingest"+dataFile.getOwner().getId(), facesMessage);
            Logger.getLogger(IngestServiceBean.class.getName()).log(Level.INFO, "Ingest failure (IO Exception): "+ingestEx.getMessage()+ "; Sent push notification to the page.");
            return false;
        } catch (Exception unknownEx) {
            // this is a bit of a kludge, to make sure no unknown exceptions are
            // left uncaught.
            dataFile.SetIngestProblem();
            errorReport = new IngestReport();
            errorReport.setFailure();
            errorReport.setReport(unknownEx.getMessage());
            errorReport.setDataFile(dataFile);
            dataFile.setIngestReport(errorReport);
            dataFile = fileService.save(dataFile);
            
            dataFile = fileService.save(dataFile);
            FacesMessage facesMessage = new FacesMessage("ingest failed");
            pushContext.push("/ingest"+dataFile.getOwner().getId(), facesMessage);
            Logger.getLogger(IngestServiceBean.class.getName()).log(Level.INFO, "Ingest failure (Unknown Exception): "+unknownEx.getMessage()+"; Sent push notification to the page.");
            return false;
            
        }

        try {
            if (tabDataIngest != null) {
                File tabFile = tabDataIngest.getTabDelimitedFile();

                if (tabDataIngest.getDataTable() != null
                        && tabFile != null
                        && tabFile.exists()) {

                    Logger.getLogger(IngestServiceBean.class.getName()).log(Level.INFO, "Tabular data successfully ingested; DataTable with "
                            + tabDataIngest.getDataTable().getVarQuantity() + " variables produced.");

                    Logger.getLogger(IngestServiceBean.class.getName()).log(Level.INFO, "Tab-delimited file produced: " + tabFile.getAbsolutePath());

                    if (MIME_TYPE_CSV_ALT.equals(dataFile.getContentType())) {
                        tabDataIngest.getDataTable().setOriginalFileFormat(MIME_TYPE_CSV);
                    } else {
                        tabDataIngest.getDataTable().setOriginalFileFormat(dataFile.getContentType());
                    }

                    // and we want to save the original of the ingested file: 
                    try {
                        saveIngestedOriginal(dataFile, new FileInputStream(new File(tempFileLocation)));
                    } catch (IOException iox) {
                        Logger.getLogger(IngestServiceBean.class.getName()).log(Level.INFO, "Failed to save the ingested original! " + iox.getMessage());
                    }

                    Files.copy(Paths.get(tabFile.getAbsolutePath()), dataFile.getFileSystemLocation(), StandardCopyOption.REPLACE_EXISTING);

                    // and change the mime type to "tabular" on the final datafile, 
                    // and replace (or add) the extension ".tab" to the filename: 
                    dataFile.setContentType(MIME_TYPE_TAB);
                    dataFile.getFileMetadata().setLabel(FileUtil.replaceExtension(fileName, "tab"));

                    dataFile.setDataTable(tabDataIngest.getDataTable());
                    tabDataIngest.getDataTable().setDataFile(dataFile);

                    produceSummaryStatistics(dataFile);

                    dataFile.setIngestDone();
                    dataFile = fileService.save(dataFile);
                    FacesMessage facesMessage = new FacesMessage("ingest done");
                    pushContext.push("/ingest" + dataFile.getOwner().getId(), facesMessage);
                    Logger.getLogger(IngestServiceBean.class.getName()).log(Level.INFO, "Ingest (" + dataFile.getFileMetadata().getDescription() + "); sent push notification to the page.");

                //try {
                    //} catch (IOException sumStatEx) {
                    //    dataFile.SetIngestProblem();
                    //    dataFile = fileService.save(dataFile);
                    //    FacesMessage facesMessage = new FacesMessage("ingest failed");
                    //    pushContext.push("/ingest"+dataFile.getOwner().getId(), facesMessage);
                    //    Logger.getLogger(IngestServiceBean.class.getName()).log(Level.INFO, "Ingest failure: Sent push notification to the page.");
                    //    throw new IOException ("Ingest: failed to calculate summary statistics. "+sumStatEx.getMessage());
                    //}
                    ingestSuccessful = true;
                }
            } else {
                Logger.getLogger(IngestServiceBean.class.getName()).log(Level.INFO, "Ingest failed to produce data obect; notification NOT sent to the page.");
            }
        } catch (IOException postIngestEx) {
            // TODO: 
            // try to separate the post-processing (summary stats, unfs) failures
            // from file save errors;
            // -- L.A. Aug. 2014
            dataFile.SetIngestProblem();
            errorReport = new IngestReport();
            errorReport.setFailure();
            errorReport.setReport("Ingest failed to produce Summary Statistics and/or UNF signatures; "+postIngestEx.getMessage());
            errorReport.setDataFile(dataFile);
            dataFile.setIngestReport(errorReport);
            
            dataFile = fileService.save(dataFile);
            FacesMessage facesMessage = new FacesMessage("ingest failed");
            pushContext.push("/ingest" + dataFile.getOwner().getId(), facesMessage);
            Logger.getLogger(IngestServiceBean.class.getName()).log(Level.INFO, "Ingest failure: post-ingest tasks. Sent push notification to the page.");
        } catch (Exception unknownEx) {
            // this probably means that an error occurred while saving the datafile
            // in the database. 
            Logger.getLogger(IngestServiceBean.class.getName()).log(Level.INFO, "Ingest failure: Failed to save tabular data (datatable, datavariables, etc.) in the database. Clearing the datafile object.");

            dataFile = null; 
            dataFile = fileService.find(datafile_id);
            
            if (dataFile != null) {
                dataFile.SetIngestProblem();
                errorReport = new IngestReport();
                errorReport.setFailure();
                errorReport.setReport("Ingest produced tabular data, but failed to save it in the database; " + unknownEx.getMessage() + " No further information is available.");
                errorReport.setDataFile(dataFile);
                dataFile.setIngestReport(errorReport);

            // blank the datatable that may have already been attached to the
                // datafile (it may have something "unsave-able" in it!)
                dataFile.setDataTables(null);
                if (tabDataIngest != null && tabDataIngest.getDataTable() != null) {
                    tabDataIngest.getDataTable().setDataFile(null);
                }

                ////try {
                dataFile = fileService.save(dataFile);
                ////} catch (Exception unknownEx2) {
                ////    logger.info("Another unknown exception occured while saving the datafile.");
                ////}
                FacesMessage facesMessage = new FacesMessage("ingest failed");
                pushContext.push("/ingest" + dataFile.getOwner().getId(), facesMessage);
                logger.info("Sent push notification to the page.");
            } else {
                // ??
            }
        }

        
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

        if (mimeType.equals(MIME_TYPE_STATA)) {
            ingestPlugin = new DTAFileReader(new DTAFileReaderSpi());
        } else if (mimeType.equals(MIME_TYPE_RDATA)) {
            ingestPlugin = new RDATAFileReader(new RDATAFileReaderSpi());
        } else if (mimeType.equals(MIME_TYPE_CSV) || mimeType.equals(MIME_TYPE_CSV_ALT)) {
            ingestPlugin = new CSVFileReader(new CSVFileReaderSpi());
        } else if (mimeType.equals(MIME_TYPE_XLSX)) {
            ingestPlugin = new XLSXFileReader(new XLSXFileReaderSpi());
        } else if (mimeType.equals(MIME_TYPE_SPSS_SAV)) {
            ingestPlugin = new SAVFileReader(new SAVFileReaderSpi());
        } else if (mimeType.equals(MIME_TYPE_SPSS_POR)) {
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
    /*
     dead code (?)
    private void calculateContinuousSummaryStatistics(DataFile dataFile, Double[][] dataVectors) throws IOException {
        int k = 0;
        for (int i = 0; i < dataFile.getDataTable().getVarQuantity(); i++) {
            if ("continuous".equals(dataFile.getDataTable().getDataVariables().get(i).getVariableIntervalType().getName())) {
                double[] sumStats = SumStatCalculator.calculateSummaryStatistics(dataVectors[k++]);

                assignContinuousSummaryStatistics(dataFile.getDataTable().getDataVariables().get(i), sumStats);
            }
        }
    }
    */
    
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
    
    private void calculateUNF(DataFile dataFile, int varnum, Double[] dataVector) {
        String unf = null;
        try {
            unf = UNF5Util.calculateUNF(dataVector);
        } catch (IOException iex) {
            logger.warning("exception thrown when attempted to calculate UNF signature for (numeric, continuous) variable " + varnum);
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
            unf = UNF5Util.calculateUNF(dataVector);
        } catch (IOException iex) {
            logger.warning("exception thrown when attempted to calculate UNF signature for (numeric, discrete) variable " + varnum);
        }
        if (unf != null) {
            dataFile.getDataTable().getDataVariables().get(varnum).setUnf(unf);
        } else {
            logger.warning("failed to calculate UNF signature for variable " + varnum);
        }
    }
    
    private void calculateUNF(DataFile dataFile, int varnum, String[] dataVector) {
        String unf = null;
        
        String[] dateFormats = null; 
        
        // Special handling for Character strings that encode dates and times:
        
        if ("time".equals(dataFile.getDataTable().getDataVariables().get(varnum).getFormatCategory())) {
            dateFormats = new String[dataVector.length];
            String savedDateFormat = dataFile.getDataTable().getDataVariables().get(varnum).getFormatSchemaName();
            for (int i = 0; i < dataVector.length; i++) {
                if (dataVector[i] != null) {
                    if (savedDateFormat != null && !savedDateFormat.equals("")) {
                        dateFormats[i] = savedDateFormat;
                    } else {
                        dateFormats[i] = dateTimeFormat_ymdhmsS;
                    }
                }
            }
        } else if ("date".equals(dataFile.getDataTable().getDataVariables().get(varnum).getFormatCategory())) {
            dateFormats = new String[dataVector.length];
            String savedDateFormat = dataFile.getDataTable().getDataVariables().get(varnum).getFormatSchemaName();
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
                unf = UNF5Util.calculateUNF(dataVector);
            } else {
                unf = UNF5Util.calculateUNF(dataVector, dateFormats);
            }
        } catch (IOException iex) {
            logger.warning("exception thrown when attempted to calculate UNF signature for (character) variable " + varnum);
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
            unf = UNF5Util.calculateUNF(dataVector);
        } catch (IOException iex) {
            logger.warning("exception thrown when attempted to calculate UNF signature for numeric, \"continuous\" (float) variable " + varnum);
        }
        if (unf != null) {
            dataFile.getDataTable().getDataVariables().get(varnum).setUnf(unf);
        } else {
            logger.warning("failed to calculate UNF signature for variable " + varnum);
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
                        
                        if ("continuous".equals(dataTable.getDataVariables().get(i).getVariableIntervalType().getName())) {
                            vartype = "numeric-continuous";
                        } else {
                            if ("numeric".equals(dataTable.getDataVariables().get(i).getVariableFormatType().getName())) {
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
}
