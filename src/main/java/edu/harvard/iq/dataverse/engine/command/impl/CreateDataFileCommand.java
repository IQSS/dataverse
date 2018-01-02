/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.emory.mathcs.backport.java.util.Collections;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.IdServiceBean;
import edu.harvard.iq.dataverse.Template;
import edu.harvard.iq.dataverse.api.imports.ImportUtil;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import edu.harvard.iq.dataverse.dataaccess.StorageIO;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.ingest.IngestUtil;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.FileUtil;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author rohit
 */
@RequiredPermissions(Permission.AddDataset)
public class CreateDataFileCommand extends AbstractCommand<DataFile>{

    private static final Logger logger = Logger.getLogger(CreateDataFileCommand.class.getCanonicalName());
    private final DataFile theDataFile;
    private final DatasetVersion version;
    private final String identifier;
       
  

    public CreateDataFileCommand(DataFile theDataFile,DatasetVersion version, DataverseRequest aRequest) {
        super(aRequest, theDataFile.getOwner());
        this.theDataFile = theDataFile;
        this.version=version;  
        this.identifier = null; 
    }
    
    
    public CreateDataFileCommand(DataFile theDataFile,DatasetVersion version, DataverseRequest aRequest, String identifier) {
        super(aRequest, theDataFile.getOwner());
        this.theDataFile = theDataFile;
        this.version=version; 
        this.identifier = identifier; 
    }
    
    
  
