package edu.harvard.iq.dataverse.batch.jobs.importer.filesystem;

import edu.harvard.iq.dataverse.DataFile;
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

import javax.batch.api.chunk.AbstractItemWriter;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.context.JobContext;
import javax.ejb.EJB;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.Serializable;
import java.util.ArrayList;
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

    @EJB
    DatasetServiceBean datasetServiceBean;

    @EJB
    DataFileServiceBean fileService;

    @EJB
    AuthenticationServiceBean authenticationServiceBean;

    @EJB
    PermissionServiceBean permissionServiceBean;

    @EJB
    UserServiceBean userServiceBean;

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    Dataset dataset;
    AuthenticatedUser user;
    String mode = ImportMode.MERGE.name();
    
    @Override
    public void open(Serializable checkpoint) throws Exception {
        JobOperator jobOperator = BatchRuntime.getJobOperator();
        Properties jobParams = jobOperator.getParameters(jobContext.getInstanceId());
        dataset = datasetServiceBean.findByGlobalId(jobParams.getProperty("datasetId"));
        user = authenticationServiceBean.getAuthenticatedUser(jobParams.getProperty("userId"));
        mode = jobParams.getProperty("mode");
    }

    @Override
    public void writeItems(List list) {
        if (permissionServiceBean.userOn(user, dataset.getOwner()).has(Permission.AddDataset) &&
                dataset.getLatestVersion().getVersionState() == DatasetVersion.VersionState.DRAFT) {
            List<DataFile> datafiles = dataset.getFiles();
            
            // todo: decide if we want to remove datafiles in REPLACE mode
            //removeDataFilesThatNoLongerExist(list);
            
            // add files
            for (Object dataFile : list) {
                datafiles.add((DataFile) dataFile);
            }
            dataset.getLatestVersion().getDataset().setFiles(datafiles);
        } else {
            logger.log(Level.SEVERE, "Unable to save imported datafiles because the authenticated user has " +
                    "insufficient permission.");
        }
    }

    // utils

    /**
     * todo: implement this for real if needed - i don't really know how to destroy datafiles (this doesn't work)
     */
    private void removeDataFilesThatNoLongerExist(List list) {
        List<DataFile> datafiles = dataset.getFiles();
        List<DataFile> removeList = new ArrayList<>();

        if (mode.equalsIgnoreCase(ImportMode.REPLACE.name())) {
            for (DataFile dataFile : datafiles) {
                boolean found = false;
                for (Object item : list) {
                    if (((DataFile) item).getStorageIdentifier().equalsIgnoreCase(dataFile.getStorageIdentifier())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    removeList.add(dataFile);
                }
            }
            // remove any datafiles not found in the import list
            for (DataFile victim : removeList) {
                DataFile merged = em.merge(victim);
                em.remove(merged);
            }
        }
    }
    
}