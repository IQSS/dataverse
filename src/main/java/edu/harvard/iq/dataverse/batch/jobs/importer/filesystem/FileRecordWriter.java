/*
   Copyright (C) 2005-2017, by the President and Fellows of Harvard College.

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
*/

package edu.harvard.iq.dataverse.batch.jobs.importer.filesystem;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFile.ChecksumType;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetVersionCommand;
import edu.harvard.iq.dataverse.util.FileUtil;

import javax.annotation.PostConstruct;
import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.AbstractItemWriter;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.ejb.EJB;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;

@Named
@Dependent
public class FileRecordWriter extends AbstractItemWriter {
    
    @Inject
    JobContext jobContext;

    @Inject
    StepContext stepContext;

    @Inject
    @BatchProperty
    String checksumType;

    @EJB
    DatasetServiceBean datasetServiceBean;

    @EJB
    AuthenticationServiceBean authenticationServiceBean;
    
    @EJB
    DataFileServiceBean dataFileServiceBean;

    @EJB
    EjbDataverseEngine commandEngine;

    Dataset dataset;
    AuthenticatedUser user;
    private String persistentUserData = "";
    private static Logger jobLogger;
    int fileCount;
    String fileMode; 

    public static String FILE_MODE_INDIVIDUAL_FILES = "individual_files";
    public static String FILE_MODE_PACKAGE_FILE = "package_file";
    
    
    
    @PostConstruct
    public void init() {
        JobOperator jobOperator = BatchRuntime.getJobOperator();
        Properties jobParams = jobOperator.getParameters(jobContext.getInstanceId());
        dataset = datasetServiceBean.findByGlobalId(jobParams.getProperty("datasetId"));
        user = authenticationServiceBean.getAuthenticatedUser(jobParams.getProperty("userId"));
        jobLogger = Logger.getLogger("job-"+Long.toString(jobContext.getInstanceId()));
        fileCount = ((HashMap<String, String>) jobContext.getTransientUserData()).size();
        fileMode = jobParams.getProperty("fileMode");
    }
    
    @Override
    public void open(Serializable checkpoint) throws Exception {
        // no-op    
    }

    @Override
    public void close() {
        // no-op
    }

    @Override
    public void writeItems(List list) {
        if (!list.isEmpty()) {
            if (FILE_MODE_INDIVIDUAL_FILES.equals(fileMode)) {
                List<DataFile> datafiles = dataset.getFiles();
                for (Object file : list) {
                    DataFile df = createDataFile((File) file);
                    if (df != null) {
                        // log success if the dataset isn't huge
                        if (fileCount < 20000) {
                            jobLogger.log(Level.INFO, "Creating DataFile for: " + ((File) file).getAbsolutePath());
                        }
                        datafiles.add(df);
                    } else {
                        jobLogger.log(Level.SEVERE, "Unable to create DataFile for: " + ((File) file).getAbsolutePath());
                    }
                }
                dataset.getLatestVersion().getDataset().setFiles(datafiles);
            } else if (FILE_MODE_PACKAGE_FILE.equals(fileMode)) {
                DataFile packageFile = createPackageDataFile(list);
                if (packageFile == null) {
                    jobLogger.log(Level.SEVERE, "File package import failed.");
                    jobContext.setExitStatus("FAILED");
                    return;
                }
                updateDatasetVersion(dataset.getLatestVersion());
            } else {
                jobLogger.log(Level.SEVERE, "File mode "+fileMode+" is not supported.");
                jobContext.setExitStatus("FAILED");
            }
        } else {
            jobLogger.log(Level.SEVERE, "No items in the writeItems list.");
        }
    }
    
    // utils
    /**
     * Update the dataset version using the command engine so permissions and constraints are enforced.
     * Log errors to both the glassfish log and inside the job step's persistentUserData
     * 
     * @param version dataset version
     *        
     */
    private void updateDatasetVersion(DatasetVersion version) {
    
        // update version using the command engine to enforce user permissions and constraints
        if (dataset.getVersions().size() == 1 && version.getVersionState() == DatasetVersion.VersionState.DRAFT) {
            try {
                Command<DatasetVersion> cmd;
                cmd = new UpdateDatasetVersionCommand(new DataverseRequest(user, (HttpServletRequest) null), version);
                commandEngine.submit(cmd);
            } catch (CommandException ex) {
                String commandError = "CommandException updating DatasetVersion from batch job: " + ex.getMessage();
                jobLogger.log(Level.SEVERE, commandError);
                persistentUserData += commandError + " ";
            }
        } else {
            String constraintError = "ConstraintException updating DatasetVersion form batch job: dataset must be a "
                    + "single version in draft mode.";
            jobLogger.log(Level.SEVERE, constraintError);
            persistentUserData += constraintError + " ";
        }
       
    }
    
