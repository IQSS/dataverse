package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.GlobalIdServiceBean;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.AbstractVoidCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;

/**
 *
 * @author skraffmi
 * No required permissions because we are enforcing super user status in the execute
 */
@RequiredPermissions({})
public class UpdateDvObjectPIDMetadataCommand extends AbstractVoidCommand {

    private final Dataset target;

    public UpdateDvObjectPIDMetadataCommand(Dataset target, DataverseRequest aRequest) {
        super(aRequest, target);
        this.target = target;
    }

    @Override
    protected void executeImpl(CommandContext ctxt) throws CommandException {


        if (!(getUser() instanceof AuthenticatedUser) || !getUser().isSuperuser()) {
            throw new PermissionException(BundleUtil.getStringFromBundle("datasets.api.updatePIDMetadata.auth.mustBeSuperUser"),
                    this, Collections.singleton(Permission.EditDataset), target);
        }
        if (!this.target.isReleased()){
            //This is for the bulk update version of the api.
            //We don't want to modify drafts, but we want it to keep going
            //the single dataset update api checks for drafts before calling the command
            return;
        }
        GlobalIdServiceBean idServiceBean = GlobalIdServiceBean.getBean(target.getProtocol(), ctxt);
        try {
            Boolean doiRetString = idServiceBean.publicizeIdentifier(target);
            if (doiRetString) {
                target.setGlobalIdCreateTime(new Timestamp(new Date().getTime()));
                ctxt.em().merge(target);
                ctxt.em().flush();
                // When updating, we want to traverse through files even if the dataset itself
                // didn't need updating.
                String currentGlobalIdProtocol = ctxt.settings().getValueForKey(SettingsServiceBean.Key.Protocol, "");
                String dataFilePIDFormat = ctxt.settings().getValueForKey(SettingsServiceBean.Key.DataFilePIDFormat, "DEPENDENT");
                boolean isFilePIDsEnabled = ctxt.systemConfig().isFilePIDsEnabledForCollection(target.getOwner());
                // We will skip trying to update the global identifiers for datafiles if they
                // aren't being used.
                // If they are, we need to assure that there's an existing PID or, as when
                // creating PIDs, that the protocol matches that of the dataset DOI if
                // we're going to create a DEPENDENT file PID.
                String protocol = target.getProtocol();
                for (DataFile df : target.getFiles()) {
                    if (isFilePIDsEnabled && // using file PIDs and
                            (!(df.getIdentifier() == null || df.getIdentifier().isEmpty()) || // identifier exists, or
                                    currentGlobalIdProtocol.equals(protocol) || // right protocol to create dependent DOIs, or
                                    dataFilePIDFormat.equals("INDEPENDENT"))// or independent. TODO(pm) - check authority too
                    ) {
                        doiRetString = idServiceBean.publicizeIdentifier(df);
                        if (doiRetString) {
                            df.setGlobalIdCreateTime(new Timestamp(new Date().getTime()));
                            ctxt.em().merge(df);
                            ctxt.em().flush();
                        }
                    }
                }
            } else {
                //do nothing - we'll know it failed because the global id create time won't have been updated.
            }
        } catch (Exception e) {
            //do nothing - item and the problem has been logged
        }
    }

}
