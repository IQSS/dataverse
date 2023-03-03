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

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.batch.jobs.importer.ImportMode;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import org.apache.commons.io.filefilter.NotFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import javax.annotation.PostConstruct;
import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.AbstractItemReader;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.ejb.EJB;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.FileFilter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

@Named
@Dependent
public class FileRecordReader extends AbstractItemReader {
    
    public static final String SEP = File.separator;

    @Inject
    JobContext jobContext;

    @Inject
    StepContext stepContext;

    @Inject
    @BatchProperty
    String excludes;
    
    @EJB
    DatasetServiceBean datasetServiceBean;

    @EJB
    AuthenticationServiceBean authenticationServiceBean;

    File directory;
    List<File> files;
    Iterator<File> iterator;

    long currentRecordNumber;
    long totalRecordNumber;

    Dataset dataset;
    AuthenticatedUser user;
    String mode = ImportMode.MERGE.name();
    String uploadFolder;

    @PostConstruct
    public void init() {
        JobOperator jobOperator = BatchRuntime.getJobOperator();
        Properties jobParams = jobOperator.getParameters(jobContext.getInstanceId());
        dataset = datasetServiceBean.find(new Long(jobParams.getProperty("datasetId")));
        user = authenticationServiceBean.getAuthenticatedUser(jobParams.getProperty("userId"));
        mode = jobParams.getProperty("mode");
        uploadFolder = jobParams.getProperty("uploadFolder");
    }

    @Override
    public void open(Serializable checkpoint) throws Exception {
    
        // Retrieve via MPCONFIG. Has sane default /tmp/dataverse from META-INF/microprofile-config.properties
        String baseDir = JvmSettings.FILES_DIRECTORY.lookup();
        
        directory = new File(baseDir + SEP + dataset.getAuthority() + SEP + dataset.getIdentifier() + SEP + uploadFolder);
        // TODO: 
        // The above goes directly to the filesystem directory configured by the 
        // old "dataverse.files.directory" JVM option (otherwise used for temp
        // files only, after the Multistore implementation (#6488). 
        // We probably want package files to be able to use specific stores instead.
        // More importantly perhaps, the approach above does not take into account
        // if the dataset may have an AlternativePersistentIdentifier, that may be 
        // designated isStorageLocationDesignator() - i.e., if a different identifer
        // needs to be used to name the storage directory, instead of the main/current
        // persistent identifier above. 
        getJobLogger().log(Level.INFO, "Reading dataset directory: " + directory.getAbsolutePath() 
                + " (excluding: " + excludes + ")");
        if (isValidDirectory(directory)) {
            files = getFiles(directory);
            iterator = files.listIterator();
            currentRecordNumber = 0;
            totalRecordNumber = (long) files.size();
            getJobLogger().log(Level.INFO, "Files found = " + totalRecordNumber);
            // report if checksum total not equal to file total
            int checksumCount = ((HashMap<String, String>) jobContext.getTransientUserData()).size();
            if (checksumCount != files.size()) {
                getJobLogger().log(Level.SEVERE, "Checksum mismatch: " + checksumCount + " checksums found in the manifest "
                        + "and " + files.size() + " files found in the dataset directory.");
            }

        } else {
            stepContext.setExitStatus("FAILED");
        }
    }

    @Override
    public void close() {
        getJobLogger().log(Level.INFO, "Files read  = " + currentRecordNumber);
    }

    @Override
    public File readItem() {
        if (iterator.hasNext()) {
            currentRecordNumber++;
            return iterator.next();
        }
        return null;
    }

    /**
     * Get the list of files in the directory, minus any in the skip list.
     * @param directory directory where dataset files can be found
     * @return list of files
     */
    private List<File> getFiles(final File directory) {
        // create filter from job xml excludes property
        FileFilter excludeFilter = new NotFileFilter(new WildcardFileFilter(Arrays.asList(excludes.split("\\s*,\\s*"))));
        List<File> files = new ArrayList<>();
        File[] filesList = directory.listFiles(excludeFilter);
        if (filesList != null) {
            for (File file : filesList) {
                if (file.isFile()) {
                    files.add(file);
                } else {
                    files.addAll(getFiles(file));
                }
            }
        }
        return files;
    }

    /**
     * Make sure the directory path is truly a directory, exists and we can read it.
     * @return isValid
     */
    private boolean isValidDirectory(File directory) {
        String path = directory.getAbsolutePath();
        if (!directory.exists()) {
            getJobLogger().log(Level.SEVERE, "Directory " + path + " does not exist.");
            return false;
        }
        if (!directory.isDirectory()) {
            getJobLogger().log(Level.SEVERE, path + " is not a directory.");
            return false;
        }
        if (!directory.canRead()) {
            getJobLogger().log(Level.SEVERE, "Unable to read files from directory " + path + ". Permission denied.");
            return false;
        }
        return true;
    }
    
    private Logger getJobLogger() {
        return Logger.getLogger("job-"+jobContext.getInstanceId());
    }
    
}
