package edu.harvard.iq.dataverse.batch.jobs.importer.filesystem;

import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.UserServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.batch.jobs.importer.ImportMode;
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
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

@Named
@Dependent
public class FileRecordReader extends AbstractItemReader {

    private static final Logger logger = Logger.getLogger(FileRecordReader.class.getName());

    @Inject
    JobContext jobContext;

    @Inject
    StepContext stepContext;

    @Inject
    @BatchProperty
    String excludes;

    @EJB
    DatasetServiceBean datasetService;

    @EJB
    DataFileServiceBean fileService;

    @EJB
    UserServiceBean userServiceBean;

    @EJB
    PermissionServiceBean permissionServiceBean;

    @EJB
    DatasetServiceBean datasetServiceBean;

    @EJB
    AuthenticationServiceBean authenticationServiceBean;

    File directory;
    List<File> files;
    Iterator<File> iterator;

    long currentRecordNumber;
    long totalRecordNumber;
    private String persistentUserData = "";

    Dataset dataset;
    AuthenticatedUser user;
    String mode = ImportMode.MERGE.name();

    @PostConstruct
    public void init() {
        JobOperator jobOperator = BatchRuntime.getJobOperator();
        Properties jobParams = jobOperator.getParameters(jobContext.getInstanceId());
        dataset = datasetServiceBean.findByGlobalId(jobParams.getProperty("datasetId"));
        user = authenticationServiceBean.getAuthenticatedUser(jobParams.getProperty("userId"));
        mode = jobParams.getProperty("mode");
    }

    @Override
    public void open(Serializable checkpoint) throws Exception {
        directory = new File(System.getProperty("dataverse.files.directory")
                + File.separator + dataset.getAuthority() + File.separator + dataset.getIdentifier());
        if (isValidDirectory()) {
            files = getFiles(directory);
            iterator = files.listIterator();
            currentRecordNumber = 0;
            totalRecordNumber = (long) files.size();
        } else {
            stepContext.setExitStatus("FAILED");
        }
        if (!permissionServiceBean.userOn(user, dataset.getOwner()).has(Permission.AddDataset)) {
            logger.log(Level.SEVERE, "User doesn't have permission to import files into this dataset.");
            persistentUserData += "FAILED: User doesn't have permission to import files into this dataset.";
            stepContext.setExitStatus("FAILED");
        }
        if (dataset.getLatestVersion().getVersionState() != DatasetVersion.VersionState.DRAFT) {
            logger.log(Level.SEVERE, "File system import is currently only supported for DRAFT versions.");
            persistentUserData += "FAILED: File system import is currently only supported for DRAFT versions.";
            stepContext.setExitStatus("FAILED");
        }
    }

    @Override
    public void close() {
        if (!persistentUserData.isEmpty()) {
            stepContext.setPersistentUserData(persistentUserData);
        }
    }

    @Override
    public File readItem() {
        if (iterator.hasNext()) {
            currentRecordNumber++;
            logger.log(Level.FINE,
                    "Reading file " + Long.toString(currentRecordNumber) + " of " + Long.toString(totalRecordNumber));
            return iterator.next(); // skip if it's in the ignore list
        }
        return null;
    }

    /**
     * Get the list of files in the directory, minus any in the skip list.
     * @param directory
     * @return list of files
     */
    private List<File> getFiles(final File directory) {

        // create filter from excludes property
        FileFilter excludeFilter = new NotFileFilter(
                new WildcardFileFilter(Arrays.asList(excludes.split("\\s*,\\s*"))));

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
    private boolean isValidDirectory() {
        String path = directory.getAbsolutePath();
        if (!directory.exists()) {
            logger.log(Level.SEVERE, "Directory " + path + "does not exist.");
            persistentUserData += "FAILED: Directory " + path + "does not exist.";
            return false;
        }
        if (!directory.isDirectory()) {
            logger.log(Level.SEVERE, path + " is not a directory.");
            persistentUserData += "FAILED: " + path + " is not a directory.";
            return false;
        }
        if (!directory.canRead()) {
            logger.log(Level.SEVERE, "Unable to read files from directory " + path + ". Permission denied.");
            persistentUserData += "FAILED: Unable to read files from directory " + path + ". Permission denied.";
            return false;
        }
        return true;
    }

}
