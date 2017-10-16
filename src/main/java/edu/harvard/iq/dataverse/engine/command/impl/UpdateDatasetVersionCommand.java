package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetLock;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import javax.validation.ConstraintViolation;

/**
 * Updates a {@link DatasetVersion}, as long as that version is in a "draft" state.
 * @author michael
 */
@RequiredPermissions(Permission.EditDataset)
public class UpdateDatasetVersionCommand extends AbstractCommand<DatasetVersion> {
    
    final DatasetVersion newVersion;
    
    public UpdateDatasetVersionCommand(DataverseRequest aRequest, DatasetVersion theNewVersion) {
        super(aRequest, theNewVersion.getDataset());
        newVersion = theNewVersion;
    }
    
    
    
    @Override
    public DatasetVersion execute(CommandContext ctxt) throws CommandException {
        
        Dataset ds = newVersion.getDataset();
        // TODO: Figure out why UpdateDatasetCommand is used by the GUI but UpdateDatasetVersionCommand is used by the API.
        DatasetLock datasetLock = ds.getDatasetLock();
        if (datasetLock != null) {
            if (DatasetLock.Reason.InReview.equals(datasetLock.getReason())) {
                User user = getUser();
                if (!ctxt.permissions().isUserAllowedOn(user, new PublishDatasetCommand(ds, getRequest(), true), ds)) {
                    throw new IllegalCommandException("User does not have permission to edit due to dataset lock " + datasetLock.getReason() + ".", this);
                }
            }
        }
        DatasetVersion latest = ds.getLatestVersion();
        
        if ( latest == null ) {
            throw new IllegalCommandException("Dataset " + ds.getId() + " does not have a latest version.", this);
        }
            
        if ( ! latest.isDraft() ) {
            throw new IllegalCommandException("Cannot update a dataset version that's not a draft", this);
        }
        
        DatasetVersion edit = ds.getEditVersion();
        edit.setDatasetFields( newVersion.getDatasetFields() );
   
        edit.setDatasetFields(edit.initDatasetFields());

        Set<ConstraintViolation> constraintViolations = edit.validate();
        if (!constraintViolations.isEmpty()) {
            String validationFailedString = "Validation failed:";
            for (ConstraintViolation constraintViolation : constraintViolations) {
                validationFailedString += " " + constraintViolation.getMessage();
            }
            throw new IllegalCommandException(validationFailedString, this);
        }

        Iterator<DatasetField> dsfIt = edit.getDatasetFields().iterator();
        while (dsfIt.hasNext()) {
            if (dsfIt.next().removeBlankDatasetFieldValues()) {
                dsfIt.remove();
            }
        }
        
        
        
        
        Timestamp now = new Timestamp(new Date().getTime());
        edit.setLastUpdateTime(now);
        ds.setModificationTime(now);
        DatasetVersion managed = ctxt.em().merge(edit);
        
        boolean doNormalSolrDocCleanUp = true;
        ctxt.index().indexDataset(managed.getDataset(), doNormalSolrDocCleanUp);
        
        return managed;
    }
    
}
