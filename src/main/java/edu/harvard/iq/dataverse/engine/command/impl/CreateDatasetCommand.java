package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldConstant;
import edu.harvard.iq.dataverse.DatasetFieldValue;
import edu.harvard.iq.dataverse.DataverseUser;
import edu.harvard.iq.dataverse.engine.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Creates a {@link Dataset} in the passed {@link CommandContext}.
 *
 * @author michael
 */
@RequiredPermissions(Permission.AddDataset)
public class CreateDatasetCommand extends AbstractCommand<Dataset> {
   private static final Logger logger = Logger.getLogger(CreateDatasetCommand.class.getCanonicalName());
 
    private final Dataset theDataset;

    public CreateDatasetCommand(Dataset theDataset, DataverseUser user) {
        super(user, theDataset.getOwner());
        this.theDataset = theDataset;
    }

    @Override
    public Dataset execute(CommandContext ctxt) throws CommandException {
        //On create set depositor and deposit data - these are not editable on create but may be modified
         for (DatasetField dsf : theDataset.getEditVersion().getDatasetFields()){
            if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.depositor)){
                DatasetFieldValue dsfv = new DatasetFieldValue (dsf);
                dsfv.setValue(getUser().getFirstName() + " " + getUser().getLastName());
                dsf.getDatasetFieldValues().add(dsfv);
            }
            if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.dateOfDeposit)){
                DatasetFieldValue dsfv = new DatasetFieldValue (dsf); 
                dsfv.setValue(new SimpleDateFormat("yyyy-MM-dd").format(new Timestamp(new Date().getTime())));
                dsf.getDatasetFieldValues().add(dsfv);
            }
        }
         //add creator and create date to dataset
            theDataset.setCreator(getUser());
            theDataset.setCreateDate(new Timestamp(new Date().getTime()));

        Iterator<DatasetField> dsfIt = theDataset.getEditVersion().getDatasetFields().iterator();
        while (dsfIt.hasNext()) {
            if (ctxt.datasets().removeBlankDatasetFieldValues(dsfIt.next())) {
                dsfIt.remove();
            }
        }
        return save(ctxt);
    }

    public Dataset save(CommandContext ctxt) {

        Dataset savedDataset = ctxt.em().merge(theDataset);
        String indexingResult = ctxt.index().indexDataset(savedDataset);
        logger.info("during dataset save, indexing result was: " + indexingResult);
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
