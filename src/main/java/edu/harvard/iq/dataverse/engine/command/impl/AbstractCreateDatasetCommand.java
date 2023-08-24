package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.batch.util.LoggingUtil;
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
import org.apache.solr.client.solrj.SolrServerException;

/**;
 * An abstract base class for commands that creates {@link Dataset}s.
 *
 * @author michael
 */
@RequiredPermissions(Permission.AddDataset)
public abstract class AbstractCreateDatasetCommand extends AbstractDatasetCommand<Dataset> {
    
    private static final Logger logger = Logger.getLogger(AbstractCreateDatasetCommand.class.getCanonicalName());
    
    final protected boolean harvested;
    final protected boolean validate;
    
    public AbstractCreateDatasetCommand(Dataset theDataset, DataverseRequest aRequest) {
        this(theDataset, aRequest, false);
    }

    public AbstractCreateDatasetCommand(Dataset theDataset, DataverseRequest aRequest, boolean isHarvested) {
        super(aRequest, theDataset);
        harvested=isHarvested;
        this.validate = true;
    }

    public AbstractCreateDatasetCommand(Dataset theDataset, DataverseRequest aRequest, boolean isHarvested, boolean validate) {
        super(aRequest, theDataset);
        harvested=isHarvested;
        this.validate = validate;
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
    

    protected void postDBFlush( Dataset theDataset, CommandContext ctxt ) throws CommandException {
        // base class - default to nothing.
    }
    
    protected abstract void handlePid( Dataset theDataset, CommandContext ctxt ) throws CommandException ;
    
    @Override
    public Dataset execute(CommandContext ctxt) throws CommandException {
        
        additionalParameterTests(ctxt);
        
        Dataset theDataset = getDataset();
        GlobalIdServiceBean idServiceBean = GlobalIdServiceBean.getBean(ctxt);
        if ( isEmpty(theDataset.getIdentifier()) ) {
            theDataset.setIdentifier(idServiceBean.generateDatasetIdentifier(theDataset));
        }
        
        DatasetVersion dsv = getVersionToPersist(theDataset);
        // This re-uses the state setup logic of CreateDatasetVersionCommand, but
        // without persisting the new version, or altering its files. 
        new CreateDatasetVersionCommand(getRequest(), theDataset, dsv, validate).prepareDatasetAndVersion();
        
        if(!harvested) {
            checkSystemMetadataKeyIfNeeded(dsv, null);
        }
        
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
        	String driverId = theDataset.getEffectiveStorageDriverId();
        	theDataset.setStorageIdentifier(driverId  + DataAccess.SEPARATOR + theDataset.getAuthorityForFileStorage() + "/" + theDataset.getIdentifierForFileStorage());
        }
        if (theDataset.getIdentifier()==null) {
            theDataset.setIdentifier(idServiceBean.generateDatasetIdentifier(theDataset));
        }
        
        // Attempt the registration if importing dataset through the API, or the app (but not harvest)
        handlePid(theDataset, ctxt);
        
        ctxt.em().persist(theDataset);
        
        postPersist(theDataset, ctxt);
        
        createDatasetUser(ctxt);
        
        theDataset = ctxt.em().merge(theDataset); // store last updates
        
        // DB updates - done.
        
        // Now we need the acutal dataset id, so we can start indexing.
        ctxt.em().flush();
        
        //Use for code that requires database ids
        postDBFlush(theDataset, ctxt);
        
        ctxt.index().asyncIndexDataset(theDataset, true);
                 
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
