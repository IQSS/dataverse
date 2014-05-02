package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DataverseRole;
import edu.harvard.iq.dataverse.DataverseUser;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.engine.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.logging.Level;
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
        //add creator and create date to dataset
        theDataset.setCreator(getUser());
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
        return save(ctxt);
    }

    public Dataset save(CommandContext ctxt) {
        theDataset.getEditVersion().setCreateTime(new Timestamp(new Date().getTime()));
        Dataset savedDataset = ctxt.em().merge(theDataset);
        String indexingResult = ctxt.index().indexDataset(savedDataset);
        logger.log(Level.INFO, "during dataset save, indexing result was: {0}", indexingResult);
        
        DataverseRole manager = new DataverseRole();
        manager.addPermissions(EnumSet.allOf(Permission.class));
        manager.setAlias("manager");
        manager.setName("Dataset Manager");
        manager.setDescription("Auto-generated role for the creator of this dataset");
        manager.setOwner(savedDataset);
        ctxt.roles().save(manager);
        ctxt.roles().save(new RoleAssignment(manager, getUser(), savedDataset));
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
