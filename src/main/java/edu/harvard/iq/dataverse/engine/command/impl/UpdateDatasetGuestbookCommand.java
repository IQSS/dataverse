package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.Guestbook;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;

import java.util.List;

@RequiredPermissions(Permission.EditDataset)
public class UpdateDatasetGuestbookCommand extends AbstractCommand<Dataset> {

    private final Dataset dataset;
    private final Guestbook guestbook;

    public UpdateDatasetGuestbookCommand(Dataset dataset, Guestbook guestbook, DataverseRequest aRequest) {
        super(aRequest, dataset);
        this.dataset = dataset;
        this.guestbook = guestbook;
    }

    @Override
    public Dataset execute(CommandContext ctxt) throws CommandException {
        Guestbook allowedGuestbook = null;
        // if guestbook is null then we are removing it from the dataset
        if (guestbook != null) {
            // Make sure the requested guestbook is available via the dataset's ancestry
            final List<Guestbook> guestbooks = dataset.getOwner().getAvailableGuestbooks();
            for (Guestbook gb : guestbooks) {
                if (gb.getId() == guestbook.getId()) {
                    allowedGuestbook = gb;
                    break;
                }
            }

            if (allowedGuestbook == null) {
                throw new IllegalCommandException("Could not find an available guestbook with id " + guestbook.getId(), this);
            }
        }
        dataset.setGuestbook(allowedGuestbook);
        Dataset savedDataset = ctxt.em().merge(dataset);
        ctxt.em().flush();
        return savedDataset;
    }
}
