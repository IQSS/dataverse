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
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetLock;
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
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
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
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import edu.harvard.iq.dataverse.GlobalIdServiceBean;

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
    
    @Inject
    @BatchProperty
    String checksumManifest;

    @EJB
    DatasetServiceBean datasetServiceBean;

    @EJB
    AuthenticationServiceBean authenticationServiceBean;
    
    @EJB
    SettingsServiceBean settingsService;
    
    @EJB
    DataFileServiceBean dataFileServiceBean;

    @EJB
    EjbDataverseEngine commandEngine;

    Dataset dataset;
    AuthenticatedUser user;
    int fileCount;
    String fileMode; 
    Long suppliedSize = null;
    String uploadFolder; 

    public static String FILE_MODE_INDIVIDUAL_FILES = "individual_files";
    public static String FILE_MODE_PACKAGE_FILE = "package_file";
    
    @PostConstruct
    public void init() {
        JobOperator jobOperator = BatchRuntime.getJobOperator();
        Properties jobParams = jobOperator.getParameters(jobContext.getInstanceId());
        dataset = datasetServiceBean.find(Long.parseLong(jobParams.getProperty("datasetId")));
        user = authenticationServiceBean.getAuthenticatedUser(jobParams.getProperty("userId"));
        //jobLogger = Logger.getLogger("job-"+Long.toString(jobContext.getInstanceId()));
        fileCount = ((Map<String, String>) jobContext.getTransientUserData()).size();
        fileMode = jobParams.getProperty("fileMode");
        uploadFolder = jobParams.getProperty("uploadFolder");
        if (jobParams.getProperty("totalSize") != null) {
            try { 
                suppliedSize = new Long(jobParams.getProperty("totalSize"));
                getJobLogger().log(Level.INFO, "Size parameter supplied: "+suppliedSize);
            } catch (NumberFormatException ex) {
                getJobLogger().log(Level.WARNING, "Invalid file size supplied (in FileRecordWriter.init()): "+jobParams.getProperty("totalSize"));
                suppliedSize = null; 
            }
        }
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
                            getJobLogger().log(Level.INFO, "Creating DataFile for: " + ((File) file).getAbsolutePath());
                        }
                        datafiles.add(df);
                    } else {
                        getJobLogger().log(Level.SEVERE, "Unable to create DataFile for: " + ((File) file).getAbsolutePath());
                    }
                }
                dataset.getLatestVersion().getDataset().setFiles(datafiles);
            } else if (FILE_MODE_PACKAGE_FILE.equals(fileMode)) {
                DataFile packageFile = createPackageDataFile(list);
                if (packageFile == null) {
                    getJobLogger().log(Level.SEVERE, "File package import failed.");
                    jobContext.setExitStatus("FAILED");
                    return;
                }
                DatasetLock dcmLock = dataset.getLockFor(DatasetLock.Reason.DcmUpload);
                if (dcmLock == null) {
                    getJobLogger().log(Level.WARNING, "Dataset not locked for DCM upload");
                } else {
                    datasetServiceBean.removeDatasetLocks(dataset, DatasetLock.Reason.DcmUpload);
                    dataset.removeLock(dcmLock);
                }
                updateDatasetVersion(dataset.getLatestVersion());
            } else {
                getJobLogger().log(Level.SEVERE, "File mode "+fileMode+" is not supported.");
                jobContext.setExitStatus("FAILED");
            }
        } else {
            getJobLogger().log(Level.SEVERE, "No items in the writeItems list.");
        }
    }
    
    // utils
    /**
     * Update the dataset version using the command engine so permissions and constraints are enforced.
     * Log errors to both the glassfish log and in the job context, as the exit status "failed". 
     * 
     * @param version dataset version
     *        
     */
    private void updateDatasetVersion(DatasetVersion version) {
    
        // update version using the command engine to enforce user permissions and constraints
        if (dataset.getVersions().size() == 1 && version.getVersionState() == DatasetVersion.VersionState.DRAFT) {
            try {
                Command<Dataset> cmd;
                cmd = new UpdateDatasetVersionCommand(version.getDataset(), new DataverseRequest(user, (HttpServletRequest) null));
                commandEngine.submit(cmd);
            } catch (CommandException ex) {
                String commandError = "CommandException updating DatasetVersion from batch job: " + ex.getMessage();
                getJobLogger().log(Level.SEVERE, commandError);
                jobContext.setExitStatus("FAILED");
            }
        } else {
            String constraintError = "ConstraintException updating DatasetVersion form batch job: dataset must be a "
                    + "single version in draft mode.";
            getJobLogger().log(Level.SEVERE, constraintError);
            jobContext.setExitStatus("FAILED");
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
     * all the supplied files there.l
     */
    private DataFile createPackageDataFile(List<File> files) {
        DataFile packageFile = new DataFile(DataFileServiceBean.MIME_TYPE_PACKAGE_FILE);
        FileUtil.generateStorageIdentifier(packageFile);
        
        String datasetDirectory = null;
        String folderName = null; 
        
        long totalSize;

        if (suppliedSize != null) {
            totalSize = suppliedSize;
        } else {
            totalSize = 0L;
        }
        
        String gid = dataset.getAuthority() + "/" + dataset.getIdentifier();
        
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
            // the folderName and datasetDirectory need to be initialized only once: 
            if (datasetDirectory == null && folderName == null) {
                datasetDirectory = path.substring(0, path.indexOf(gid) + gid.length() + 1);
                if (relativePath != null && relativePath.indexOf(File.separatorChar) > -1) {
                    folderName = relativePath.substring(0, relativePath.indexOf(File.separatorChar));
                } else {
                    getJobLogger().log(Level.SEVERE, "Invalid file package (files are not in a folder)");
                    jobContext.setExitStatus("FAILED");
                    return null;
                }
                if (!uploadFolder.equals(folderName)) {
                    getJobLogger().log(Level.SEVERE, "Folder name mismatch: "+uploadFolder+" expected, "+folderName+" found.");
                    jobContext.setExitStatus("FAILED");
                    return null;
                }
            }

            if (suppliedSize == null) {
                totalSize += file.length();
            }

            String checksumValue;

            // lookup the checksum value in the job's manifest hashmap
            if (jobContext.getTransientUserData() != null) {
                String manifestPath = relativePath.substring(folderName.length() + 1);
                checksumValue = ((Map<String, String>) jobContext.getTransientUserData()).get(manifestPath);
                if (checksumValue != null) {
                    // remove the key, so we can check for unused checksums when the job is complete
                    ((Map<String, String>) jobContext.getTransientUserData()).remove(manifestPath);

                } else {
                    getJobLogger().log(Level.WARNING, "Unable to find checksum in manifest for: " + file.getAbsolutePath());
                }
            } else {
                getJobLogger().log(Level.SEVERE, "No checksum hashmap found in transientUserData");
                jobContext.setExitStatus("FAILED");
                return null;
            }

        }
        
        // If the manifest file is present, calculate the checksum of the manifest 
        // and use it as the checksum of the datafile: 
        
        if (System.getProperty("checksumManifest") != null) {
            checksumManifest = System.getProperty("checksumManifest");
        }
        
        File checksumManifestFile = null; 
        if (checksumManifest != null && !checksumManifest.isEmpty()) {
            String checksumManifestPath = datasetDirectory + File.separator + folderName + File.separator + checksumManifest;
            checksumManifestFile = new File (checksumManifestPath);
        
            if (!checksumManifestFile.exists()) {
                getJobLogger().log(Level.WARNING, "Manifest file not found");
                // TODO: 
                // add code to generate the manifest, if not present? -- L.A. 
            } else {
                try {
                    packageFile.setChecksumValue(FileUtil.calculateChecksum(checksumManifestPath, packageFile.getChecksumType()));
                } catch (Exception ex) {
                    getJobLogger().log(Level.SEVERE, "Failed to calculate checksum (type "+packageFile.getChecksumType()+") "+ex.getMessage());
                    jobContext.setExitStatus("FAILED");
                    return null;
                }
            }
        } else {
            getJobLogger().log(Level.WARNING, "No checksumManifest property supplied");
        }
        
        // Move the folder to the final destination: 
        if (!(new File(datasetDirectory + File.separator + folderName).renameTo(new File(datasetDirectory + File.separator + packageFile.getStorageIdentifier())))) {
            getJobLogger().log(Level.SEVERE, "Could not move the file folder to the final destination (" + datasetDirectory + File.separator + packageFile.getStorageIdentifier() + ")");
            jobContext.setExitStatus("FAILED");
            return null;
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
        fmd.setLabel(folderName);
        
        fmd.setDataFile(packageFile);
        packageFile.getFileMetadatas().add(fmd);
        if (dataset.getLatestVersion().getFileMetadatas() == null) dataset.getLatestVersion().setFileMetadatas(new ArrayList<>());
        
        dataset.getLatestVersion().getFileMetadatas().add(fmd);
        fmd.setDatasetVersion(dataset.getLatestVersion());
        
    if (commandEngine.getContext().systemConfig().isFilePIDsEnabledForCollection(dataset.getOwner())) {

        GlobalIdServiceBean idServiceBean = GlobalIdServiceBean.getBean(packageFile.getProtocol(), commandEngine.getContext());
        if (packageFile.getIdentifier() == null || packageFile.getIdentifier().isEmpty()) {
            packageFile.setIdentifier(idServiceBean.generateDataFileIdentifier(packageFile));
        }
        String nonNullDefaultIfKeyNotFound = "";
        String protocol = commandEngine.getContext().settings().getValueForKey(SettingsServiceBean.Key.Protocol, nonNullDefaultIfKeyNotFound);
        String authority = commandEngine.getContext().settings().getValueForKey(SettingsServiceBean.Key.Authority, nonNullDefaultIfKeyNotFound);
        if (packageFile.getProtocol() == null) {
            packageFile.setProtocol(protocol);
        }
        if (packageFile.getAuthority() == null) {
            packageFile.setAuthority(authority);
        }

        if (!packageFile.isIdentifierRegistered()) {
            String doiRetString = "";
            idServiceBean = GlobalIdServiceBean.getBean(commandEngine.getContext());
            try {
                doiRetString = idServiceBean.createIdentifier(packageFile);
            } catch (Throwable e) {
                
            }

            // Check return value to make sure registration succeeded
            if (!idServiceBean.registerWhenPublished() && doiRetString.contains(packageFile.getIdentifier())) {
                packageFile.setIdentifierRegistered(true);
                packageFile.setGlobalIdCreateTime(new Date());
            }
        }
	}

        getJobLogger().log(Level.INFO, "Successfully created a file of type package");
        
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
        String gid = dataset.getAuthority() + "/" + dataset.getIdentifier();
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
            String checksumVal = ((Map<String, String>) jobContext.getTransientUserData()).get(relativePath);
            if (checksumVal != null) {
                datafile.setChecksumValue(checksumVal);
                // remove the key, so we can check for unused checksums when the job is complete
                ((Map<String, String>) jobContext.getTransientUserData()).remove(relativePath);
            } else {
                datafile.setChecksumValue("Unknown");
                getJobLogger().log(Level.WARNING, "Unable to find checksum in manifest for: " + file.getAbsolutePath());
            }
        } else {
            getJobLogger().log(Level.SEVERE, "No checksum hashmap found in transientUserData");
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
    
    private Logger getJobLogger() {
        return Logger.getLogger("job-"+jobContext.getInstanceId());
    }
    
}
