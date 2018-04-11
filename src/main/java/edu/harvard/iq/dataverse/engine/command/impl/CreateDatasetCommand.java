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
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.validation.ConstraintViolation;

/**
 * Creates a {@link Dataset} in the passed {@link CommandContext}.
 *
 * @author michael
 */
@RequiredPermissions(Permission.AddDataset)
public class CreateDatasetCommand extends AbstractCommand<Dataset> {
    
    private static final Logger logger = Logger.getLogger(CreateDatasetCommand.class.getCanonicalName());

    private final Dataset theDataset;
    private final boolean registrationRequired;
    // TODO: rather than have a boolean, create a sub-command for creating a dataset during import
    private final ImportUtil.ImportType importType;
    private final Template template;

    public CreateDatasetCommand(Dataset theDataset, DataverseRequest aRequest) {
        super(aRequest, theDataset.getOwner());
        this.theDataset = theDataset;
        this.registrationRequired = false;
        this.importType=null;
        this.template=null;
    }

    public CreateDatasetCommand(Dataset theDataset, DataverseRequest aRequest, boolean registrationRequired) {
        super(aRequest, theDataset.getOwner());
        this.theDataset = theDataset;
        this.registrationRequired = registrationRequired;
        this.importType=null;
        this.template=null;
    }
    
    public CreateDatasetCommand(Dataset theDataset, DataverseRequest aRequest, boolean registrationRequired, ImportUtil.ImportType importType) {
        super(aRequest, theDataset.getOwner());
        this.theDataset = theDataset;
        this.registrationRequired = registrationRequired;
        this.importType=importType;
        this.template=null;
    }
    
    public CreateDatasetCommand(Dataset theDataset, DataverseRequest aRequest, boolean registrationRequired, ImportUtil.ImportType importType, Template template) {
        super(aRequest, theDataset.getOwner());
        this.theDataset = theDataset;
        this.registrationRequired = registrationRequired;
        this.importType=importType;
        this.template=template;
    }
    
