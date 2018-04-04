package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.DatasetVersion.VersionState;
import edu.harvard.iq.dataverse.api.imports.ImportUtil;
import edu.harvard.iq.dataverse.api.imports.ImportUtil.ImportType;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import edu.harvard.iq.dataverse.datacapturemodule.DataCaptureModuleUtil;
import edu.harvard.iq.dataverse.datacapturemodule.ScriptRequestResponse;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import static edu.harvard.iq.dataverse.util.StringUtil.isEmpty;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Creates a {@link Dataset} in the passed {@link CommandContext}.
 *
 * @author michael
 */
@RequiredPermissions(Permission.AddDataset)
public class CreateDatasetCommand extends AbstractDatasetCommand<Dataset> {
    
    private static final Logger logger = Logger.getLogger(CreateDatasetCommand.class.getCanonicalName());

    private final boolean registrationRequired;
    // TODO: rather than have a boolean, create a sub-command for creating a dataset during import
    private final ImportUtil.ImportType importType;
    private final Template template;

    public CreateDatasetCommand(Dataset theDataset, DataverseRequest aRequest) {
        super(aRequest, theDataset, theDataset.getOwner());
        this.registrationRequired = false;
        this.importType=null;
        this.template=null;
    }

    public CreateDatasetCommand(Dataset theDataset, DataverseRequest aRequest, boolean registrationRequired) {
        super(aRequest, theDataset, theDataset.getOwner());
        this.registrationRequired = registrationRequired;
        this.importType=null;
        this.template=null;
    }
    
    public CreateDatasetCommand(Dataset theDataset, DataverseRequest aRequest, boolean registrationRequired, ImportUtil.ImportType importType) {
        super(aRequest, theDataset, theDataset.getOwner());
        this.registrationRequired = registrationRequired;
        this.importType=importType;
        this.template=null;
    }
    
    public CreateDatasetCommand(Dataset theDataset, DataverseRequest aRequest, boolean registrationRequired, ImportUtil.ImportType importType, Template template) {
        super(aRequest, theDataset, theDataset.getOwner());
        this.registrationRequired = registrationRequired;
        this.importType=importType;
        this.template=template;
    }
    
