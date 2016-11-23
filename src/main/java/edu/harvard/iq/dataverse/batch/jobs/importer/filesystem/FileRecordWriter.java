package edu.harvard.iq.dataverse.batch.jobs.importer.filesystem;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IpAddress;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetVersionCommand;

import javax.batch.api.chunk.AbstractItemWriter;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;


@Named
@Dependent
public class FileRecordWriter extends AbstractItemWriter {
    
    private static final Logger logger = Logger.getLogger(FileRecordWriter.class.getName());

    @Inject
    JobContext jobContext;

    @Inject
    StepContext stepContext;

    @EJB
    DatasetServiceBean datasetServiceBean;

    @EJB
    AuthenticationServiceBean authenticationServiceBean;

    @EJB
    EjbDataverseEngine commandEngine;
    
    Dataset dataset;
    AuthenticatedUser user;
    private String persistentUserData = "";

    @Override
    public void open(Serializable checkpoint) throws Exception {
        JobOperator jobOperator = BatchRuntime.getJobOperator();
        Properties jobParams = jobOperator.getParameters(jobContext.getInstanceId());
        dataset = datasetServiceBean.findByGlobalId(jobParams.getProperty("datasetId"));
        user = authenticationServiceBean.getAuthenticatedUser(jobParams.getProperty("userId"));
    }

    @Override
    public void close() {
        if (!persistentUserData.isEmpty()) {
            stepContext.setPersistentUserData(persistentUserData);
        }
    }

    @Override
    public void writeItems(List list) {

        List<DataFile> datafiles = dataset.getFiles();
        for (Object file : list) {
            datafiles.add(createDataFile((File) file));
        }
        // update the dataset
        dataset.getLatestVersion().getDataset().setFiles(datafiles);
        updateDatasetVersion(dataset.getLatestVersion());

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
                cmd = new UpdateDatasetVersionCommand(new DataverseRequest(user, (IpAddress) null), version);
                commandEngine.submit(cmd);
            } catch (EJBException cause) {
                StringBuilder trace = new StringBuilder();
                for(StackTraceElement element:cause.getStackTrace()) {
                    trace.append(cause).append(" ").append(element).append(cause.getMessage()).append("\n");
                }
                String ejbError = "EJBException updating DatasetVersion from batch job: " + trace.toString();
                logger.log(Level.SEVERE, ejbError);
                persistentUserData += ejbError + " ";
            } catch (CommandException ex) {
                String commandError = "CommandException updating DatasetVersion from batch job: " + ex.getMessage();
                logger.log(Level.SEVERE, commandError);
                persistentUserData += commandError + " ";
            }
        } else {
            String constraintError = "ConstraintException updating DatasetVersion form batch job: dataset must be a "
                    + "single version in draft mode.";
            logger.log(Level.SEVERE, constraintError);
            persistentUserData += constraintError + " ";
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
            datafile.setChecksumType(DataFile.ChecksumType.SHA1);
            datafile.setChecksumValue("Unknown"); // only temporary since a checksum import job will run next

            // set metadata and add to latest version
            FileMetadata fmd = new FileMetadata();
            fmd.setLabel(file.getName());
            fmd.setDirectoryLabel(relativePath.replace(File.separator + file.getName(), ""));
            fmd.setDataFile(datafile);
            datafile.getFileMetadatas().add(fmd);
            if (version.getFileMetadatas() == null) version.setFileMetadatas(new ArrayList<>());
            version.getFileMetadatas().add(fmd);
            fmd.setDatasetVersion(version);

            return datafile;
    }
    
}