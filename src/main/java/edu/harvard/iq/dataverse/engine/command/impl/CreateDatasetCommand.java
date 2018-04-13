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
        Dataset theDataset = getDataset();
        
        PersistentIdentifierServiceBean idServiceBean = PersistentIdentifierServiceBean.getBean(theDataset.getProtocol(), ctxt);
        if ( isEmpty(theDataset.getIdentifier()) ) {
            theDataset.setIdentifier(ctxt.datasets().generateDatasetIdentifier(theDataset, idServiceBean));
        }
        if ( (importType != ImportType.MIGRATION && importType != ImportType.HARVEST) 
              && !ctxt.datasets().isIdentifierUniqueInDatabase(theDataset.getIdentifier(), theDataset, idServiceBean) ) {
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
        tidyUpFields(dsv);
        validateOrDie(dsv, false);
                
        theDataset.setCreator((AuthenticatedUser) getRequest().getUser());
        
        theDataset.setCreateDate(getTimestamp());

        dsv.setCreateTime(getTimestamp());
        dsv.setLastUpdateTime(getTimestamp());
        theDataset.setModificationTime(getTimestamp());
        for (DataFile dataFile: theDataset.getFiles() ){
            dataFile.setCreator((AuthenticatedUser) getRequest().getUser());
            dataFile.setCreateDate(theDataset.getCreateDate());
        }
        String nonNullDefaultIfKeyNotFound = "";
        if (theDataset.getProtocol()==null) theDataset.setProtocol(ctxt.settings().getValueForKey(SettingsServiceBean.Key.Protocol, nonNullDefaultIfKeyNotFound));
        if (theDataset.getAuthority()==null) theDataset.setAuthority(ctxt.settings().getValueForKey(SettingsServiceBean.Key.Authority, nonNullDefaultIfKeyNotFound));
        if (theDataset.getDoiSeparator()==null) theDataset.setDoiSeparator(ctxt.settings().getValueForKey(SettingsServiceBean.Key.DoiSeparator, nonNullDefaultIfKeyNotFound));
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
        
        logger.fine("Saving the files permanently.");
        ctxt.ingest().addFiles(dsv, theDataset.getFiles());
        // Attempt the registration if importing dataset through the API, or the app (but not harvest or migrate)
        if ( (importType==null || importType.equals(ImportType.NEW)) && 
             theDataset.getGlobalIdCreateTime() == null) {
            String doiRetString = "";
            idServiceBean = PersistentIdentifierServiceBean.getBean(ctxt);
            try {
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
        if ( importType==null || importType.equals(ImportType.NEW) ) {
            String privateUrlToken = null;
            RoleAssignment roleAssignment = new RoleAssignment(theDataset.getOwner().getDefaultContributorRole(),
                getRequest().getUser(), theDataset, privateUrlToken);
            ctxt.roles().save(roleAssignment, false);
        }
        theDataset.setPermissionModificationTime(getTimestamp());
        
        if ( template != null ) {
            ctxt.templates().incrementUsageCount(template.getId());
        }

        // if we are not migrating, assign the user to this version
        createDatasetUser(ctxt);
        
        theDataset = ctxt.em().merge(theDataset); // store last updates
        
        // DB updates - done.
        
        // Now we need the acutal dataset id, so we can start indexing.
        ctxt.em().flush();
        
        // TODO: switch to asynchronous version when JPA sync works
        // ctxt.index().asyncIndexDataset(theDataset.getId(), true); 
        ctxt.index().indexDataset(theDataset, true);
        ctxt.solrIndex().indexPermissionsOnSelfAndChildren(theDataset.getId());
        
        if (DataCaptureModuleUtil.rsyncSupportEnabled(ctxt.settings().getValueForKey(SettingsServiceBean.Key.UploadMethods))) {
            logger.fine("Requesting rsync support.");
            try {
                ScriptRequestResponse scriptRequestResponse = ctxt.engine().submit(new RequestRsyncScriptCommand(getRequest(), theDataset));
                logger.log(Level.FINE, "script: {0}", scriptRequestResponse.getScript());
            } catch (RuntimeException ex) {
                logger.log(Level.WARNING, "Problem getting rsync script: {0}", ex.getLocalizedMessage());
            }
            logger.fine("Done with rsync request.");
        }
        
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
