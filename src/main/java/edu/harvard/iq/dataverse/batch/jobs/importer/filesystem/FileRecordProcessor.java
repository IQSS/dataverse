package edu.harvard.iq.dataverse.batch.jobs.importer.filesystem;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
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
        return new File(path);
    }

}
