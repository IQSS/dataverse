package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import edu.harvard.iq.dataverse.datacapturemodule.DataCaptureModuleUtil;
import edu.harvard.iq.dataverse.datacapturemodule.ScriptRequestResponse;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.CommandExecutionException;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import static edu.harvard.iq.dataverse.util.StringUtil.isEmpty;
import java.io.IOException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**;
 * An abstract base class for commands that creates {@link Dataset}s.
 *
 * @author michael
 */
@RequiredPermissions(Permission.AddDataset)
public abstract class AbstractCreateDatasetCommand extends AbstractDatasetCommand<Dataset> {
    
    private static final Logger logger = Logger.getLogger(AbstractCreateDatasetCommand.class.getCanonicalName());
    
    final protected boolean registrationRequired;
    
    public AbstractCreateDatasetCommand(Dataset theDataset, DataverseRequest aRequest) {
        this(theDataset, aRequest, false);
    }

    public AbstractCreateDatasetCommand(Dataset theDataset, DataverseRequest aRequest, boolean isRegistrationRequired) {
        super(aRequest, theDataset);
        registrationRequired = isRegistrationRequired;
    }
   
    protected void additionalParameterTests(CommandContext ctxt) throws CommandException {
        // base class - do nothing.
    }
    
    protected DatasetVersion getVersionToPersist( Dataset theDataset ) {
        return theDataset.getLatestVersion();
    }
    
    /**
     * Called after the dataset has been persisted, but before the persistence context
     * has been flushed. 
     * @param theDataset The em-managed dataset.
     * @param ctxt 
     * @throws edu.harvard.iq.dataverse.engine.command.exception.CommandException 
     */
    protected void postPersist( Dataset theDataset, CommandContext ctxt ) throws CommandException {
        // base class - default to nothing.
    }
    
    protected abstract void handlePid( Dataset theDataset, CommandContext ctxt ) throws CommandException ;
    
    @Override
    public Dataset execute(CommandContext ctxt) throws CommandException {
        
        additionalParameterTests(ctxt);
        
        Dataset theDataset = getDataset();
        GlobalIdServiceBean idServiceBean = GlobalIdServiceBean.getBean(ctxt);
        if ( isEmpty(theDataset.getIdentifier()) ) {
            theDataset.setIdentifier(ctxt.datasets().generateDatasetIdentifier(theDataset, idServiceBean));
        }
        
        DatasetVersion dsv = getVersionToPersist(theDataset);
        // This re-uses the state setup logic of CreateDatasetVersionCommand, but
        // without persisting the new version, or altering its files. 
        new CreateDatasetVersionCommand(getRequest(), theDataset, dsv).prepareDatasetAndVersion();
        
        theDataset.setCreator((AuthenticatedUser) getRequest().getUser());
        
        theDataset.setCreateDate(getTimestamp());

        theDataset.setModificationTime(getTimestamp());
        for (DataFile dataFile: theDataset.getFiles() ){
            dataFile.setCreator((AuthenticatedUser) getRequest().getUser());
            dataFile.setCreateDate(theDataset.getCreateDate());
        }
        
        String nonNullDefaultIfKeyNotFound = "";
        if (theDataset.getProtocol()==null) {
            theDataset.setProtocol(ctxt.settings().getValueForKey(SettingsServiceBean.Key.Protocol, nonNullDefaultIfKeyNotFound));
        }
        if (theDataset.getAuthority()==null) {
            theDataset.setAuthority(ctxt.settings().getValueForKey(SettingsServiceBean.Key.Authority, nonNullDefaultIfKeyNotFound));
        }
        if (theDataset.getStorageIdentifier() == null) {
            try {
                DataAccess.createNewStorageIO(theDataset, "placeholder");
            } catch (IOException ioex) {
                // if setting the storage identifier through createNewStorageIO fails, dataset creation
                // does not have to fail. we just set the storage id to a default -SF
                String storageDriver = (System.getProperty("dataverse.files.storage-driver-id") != null) ? System.getProperty("dataverse.files.storage-driver-id") : "file";
                theDataset.setStorageIdentifier(storageDriver  + "://" + theDataset.getGlobalId().asString());
                logger.log(Level.INFO, "Failed to create StorageIO. StorageIdentifier set to default. Not fatal.({0})", ioex.getMessage());
            }
        }
        if (theDataset.getIdentifier()==null) {
            theDataset.setIdentifier(ctxt.datasets().generateDatasetIdentifier(theDataset, idServiceBean));
        }
        
        // Attempt the registration if importing dataset through the API, or the app (but not harvest)
        handlePid(theDataset, ctxt);
                
        if (registrationRequired && (theDataset.getGlobalIdCreateTime() == null)) {
            throw new CommandExecutionException("Dataset could not be created.  Registration failed", this);
        }
        
        ctxt.em().persist(theDataset);
        
        postPersist(theDataset, ctxt);
        
        createDatasetUser(ctxt);
        
        theDataset = ctxt.em().merge(theDataset); // store last updates
        
        // DB updates - done.
        
        // Now we need the acutal dataset id, so we can start indexing.
        ctxt.em().flush();
        
        // TODO: switch to asynchronous version when JPA sync works
        // ctxt.index().asyncIndexDataset(theDataset.getId(), true); 
        ctxt.index().indexDataset(theDataset, true);
        ctxt.solrIndex().indexPermissionsOnSelfAndChildren(theDataset.getId());
        
        /*
        if (DataCaptureModuleUtil.rsyncSupportEnabled(ctxt.settings().getValueForKey(SettingsServiceBean.Key.UploadMethods))) {
            logger.fine("Requesting rsync support.");
            try {
                ScriptRequestResponse scriptRequestResponse = ctxt.engine().submit(new RequestRsyncScriptCommand(getRequest(), theDataset));
                logger.log(Level.FINE, "script: {0}", scriptRequestResponse.getScript());
            } catch (RuntimeException ex) {
                logger.log(Level.WARNING, "Problem getting rsync script: {0}", ex.getLocalizedMessage());
            }
            logger.fine("Done with rsync request.");
        }*/
        return theDataset;
    }

    @Override
    public int hashCode() {
        return 97 + Objects.hashCode(getDataset());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof AbstractCreateDatasetCommand)) {
            return false;
        }
        final AbstractCreateDatasetCommand other = (AbstractCreateDatasetCommand) obj;
        return Objects.equals(getDataset(), other.getDataset());
    }

    @Override
    public String toString() {
        return "[DatasetCreate dataset:" + getDataset().getId() + "]";
    }

}