    @Override
    public DataFile execute(CommandContext ctxt) throws CommandException {
       
                IngestUtil.checkForDuplicateFileNamesFinal(version,Collections.singletonList(theDataFile));
//                DatasetVersion version= theDataFile.getOwner().getLatestVersion();
                Dataset dataset = version.getDataset();
                
                    String tempFileLocation = FileUtil.getFilesTempDirectory() + "/" + theDataFile.getStorageIdentifier();

                    // These are all brand new files, so they should all have 
                    // one filemetadata total. -- L.A. 
                    FileMetadata fileMetadata = theDataFile.getFileMetadatas().get(0);
                    String fileName = fileMetadata.getLabel();
                    
                    
                    // Make sure the file is attached to the dataset and to the version, if this 
                    // hasn't been done yet:
                    if (theDataFile.getOwner() == null) {
                        theDataFile.setOwner(dataset);
                    
                        version.getFileMetadatas().add(theDataFile.getFileMetadata());
                        theDataFile.getFileMetadata().setDatasetVersion(version);
                        dataset.getFiles().add(theDataFile);
                    }
                    
                    IdServiceBean idServiceBean = IdServiceBean.getBean(theDataFile.getProtocol(),ctxt);
                    if(theDataFile.getIdentifier()==null || theDataFile.getIdentifier().isEmpty())
                    {
                        if (this.identifier == null){
                            theDataFile.setIdentifier(ctxt.files().generateDataFileIdentifier(theDataFile, idServiceBean));
                        } else {
                            theDataFile.setIdentifier(this.identifier);
                        }
                    }
                    String nonNullDefaultIfKeyNotFound = "";
                    String    protocol = ctxt.settings().getValueForKey(SettingsServiceBean.Key.Protocol, nonNullDefaultIfKeyNotFound);
                    String    authority = ctxt.settings().getValueForKey(SettingsServiceBean.Key.Authority, nonNullDefaultIfKeyNotFound);
                    String    doiSeparator = ctxt.settings().getValueForKey(SettingsServiceBean.Key.DoiSeparator, nonNullDefaultIfKeyNotFound);
                    if (theDataFile.getProtocol()==null) theDataFile.setProtocol(protocol);
                    if (theDataFile.getAuthority()==null) theDataFile.setAuthority(authority);
                    if (theDataFile.getDoiSeparator()==null) theDataFile.setDoiSeparator(doiSeparator);
                    
        if (!theDataFile.isIdentifierRegistered()) {
            String doiRetString = "";
            idServiceBean = IdServiceBean.getBean(ctxt);
            try {
                logger.log(Level.FINE, "creating identifier");
                doiRetString = idServiceBean.createIdentifier(theDataFile);
            } catch (Throwable e) {
                logger.log(Level.WARNING, "Exception while creating Identifier: " + e.getMessage(), e);
            }

            // Check return value to make sure registration succeeded
            if (!idServiceBean.registerWhenPublished() && doiRetString.contains(theDataFile.getIdentifier())) {
                theDataFile.setIdentifierRegistered(true);
                theDataFile.setGlobalIdCreateTime(new Date());
            }
        }
                    
                    
                    
                    boolean metadataExtracted = false;
                    
                    if (FileUtil.ingestableAsTabular(theDataFile)) {
                        /*
                         * Note that we don't try to ingest the file right away - 
                         * instead we mark it as "scheduled for ingest", then at 
                         * the end of the save process it will be queued for async. 
                         * ingest in the background. In the meantime, the file 
                         * will be ingested as a regular, non-tabular file, and 
                         * appear as such to the user, until the ingest job is
                         * finished with the Ingest Service.
                         */
                        theDataFile.SetIngestScheduled();
                    } else if (ctxt.ingest().fileMetadataExtractable(theDataFile)) {

                        try {
                            // FITS is the only type supported for metadata 
                            // extraction, as of now. -- L.A. 4.0 
                            theDataFile.setContentType("application/fits");
                            metadataExtracted = ctxt.ingest().extractMetadata(tempFileLocation, theDataFile, version);
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
                    
                    String storageId = theDataFile.getStorageIdentifier().replaceFirst("^tmp://", "");
                    
                    
                    Path tempLocationPath = Paths.get(FileUtil.getFilesTempDirectory() + "/" + storageId);
                    WritableByteChannel writeChannel = null;
                    FileChannel readChannel = null;
                    
                    boolean localFile = false;
                    boolean savedSuccess = false; 
                    StorageIO<DataFile> dataAccess = null;
                    
                    try {

                        logger.fine("Attempting to create a new storageIO object for " + storageId);
                        dataAccess = DataAccess.createNewStorageIO(theDataFile, storageId);
                        
                        if (dataAccess.isLocalFile()) {
                            localFile = true; 
                        }

                        logger.fine("Successfully created a new storageIO object.");
                        
                        dataAccess.savePath(tempLocationPath);

                        // Set filesize in bytes 
                        theDataFile.setFilesize(dataAccess.getSize());
                        savedSuccess = true;
                        logger.fine("Success: permanently saved file "+theDataFile.getFileMetadata().getLabel());
                        
                    } catch (IOException ioex) {
                        logger.warning("Failed to save the file, storage id " + theDataFile.getStorageIdentifier() + " (" + ioex.getMessage() + ")");
                    } finally {
                        if (readChannel != null) {try{readChannel.close();}catch(IOException e){}}
                        if (writeChannel != null) {try{writeChannel.close();}catch(IOException e){}}
                    }

                    // Since we may have already spent some CPU cycles scaling down image thumbnails, 
                    // we may as well save them, by moving these generated images to the permanent 
                    // dataset directory. We should also remember to delete any such files in the
                    // temp directory:
                    
                    List<Path> generatedTempFiles = listGeneratedTempFiles(Paths.get(FileUtil.getFilesTempDirectory()), storageId);
                    if (generatedTempFiles != null) {
                        for (Path generated : generatedTempFiles) {
                            if (savedSuccess) { // && localFile) {
                                logger.fine("(Will also try to permanently save generated thumbnail file "+generated.toString()+")");
                                try {
                                    //Files.copy(generated, Paths.get(dataset.getFileSystemDirectory().toString(), generated.getFileName().toString()));
                                    int i = generated.toString().lastIndexOf("thumb");
                                    if (i > 1) {
                                        String extensionTag = generated.toString().substring(i);
                                        dataAccess.savePathAsAux(generated, extensionTag);
                                        logger.fine("Saved generated thumbnail as aux object. \"preview available\" status: "+theDataFile.isPreviewImageAvailable());
                                    } else {
                                        logger.warning("Generated thumbnail file name does not match the expected pattern: "+generated.toString());
                                    }
                                        
                                    
                                } catch (IOException ioex) {
                                    logger.warning("Failed to save generated file "+generated.toString());
                                }
                                
                                try {
                                    Files.delete(generated);
                                } catch (IOException ioex) {
                                    logger.warning("Failed to delete generated file "+generated.toString());
                                }
                            }
                        }
                    }
                    
                    try {
                        logger.fine("Will attempt to delete the temp file "+tempLocationPath.toString());
                        Files.delete(tempLocationPath);
                    } catch (IOException ex) {
                        // (non-fatal - it's just a temp file.)
                        logger.warning("Failed to delete temp file "+tempLocationPath.toString());
                    }
                    
                    // Any necessary post-processing: 
                    //performPostProcessingTasks(dataFile);
                
                logger.fine("Done! Finished saving new files in permanent storage.");
                
                
                return theDataFile;
    }
    


 private List<Path> listGeneratedTempFiles(Path tempDirectory, String baseName) {
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
}
