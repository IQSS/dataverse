package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DataverseUser;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import java.util.Collections;

/**
 *
 * @author Naomi
 */
@RequiredPermissions( Permission.Access )
public class GetDraftDatasetVersionCommand extends AbstractCommand<DatasetVersion>{
    private final Dataset ds;

    public GetDraftDatasetVersionCommand(DataverseUser aUser, Dataset anAffectedDataset) {
        super(aUser, anAffectedDataset);
        ds = anAffectedDataset;
    }

    @Override
    public DatasetVersion execute(CommandContext ctxt) throws CommandException {
        if (ctxt.permissions().on(ds).user(getUser()).has(Permission.AddDatasetVersion)) {
            return ds.getEditVersion();
        } else {
            throw new PermissionException("User does not have permission to view draft version",
                    this, Collections.singleton(Permission.AddDatasetVersion), ds);
        }
            
    }
    
}