    @Override
    public Dataset execute(CommandContext ctxt) throws CommandException {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-hh.mm.ss");
        
        
        
        IdServiceBean idServiceBean = IdServiceBean.getBean(theDataset.getProtocol(), ctxt);
        if(theDataset.getIdentifier() == null || theDataset.getIdentifier().isEmpty()){
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
        Set<ConstraintViolation> constraintViolations = dsv.validate();
        if (!constraintViolations.isEmpty()) {
            String validationFailedString = "Validation failed:";
            for (ConstraintViolation constraintViolation : constraintViolations) {
                validationFailedString += " " + constraintViolation.getMessage();
                validationFailedString += " Invalid value: '" + constraintViolation.getInvalidValue() + "'.";
            }
            throw new IllegalCommandException(validationFailedString, this);
        }
                
        theDataset.setCreator((AuthenticatedUser) getRequest().getUser());
        
        theDataset.setCreateDate(new Timestamp(new Date().getTime()));

        Iterator<DatasetField> dsfIt = dsv.getDatasetFields().iterator();
        while (dsfIt.hasNext()) {
            if (dsfIt.next().removeBlankDatasetFieldValues()) {
                dsfIt.remove();
            }
        }
        Iterator<DatasetField> dsfItSort = dsv.getDatasetFields().iterator();
        while (dsfItSort.hasNext()) {
            dsfItSort.next().setValueDisplayOrder();
        }
        Timestamp createDate = new Timestamp(new Date().getTime());
        dsv.setCreateTime(createDate);
        dsv.setLastUpdateTime(createDate);
        theDataset.setModificationTime(createDate);
        for (DataFile dataFile: theDataset.getFiles() ){
            dataFile.setCreator((AuthenticatedUser)  getRequest().getUser());
            dataFile.setCreateDate(theDataset.getCreateDate());
        }
        String nonNullDefaultIfKeyNotFound = "";
        String    protocol = ctxt.settings().getValueForKey(SettingsServiceBean.Key.Protocol, nonNullDefaultIfKeyNotFound);
        String    authority = ctxt.settings().getValueForKey(SettingsServiceBean.Key.Authority, nonNullDefaultIfKeyNotFound);
        String  doiSeparator = ctxt.settings().getValueForKey(SettingsServiceBean.Key.DoiSeparator, nonNullDefaultIfKeyNotFound);
        String    doiProvider = ctxt.settings().getValueForKey(SettingsServiceBean.Key.DoiProvider, nonNullDefaultIfKeyNotFound);
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
                logger.info("Failed to create StorageIO. StorageIdentifier set to default. Not fatal." + "(" + ioex.getMessage() + ")");
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
        logger.log(Level.FINE,"doiProvider={0} protocol={1}  importType={2}  GlobalIdCreateTime=={3}", new Object[]{doiProvider, protocol,  importType, theDataset.getGlobalIdCreateTime()});
        // Attempt the registration if importing dataset through the API, or the app (but not harvest or migrate)
        if ((importType == null || importType.equals(ImportType.NEW))
                && theDataset.getGlobalIdCreateTime() == null) {
                String doiRetString = "";
                idServiceBean = IdServiceBean.getBean(ctxt);
                try{
                    logger.log(Level.FINE,"creating identifier");
                    doiRetString = idServiceBean.createIdentifier(theDataset);
                } catch (Throwable e){
                    logger.log(Level.WARNING, "Exception while creating Identifier: " + e.getMessage(), e);
                }

                // Check return value to make sure registration succeeded
                if (!idServiceBean.registerWhenPublished() && doiRetString.contains(theDataset.getIdentifier())) {
                    theDataset.setGlobalIdCreateTime(createDate);
                }
        } else // If harvest or migrate, and this is a released dataset, we don't need to register,
        // so set the globalIdCreateTime to now
        if (theDataset.getLatestVersion().getVersionState().equals(VersionState.RELEASED)) {
            theDataset.setGlobalIdCreateTime(new Date());
        }
        
        if (registrationRequired && theDataset.getGlobalIdCreateTime() == null) {
            throw new IllegalCommandException("Dataset could not be created.  Registration failed", this);
               }
        logger.log(Level.FINE, "after doi {0}", formatter.format(new Date().getTime()));
        Dataset savedDataset = ctxt.em().merge(theDataset);
        logger.log(Level.FINE, "after db update {0}", formatter.format(new Date().getTime()));
        // set the role to be default contributor role for its dataverse
        if (importType==null || importType.equals(ImportType.NEW)) {
            String privateUrlToken = null;
            ctxt.roles().save(new RoleAssignment(savedDataset.getOwner().getDefaultContributorRole(), getRequest().getUser(), savedDataset, privateUrlToken));
         }
        
        savedDataset.setPermissionModificationTime(new Timestamp(new Date().getTime()));
        savedDataset = ctxt.em().merge(savedDataset);
        
        if(template != null){
            ctxt.templates().incrementUsageCount(template.getId());
        }

        logger.fine("Checking if rsync support is enabled.");
        if (DataCaptureModuleUtil.rsyncSupportEnabled(ctxt.settings().getValueForKey(SettingsServiceBean.Key.UploadMethods))) {
            try {
                ScriptRequestResponse scriptRequestResponse = ctxt.engine().submit(new RequestRsyncScriptCommand(getRequest(), savedDataset));
                logger.fine("script: " + scriptRequestResponse.getScript());
            } catch (RuntimeException ex) {
                logger.info("Problem getting rsync script: " + ex.getLocalizedMessage());
            }
        }
        logger.fine("Done with rsync request, if any.");

        try {
            /**
             * @todo Do something with the result. Did it succeed or fail?
             */
            boolean doNormalSolrDocCleanUp = true;
            ctxt.index().indexDataset(savedDataset, doNormalSolrDocCleanUp);
        
        } catch ( Exception e ) { // RuntimeException e ) {
            logger.log(Level.WARNING, "Exception while indexing:" + e.getMessage()); //, e);
            /**
             * Even though the original intention appears to have been to allow the 
             * dataset to be successfully created, even if an exception is thrown during 
             * the indexing - in reality, a runtime exception there, even caught, 
             * still forces the EJB transaction to be rolled back; hence the 
             * dataset is NOT created... but the command completes and exits as if
             * it has been successful. 
             * So I am going to throw a Command Exception here, to avoid this. 
             * If we DO want to be able to create datasets even if they cannot 
             * be immediately indexed, we'll have to figure out how to do that. 
             * (Note that import is still possible when Solr is down - because indexDataset() 
             * does NOT throw an exception if it is.
             * -- L.A. 4.5
             */
            throw new CommandException("Dataset could not be created. Indexing failed", this);
            
        }
        logger.log(Level.FINE, "after index {0}", formatter.format(new Date().getTime()));      
        
        // if we are not migrating, assign the user to this version
        if (importType==null || importType.equals(ImportType.NEW)) {  
            DatasetVersionUser datasetVersionDataverseUser = new DatasetVersionUser();     
            String id =  getRequest().getUser().getIdentifier();
            id = id.startsWith("@") ? id.substring(1) : id;
            AuthenticatedUser au = ctxt.authentication().getAuthenticatedUser(id);
            datasetVersionDataverseUser.setAuthenticatedUser(au);
            datasetVersionDataverseUser.setDatasetVersion(savedDataset.getLatestVersion());
            datasetVersionDataverseUser.setLastUpdateDate(createDate); 
            if (savedDataset.getLatestVersion().getId() == null){
                logger.warning("CreateDatasetCommand: savedDataset version id is null");
            } else {
                datasetVersionDataverseUser.setDatasetVersion(savedDataset.getLatestVersion());  
            }       
            ctxt.em().merge(datasetVersionDataverseUser); 
        }
           logger.log(Level.FINE,"after create version user "  + formatter.format(new Date().getTime()));       
        return savedDataset;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.theDataset);
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
        return Objects.equals(this.theDataset, other.theDataset);
    }

    @Override
    public String toString() {
        return "[DatasetCreate dataset:" + theDataset.getId() + "]";
    }
}
