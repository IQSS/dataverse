package edu.harvard.iq.dataverse.batch.jobs.importer.filesystem;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.batch.jobs.importer.ImportMode;

import javax.annotation.PostConstruct;
import javax.batch.api.chunk.ItemProcessor;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.context.JobContext;
import javax.ejb.EJB;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;


@Named
@Dependent
public class FileRecordProcessor implements ItemProcessor {

    private static final Logger logger = Logger.getLogger(FileRecordProcessor.class.getName());

    @Inject
    JobContext jobContext;

    @EJB
    DataFileServiceBean dataFileServiceBean;
    
    @EJB
    PermissionServiceBean permissionServiceBean;

    @EJB
    AuthenticationServiceBean authenticationServiceBean;

    @EJB
    DatasetServiceBean datasetServiceBean;

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
    public Object processItem(Object object) throws Exception {
        
        String path = object.toString();
        String gid = dataset.getAuthority() + dataset.getDoiSeparator() + dataset.getIdentifier();
        String relativePath = path.substring(path.indexOf(gid) + gid.length() + 1);
        
        // if mode = MERGE, this will skip any datafiles already referenced by filemetadata
        // if mode = UPDATE or REPLACE, the filemetadata list will already be empty at this point
        for (FileMetadata fmd : dataset.getLatestVersion().getFileMetadatas()) {
            if (fmd.getDataFile().getStorageIdentifier().equalsIgnoreCase(relativePath)) {
                logger.log(Level.FINE, "Skipping: " + relativePath + " since it is already in the file metadata list.");
                return null;
            }
        }
        return createDataFile(new File(path));
    }

    /**
     * Create a DatasetFile and corresponding FileMetadata for a file on the filesystem and add it to the
     * latest dataset version (if the user has AddDataset permissions for the dataset).
     * @param file
     * @return datafile
     */
    private DataFile createDataFile(File file) {

        if (permissionServiceBean.userOn(user, dataset.getOwner()).has(Permission.AddDataset) &&
                dataset.getLatestVersion().getVersionState() == DatasetVersion.VersionState.DRAFT) {
            
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

            datafile = dataFileServiceBean.save(datafile);
            return datafile;
            
        } else {
            return null;
        }

    }
}