    @Override
    public Dataset execute(CommandContext ctxt) throws CommandException {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-hh.mm.ss");
        Dataset theDataset = getDataset();
        
        PersistentIdentifierServiceBean idServiceBean = PersistentIdentifierServiceBean.getBean(theDataset.getProtocol(), ctxt);
        if ( isEmpty(theDataset.getIdentifier()) ) {
            theDataset.setIdentifier(ctxt.datasets().generateDatasetIdentifier(theDataset, idServiceBean));
        }
        if ( (importType != ImportType.MIGRATION && importType != ImportType.HARVEST) && !ctxt.datasets().isIdentifierUniqueInDatabase(theDataset.getIdentifier(), theDataset, idServiceBean)) {
            throw new IllegalCommandException(String.format("Dataset with identifier '%s', protocol '%s' and authority '%s' already exists",
                                                             theDataset.getIdentifier(), theDataset.getProtocol(), theDataset.getAuthority()),
                                                this);
        }
        // If we are importing with the API, then we don't want to create an editable version, 
        // just save the version is already in theDataset.
        DatasetVersion dsv = importType!=null? theDataset.getLatestVersion() : theDataset.getEditVersion();
        // validate
        // @todo for now we run through an initFields method that creates empty fields for anything without a value
        // that way they can be checked for required
        dsv.setDatasetFields(dsv.initDatasetFields());
        validateOrDie(dsv, false);
                
        theDataset.setCreator((AuthenticatedUser) getRequest().getUser());
        
        theDataset.setCreateDate(getTimestamp());

        tidyUpFields(dsv);
        dsv.setCreateTime(getTimestamp());
        dsv.setLastUpdateTime(getTimestamp());
        theDataset.setModificationTime(getTimestamp());
        for (DataFile dataFile: theDataset.getFiles() ){
            dataFile.setCreator((AuthenticatedUser) getRequest().getUser());
            dataFile.setCreateDate(theDataset.getCreateDate());
        }
        String nonNullDefaultIfKeyNotFound = "";
        String  protocol     = ctxt.settings().getValueForKey(SettingsServiceBean.Key.Protocol, nonNullDefaultIfKeyNotFound);
        String  authority    = ctxt.settings().getValueForKey(SettingsServiceBean.Key.Authority, nonNullDefaultIfKeyNotFound);
        String  doiSeparator = ctxt.settings().getValueForKey(SettingsServiceBean.Key.DoiSeparator, nonNullDefaultIfKeyNotFound);
        String  doiProvider  = ctxt.settings().getValueForKey(SettingsServiceBean.Key.DoiProvider, nonNullDefaultIfKeyNotFound);
        if (theDataset.getProtocol()==null) theDataset.setProtocol(protocol);
        if (theDataset.getAuthority()==null) theDataset.setAuthority(authority);
        if (theDataset.getDoiSeparator()==null) theDataset.setDoiSeparator(doiSeparator);
        if (theDataset.getStorageIdentifier() == null) {
            try {
                DataAccess.createNewStorageIO(theDataset, "placeholder");
            } catch (IOException ioex) {
                // if setting the storage identifier through createNewStorageIO fails, dataset creation
                // does not have to fail. we just set the storage id to a default -SF
                String storageDriver = (System.getProperty("dataverse.files.storage-driver-id") != null) ? System.getProperty("dataverse.files.storage-driver-id") : "file";
                theDataset.setStorageIdentifier(storageDriver  + "://" + theDataset.getAuthority()+theDataset.getDoiSeparator()+theDataset.getIdentifier());
                logger.log(Level.INFO, "Failed to create StorageIO. StorageIdentifier set to default. Not fatal.({0})", ioex.getMessage());
            }
        }
        if (theDataset.getIdentifier()==null) {
            /* 
                If this command is being executed to save a new dataset initialized
                by the Dataset page (in CREATE mode), it already has the persistent 
                identifier. 
                Same with a new harvested dataset - the imported metadata record
                must have contained a global identifier, for the harvester to be
                trying to save it permanently in the database. 
            
                In some other cases, such as when a new dataset is created 
                via the API, the identifier will need to be generated here. 
            
                        -- L.A. 4.6.2
             */
            
            theDataset.setIdentifier(ctxt.datasets().generateDatasetIdentifier(theDataset, idServiceBean));
            
        }
//        logger.log(Level.INFO, "after doi {0}", formatter.format(new Date().getTime()));
//        ctxt.em().persist(theDataset);
//        logger.log(Level.INFO, "after db persist. Our id is: {0}", theDataset.getId());
//        ctxt.em().flush();
//        logger.log(Level.INFO, "after db update. Our id is: {0}", theDataset.getId());
        
        logger.fine("Saving the files permanently.");
        ctxt.ingest().addFiles(dsv, theDataset.getFiles());
        logger.log(Level.FINE,"doiProvider={0} protocol={1}  importType={2}  GlobalIdCreateTime=={3}", new Object[]{doiProvider, protocol,  importType, theDataset.getGlobalIdCreateTime()});
        // Attempt the registration if importing dataset through the API, or the app (but not harvest or migrate)
        if ( (importType == null || importType.equals(ImportType.NEW))
              && theDataset.getGlobalIdCreateTime() == null) {
            String doiRetString = "";
            idServiceBean = PersistentIdentifierServiceBean.getBean(ctxt);
            try{
                logger.log(Level.FINE,"creating identifier");
                doiRetString = idServiceBean.createIdentifier(theDataset);
            } catch (Throwable e){
                logger.log(Level.WARNING, "Exception while creating Identifier: " + e.getMessage(), e);
            }

            // Check return value to make sure registration succeeded
            if (!idServiceBean.registerWhenPublished() && doiRetString.contains(theDataset.getIdentifier())) {
                theDataset.setGlobalIdCreateTime(getTimestamp());
            }
        } else {
            // If harvest or migrate, and this is a released dataset, we don't need to register,
            // so set the globalIdCreateTime to now
            if (theDataset.getLatestVersion().getVersionState().equals(VersionState.RELEASED)) {
                theDataset.setGlobalIdCreateTime(new Date());
            }
        }
        
        if (registrationRequired && (theDataset.getGlobalIdCreateTime() == null)) {
            throw new IllegalCommandException("Dataset could not be created.  Registration failed", this);
        }
        
        
        ctxt.em().persist(theDataset);
        
        // set the role to be default contributor role for its dataverse
        if (importType==null || importType.equals(ImportType.NEW)) {
            String privateUrlToken = null;
            ctxt.roles().save(new RoleAssignment(theDataset.getOwner().getDefaultContributorRole(),
                                                   getRequest().getUser(), theDataset, privateUrlToken));
        }
        theDataset.setPermissionModificationTime(getTimestamp());
        
//        theDataset = ctxt.em().merge(theDataset);
        
        if ( template != null ) {
            ctxt.templates().incrementUsageCount(template.getId());
        }

        logger.fine("Checking if rsync support is enabled.");
        if (DataCaptureModuleUtil.rsyncSupportEnabled(ctxt.settings().getValueForKey(SettingsServiceBean.Key.UploadMethods))) {
            try {
                ScriptRequestResponse scriptRequestResponse = ctxt.engine().submit(new RequestRsyncScriptCommand(getRequest(), theDataset));
                logger.log(Level.FINE, "script: {0}", scriptRequestResponse.getScript());
            } catch (RuntimeException ex) {
                logger.log(Level.WARNING, "Problem getting rsync script: {0}", ex.getLocalizedMessage());
            }
        }
        logger.fine("Done with rsync request, if any.");

        // if we are not migrating, assign the user to this version
        updateDatasetUser(ctxt);
        
        ctxt.em().merge(theDataset); // store last updates
        logger.log(Level.INFO , "DB updates - done."); // FIXME: MBS: Remove.
        // DB updates - done.
        
        boolean doNormalSolrDocCleanUp = true;
        ctxt.index().asyncIndexDataset(theDataset, doNormalSolrDocCleanUp);
        
        logger.log(Level.FINE, "after index {0}", formatter.format(new Date().getTime()));      
        
        return theDataset;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(getDataset());
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof CreateDatasetCommand)) {
            return false;
        }
        final CreateDatasetCommand other = (CreateDatasetCommand) obj;
        return Objects.equals(getDataset(), other.getDataset());
    }

    @Override
    public String toString() {
        return "[DatasetCreate dataset:" + getDataset().getId() + "]";
    }
}
