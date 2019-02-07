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
            Boolean doiRetString = idServiceBean.updateIdentifier(target);
            if (doiRetString) {
                target.setGlobalIdCreateTime(new Timestamp(new Date().getTime()));
                ctxt.em().merge(target);
                ctxt.em().flush();
            }
            //When updating, we want to traverse through files even if the dataset itself didn't need updating.
            String currentGlobalIdProtocol = ctxt.settings().getValueForKey(SettingsServiceBean.Key.Protocol, "");
            String dataFilePIDFormat = ctxt.settings().getValueForKey(SettingsServiceBean.Key.DataFilePIDFormat, "DEPENDENT");
            boolean isFilePIDsEnabled = ctxt.systemConfig().isFilePIDsEnabled();
            // We will skip trying to update the global identifiers for datafiles if they aren't being used, or
            // if "dependent" file-level identifiers are requested, AND the naming 
            // protocol of the dataset global id is different from the
            // one currently configured for the Dataverse. This is to specifically 
            // address the issue with the datasets with handle ids registered, 
            // that are currently configured to use DOI.
            // ...
            // Additionally in 4.9.3 we have added a system variable to disable 
            // using file PIDs on the installation level.

            for (DataFile df : target.getFiles()) {
                String protocol = df.getProtocol();
                if ((currentGlobalIdProtocol.equals(protocol) || dataFilePIDFormat.equals("INDEPENDENT"))// TODO(pm) - check authority too
                        && isFilePIDsEnabled) {
                    doiRetString = idServiceBean.updateIdentifier(df);
                    if (doiRetString) {
                        df.setGlobalIdCreateTime(new Timestamp(new Date().getTime()));
                        ctxt.em().merge(df);
                        ctxt.em().flush();
                    }
                }
            }
        } catch (Exception e) {
            //do nothing - item and the problem has been logged
        }
    }

}
