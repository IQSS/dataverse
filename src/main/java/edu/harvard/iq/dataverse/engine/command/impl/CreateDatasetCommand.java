package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersionUser;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
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
    private final boolean importMode;

    public CreateDatasetCommand(Dataset theDataset, User user) {
        super(user, theDataset.getOwner());
        this.theDataset = theDataset;
        this.registrationRequired = false;
        this.importMode=false;
    }

    public CreateDatasetCommand(Dataset theDataset, User user, boolean registrationRequired) {
        super(user, theDataset.getOwner());
        this.theDataset = theDataset;
        this.registrationRequired = registrationRequired;
        this.importMode=false;
    }
      public CreateDatasetCommand(Dataset theDataset, User user, boolean registrationRequired, boolean importMode) {
        super(user, theDataset.getOwner());
        this.theDataset = theDataset;
        this.registrationRequired = registrationRequired;
        this.importMode=importMode;
    }
    @Override
    public Dataset execute(CommandContext ctxt) throws CommandException {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-hh.mm.ss");
        logger.log(Level.INFO, "start "  + formatter.format(new Date().getTime()));
        // If this is not a migrated dataset, Test for duplicate identifier
        if (!importMode && !ctxt.datasets().isUniqueIdentifier(theDataset.getIdentifier(), theDataset.getProtocol(), theDataset.getAuthority(), theDataset.getDoiSeparator()) ) {
            throw new IllegalCommandException(String.format("Dataset with identifier '%s', protocol '%s' and authority '%s' already exists",
                                                             theDataset.getIdentifier(), theDataset.getProtocol(), theDataset.getAuthority()),
                                                this);
        }
        // If we are in importMode, then we don't want to create an editable version, 
        // just save the version is already in theDataset.
        DatasetVersion dsv = importMode? theDataset.getLatestVersion() : theDataset.getEditVersion();
        // validate
        // @todo for now we run through an initFields method that creates empty fields for anything without a value
        // that way they can be checked for required
        dsv.setDatasetFields(dsv.initDatasetFields());
        Set<ConstraintViolation> constraintViolations = dsv.validate();
        if (!constraintViolations.isEmpty()) {
            String validationFailedString = "Validation failed:";
            for (ConstraintViolation constraintViolation : constraintViolations) {
                validationFailedString += " " + constraintViolation.getMessage();
            }
            throw new IllegalCommandException(validationFailedString, this);
        }
                
        logger.log(Level.INFO, "after validation "  + formatter.format(new Date().getTime()));
        // FIXME - need to revisit this. Either
        // theDataset.setCreator(getUser());
        // if, at all, we decide to keep it.
        
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
            dataFile.setCreateDate(theDataset.getCreateDate());
        }
        logger.log(Level.INFO,"after datascrub "  + formatter.format(new Date().getTime()));        
        String nonNullDefaultIfKeyNotFound = "";
        String    protocol = ctxt.settings().getValueForKey(SettingsServiceBean.Key.Protocol, nonNullDefaultIfKeyNotFound);
        String    doiProvider = ctxt.settings().getValueForKey(SettingsServiceBean.Key.DoiProvider, nonNullDefaultIfKeyNotFound);
        if (protocol.equals("doi") 
              && doiProvider.equals("EZID") && theDataset.getGlobalIdCreateTime() == null) {
            String doiRetString = ctxt.doiEZId().createIdentifier(theDataset);
            if (doiRetString.contains(theDataset.getIdentifier())) {
                theDataset.setGlobalIdCreateTime(createDate);
            } 
        }
        
        if (registrationRequired && theDataset.getGlobalIdCreateTime() == null) {
            throw new IllegalCommandException("Dataset could not be created.  Registration failed", this);
               }
        logger.log(Level.INFO,"after doi "  + formatter.format(new Date().getTime()));          
        Dataset savedDataset = ctxt.em().merge(theDataset);
         logger.log(Level.INFO,"after db update "  + formatter.format(new Date().getTime()));       
        // set the role to be default contributor role for its dataverse
        ctxt.roles().save(new RoleAssignment(savedDataset.getOwner().getDefaultContributorRole(), getUser(), savedDataset));
        
        savedDataset.setPermissionModificationTime(new Timestamp(new Date().getTime()));
        savedDataset = ctxt.em().merge(savedDataset);

        try {
            // TODO make async
            String indexingResult = ctxt.index().indexDataset(savedDataset);
            logger.log(Level.INFO, "during dataset save, indexing result was: {0}", indexingResult);
        } catch ( RuntimeException e ) {
            logger.log(Level.WARNING, "Exception while indexing:" + e.getMessage(), e);
        }
          logger.log(Level.INFO,"after index "  + formatter.format(new Date().getTime()));      
        DatasetVersionUser datasetVersionDataverseUser = new DatasetVersionUser();        
        datasetVersionDataverseUser.setUserIdentifier(getUser().getIdentifier());
        datasetVersionDataverseUser.setDatasetVersion(savedDataset.getLatestVersion());
        datasetVersionDataverseUser.setLastUpdateDate((Timestamp) createDate);  
        if (savedDataset.getLatestVersion().getId() == null){
            logger.warning("CreateDatasetCommand: savedDataset version id is null");
        } else {
            datasetVersionDataverseUser.setDatasetversionid(savedDataset.getLatestVersion().getId().intValue());
        }       
        ctxt.em().merge(datasetVersionDataverseUser); 
           logger.log(Level.INFO,"after create version user "  + formatter.format(new Date().getTime()));       
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