    /**
     * Import the supplied batch of files as a single "package file" DataFile 
     * (basically, a folder/directory, with the single associated DataFile/FileMetadata, etc.)
     * and add it to the
     * latest dataset version 
     * @param files list of files, already copied to the dataset directory by rsync or otherwise. 
     * @return datafile
     * 
     * Consider: 
     * instead of expecting to have an extra top-level directory/folder to be 
     * present already, generate it here (using the standard code used for generating
     * storage identifiers for "normal" files), create it as a directory, and move
     * all the supplied files there. [DONE]
     */
    private DataFile createPackageDataFile(List<File> files) {
        DataFile packageFile = new DataFile(DataFileServiceBean.MIME_TYPE_PACKAGE_FILE);
        FileUtil.generateStorageIdentifier(packageFile);
        String datasetDirectory = null;
        long totalSize = 0L;
        String combinedChecksums = null;

        String gid = dataset.getAuthority() + dataset.getDoiSeparator() + dataset.getIdentifier();
        
        packageFile.setChecksumType(DataFile.ChecksumType.SHA1); // initial default

        // check system property first, otherwise use the batch job property:
        String jobChecksumType;
        if (System.getProperty("checksumType") != null) {
            jobChecksumType = System.getProperty("checksumType");
        } else {
            jobChecksumType = checksumType;
        }

        for (DataFile.ChecksumType type : DataFile.ChecksumType.values()) {
            if (jobChecksumType.equalsIgnoreCase(type.name())) {
                packageFile.setChecksumType(type);
                break;
            }
        }

        for (File file : files) {
            String path = file.getAbsolutePath();
            String relativePath = path.substring(path.indexOf(gid) + gid.length() + 1);
            
            // All the files have been moved into the same final destination folder by now; so 
            // the datasetDirectory needs to be initialized only once: 
            if (datasetDirectory == null) {
                datasetDirectory = path.substring(0, path.indexOf(gid) + gid.length() + 1);
            }

            totalSize += file.length();

            if (file.renameTo(new File(datasetDirectory + File.separator + packageFile.getStorageIdentifier() + File.separator + relativePath))) {
                jobLogger.log(Level.SEVERE, "Could not move the file to the final destination: " + datasetDirectory + File.separator + packageFile.getStorageIdentifier() + File.separator + relativePath);
                jobContext.setExitStatus("FAILED");
                return null;
            }
            
            String checksumValue;

            // lookup the checksum value in the job's manifest hashmap
            if (jobContext.getTransientUserData() != null) {
                checksumValue = ((HashMap<String, String>) jobContext.getTransientUserData()).get(relativePath);
                if (checksumValue != null) {
                    // remove the key, so we can check for unused checksums when the job is complete
                    ((HashMap<String, String>) jobContext.getTransientUserData()).remove(relativePath);

                    if (combinedChecksums == null) {
                        combinedChecksums = checksumValue;
                    } else {
                        combinedChecksums = combinedChecksums.concat("\n").concat(checksumValue);
                    }
                } else {
                    jobLogger.log(Level.SEVERE, "Unable to find checksum in manifest for: " + file.getAbsolutePath());
                }
            } else {
                jobLogger.log(Level.SEVERE, "No checksum hashmap found in transientUserData");
                jobContext.setExitStatus("FAILED");
                return null;
            }

        }
        
        if (combinedChecksums != null) {
            // calculate the "product checksum" for the package: 
            
            try {
                packageFile.setChecksumValue(calculateProductChecksum(combinedChecksums, packageFile.getChecksumType()));
            } catch (NoSuchAlgorithmException nsae) {
                
                jobLogger.log(Level.SEVERE, "No such checksum algorithm: "+packageFile.getContentType());
                jobContext.setExitStatus("FAILED");
                return null;
            }
        }
                
        packageFile.setFilesize(totalSize);
        packageFile.setModificationTime(new Timestamp(new Date().getTime()));
        packageFile.setCreateDate(new Timestamp(new Date().getTime()));
        packageFile.setPermissionModificationTime(new Timestamp(new Date().getTime()));
        packageFile.setOwner(dataset);
        dataset.getFiles().add(packageFile);

        packageFile.setIngestDone();

        // set metadata and add to latest version
        FileMetadata fmd = new FileMetadata();
        fmd.setLabel(packageFile.getStorageIdentifier());
        
        fmd.setDataFile(packageFile);
        packageFile.getFileMetadatas().add(fmd);
        if (dataset.getLatestVersion().getFileMetadatas() == null) dataset.getLatestVersion().setFileMetadatas(new ArrayList<>());
        
        dataset.getLatestVersion().getFileMetadatas().add(fmd);
        fmd.setDatasetVersion(dataset.getLatestVersion());

        
        return packageFile;
    }
    
