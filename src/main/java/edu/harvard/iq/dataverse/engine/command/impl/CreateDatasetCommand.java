package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersionUser;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetVersionUI;
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
import java.util.Date;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

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

    public CreateDatasetCommand(Dataset theDataset, User user) {
        super(user, theDataset.getOwner());
        this.theDataset = theDataset;
        this.registrationRequired = false;
    }

    public CreateDatasetCommand(Dataset theDataset, User user, boolean registrationRequired) {
        super(user, theDataset.getOwner());
        this.theDataset = theDataset;
        this.registrationRequired = registrationRequired;
    }
    
    @Override
    public Dataset execute(CommandContext ctxt) throws CommandException {
        
        // Test for duplicate identifier
        if (!ctxt.datasets().isUniqueIdentifier(theDataset.getIdentifier(), theDataset.getProtocol(), theDataset.getAuthority(), theDataset.getDoiSeparator()) ) {
            throw new IllegalCommandException(String.format("Dataset with identifier '%s', protocol '%s' and authority '%s' already exists",
                                                             theDataset.getIdentifier(), theDataset.getProtocol(), theDataset.getAuthority()),
                                                this);
        }

        // validate
        // @todo for now we run through an initFields method that creates empty fields for anything without a value
        // that way they can be checked for required
        theDataset.getEditVersion().setDatasetFields(theDataset.getEditVersion().initDatasetFields());
        Set<ConstraintViolation> constraintViolations = theDataset.getEditVersion().validate();
        if (!constraintViolations.isEmpty()) {
            String validationFailedString = "Validation failed:";
            for (ConstraintViolation constraintViolation : constraintViolations) {
                validationFailedString += " " + constraintViolation.getMessage();
            }
            throw new IllegalCommandException(validationFailedString, this);
        }
                
        
        // FIXME - need to revisit this. Either
        // theDataset.setCreator(getUser());
        // if, at all, we decide to keep it.
        
        theDataset.setCreateDate(new Timestamp(new Date().getTime()));
        
        Iterator<DatasetField> dsfIt = theDataset.getEditVersion().getDatasetFields().iterator();
        while (dsfIt.hasNext()) {
            if (dsfIt.next().removeBlankDatasetFieldValues()) {
                dsfIt.remove();
            }
        }
        Iterator<DatasetField> dsfItSort = theDataset.getEditVersion().getDatasetFields().iterator();
        while (dsfItSort.hasNext()) {
            dsfItSort.next().setValueDisplayOrder();
        }
        Timestamp createDate = new Timestamp(new Date().getTime());
        theDataset.getEditVersion().setCreateTime(createDate);
        theDataset.getEditVersion().setLastUpdateTime(createDate);
        theDataset.setModificationTime(createDate);
        for (DataFile dataFile: theDataset.getFiles() ){
            dataFile.setCreateDate(theDataset.getCreateDate());
        }
        
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
              
        Dataset savedDataset = ctxt.em().merge(theDataset);
        
        // set the role to be default contributor role for its dataverse
        ctxt.roles().save(new RoleAssignment(savedDataset.getOwner().getDefaultContributorRole(), getUser(), savedDataset));
        
        try {
            // TODO make async
            String indexingResult = ctxt.index().indexDataset(savedDataset);
            logger.log(Level.INFO, "during dataset save, indexing result was: {0}", indexingResult);
        } catch ( RuntimeException e ) {
            logger.log(Level.WARNING, "Exception while indexing:" + e.getMessage(), e);
        }
        
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
