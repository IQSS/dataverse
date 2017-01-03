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
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;

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
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    Dataset dataset;
    Logger jobLogger;
    int fileCount;

    @PostConstruct
    public void init() {
        JobOperator jobOperator = BatchRuntime.getJobOperator();
        Properties jobParams = jobOperator.getParameters(jobContext.getInstanceId());
        dataset = datasetServiceBean.findByGlobalId(jobParams.getProperty("datasetId"));
        jobLogger = Logger.getLogger("job-"+Long.toString(jobContext.getInstanceId()));
        fileCount = ((HashMap<String, String>) jobContext.getTransientUserData()).size();
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
        } else {
            jobLogger.log(Level.SEVERE, "No items in the writeItems list.");
        }
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

}