    /**
     * Create a DatasetFile and corresponding FileMetadata for a file on the filesystem and add it to the
     * latest dataset version (if the user has AddDataset permissions for the dataset).
     * @param file file to create dataFile from
     * @return datafile
     */
    private DataFile createDataFile(File file) {
        
        DatasetVersion version = dataset.getLatestVersion();
        String path = file.getAbsolutePath();
        String gid = dataset.getAuthority() + dataset.getDoiSeparator() + dataset.getIdentifier();
        String relativePath = path.substring(path.indexOf(gid) + gid.length() + 1);
        
        DataFile datafile = new DataFile("application/octet-stream"); // we don't determine mime type
        datafile.setStorageIdentifier(relativePath);
        datafile.setFilesize(file.length());
        datafile.setModificationTime(new Timestamp(new Date().getTime()));
        datafile.setCreateDate(new Timestamp(new Date().getTime()));
        datafile.setPermissionModificationTime(new Timestamp(new Date().getTime()));
        datafile.setOwner(dataset);
        datafile.setIngestDone();

        // check system property first, otherwise use the batch job property
        String jobChecksumType;
        if (System.getProperty("checksumType") != null) {
            jobChecksumType = System.getProperty("checksumType");
        } else {
            jobChecksumType = checksumType;
        }
        datafile.setChecksumType(DataFile.ChecksumType.SHA1); // initial default
        for (DataFile.ChecksumType type : DataFile.ChecksumType.values()) {
            if (jobChecksumType.equalsIgnoreCase(type.name())) {
                datafile.setChecksumType(type);
                break;
            }
        }
        // lookup the checksum value in the job's manifest hashmap
        if (jobContext.getTransientUserData() != null) {
            String checksumVal = ((HashMap<String, String>) jobContext.getTransientUserData()).get(relativePath);
            if (checksumVal != null) {
                datafile.setChecksumValue(checksumVal);
                // remove the key, so we can check for unused checksums when the job is complete
                ((HashMap<String, String>) jobContext.getTransientUserData()).remove(relativePath);
            } else {
                datafile.setChecksumValue("Unknown");
                jobLogger.log(Level.SEVERE, "Unable to find checksum in manifest for: " + file.getAbsolutePath());
            }
        } else {
            jobLogger.log(Level.SEVERE, "No checksum hashmap found in transientUserData");
            jobContext.setExitStatus("FAILED");
            return null;
        }

        // set metadata and add to latest version
        FileMetadata fmd = new FileMetadata();
        fmd.setLabel(file.getName());
        // set the subdirectory if there is one
        if (relativePath.contains(File.separator)) {
            fmd.setDirectoryLabel(relativePath.replace(File.separator + file.getName(), ""));
        }
        fmd.setDataFile(datafile);
        datafile.getFileMetadatas().add(fmd);
        if (version.getFileMetadatas() == null) version.setFileMetadatas(new ArrayList<>());
        version.getFileMetadatas().add(fmd);
        fmd.setDatasetVersion(version);

        datafile = dataFileServiceBean.save(datafile);
        return datafile;
    }

    
    private String calculateProductChecksum (String combinedChecksums, ChecksumType type) throws NoSuchAlgorithmException {
        // calculate the "product checksum" for the package: 
            
            MessageDigest md = null;
            try {
                // Use "SHA-1" (toString) rather than "SHA1", for example.
                md = MessageDigest.getInstance(type.toString());
            } catch (NoSuchAlgorithmException e) {
                jobLogger.log(Level.SEVERE, "No such checksum algorithm: "+type);
                jobContext.setExitStatus("FAILED");
                return null;
            }

            md.update(combinedChecksums.getBytes());
            
            // convert the message digest bytes into a string: 
            byte[] mdbytes = md.digest();
            StringBuilder sb = new StringBuilder("");
            for (int i = 0; i < mdbytes.length; i++) {
                sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
            }
            
            return sb.toString();
    }
}