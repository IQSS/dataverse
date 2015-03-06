package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Set;
import javax.validation.ConstraintViolation;

/**
 *
 * @author michael
 */
@RequiredPermissions( Permission.AddDataset )
public class CreateDatasetVersionCommand extends AbstractCommand<DatasetVersion> {
    
    final DatasetVersion newVersion;
    final Dataset dataset;
    
    public CreateDatasetVersionCommand(User aUser, Dataset theDataset, DatasetVersion aVersion) {
        super(aUser, theDataset);
        dataset = theDataset;
        newVersion = aVersion;
    }
    
    @Override
    public DatasetVersion execute(CommandContext ctxt) throws CommandException {
        DatasetVersion latest = dataset.getLatestVersion();
        if ( latest.isWorkingCopy() ) {
            throw new IllegalCommandException("Latest version is already a draft. Cannot add another draft", this);
        }
         Set<ConstraintViolation> constraintViolations = newVersion.validate();
        if (!constraintViolations.isEmpty()) {
            String validationFailedString = "Validation failed:";
            for (ConstraintViolation constraintViolation : constraintViolations) {
                validationFailedString += " " + constraintViolation.getMessage();
            }
            throw new IllegalCommandException(validationFailedString, this);
        }
        Timestamp now = new Timestamp(new Date().getTime());
        newVersion.setCreateTime(now);
        newVersion.setLastUpdateTime(now);
        dataset.setModificationTime(now);
        newVersion.setDataset(dataset);
        ctxt.em().persist(newVersion);

        // TODO make async
    //    ctxt.index().indexDataset(dataset);
        
        return newVersion;
    }
    
}
