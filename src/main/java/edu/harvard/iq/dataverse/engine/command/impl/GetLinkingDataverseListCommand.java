
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.util.BundleUtil;
import java.util.List;

/**
 *
 * @author stephenkraffmiller
 */
@RequiredPermissions({})
public class GetLinkingDataverseListCommand extends AbstractCommand<List<Dataverse>> {

    private final String searchTerm;
    private final DvObject dvObject;
    private final boolean alreadyLinked;
    
    public GetLinkingDataverseListCommand(DataverseRequest aRequest, DvObject dvObject, String searchTerm) {
        super(aRequest, dvObject);
        this.searchTerm = searchTerm;
        this.dvObject = dvObject;
        this.alreadyLinked = false;
    }
    
    public GetLinkingDataverseListCommand(DataverseRequest aRequest, DvObject dvObject, boolean alreadyLinked) {
        super(aRequest, dvObject);
        this.searchTerm = "";
        this.dvObject = dvObject;
        this.alreadyLinked = alreadyLinked;
    }
    
    public GetLinkingDataverseListCommand(DataverseRequest aRequest, DvObject dvObject, String searchTerm, boolean alreadyLinked) {
        super(aRequest, dvObject);
        this.searchTerm = searchTerm;
        this.dvObject = dvObject;
        this.alreadyLinked = alreadyLinked;
    }



    @Override
    public List<Dataverse> execute(CommandContext ctxt) throws CommandException {

        User requestUser = (User) getRequest().getUser();
        AuthenticatedUser authUser;
        if (!requestUser.isAuthenticated()) {
            throw new IllegalCommandException(BundleUtil.getStringFromBundle("dataverse.link.user"), this);
        } else {
            authUser = (AuthenticatedUser) requestUser;
        }
        List<Dataverse> dataversesForLinking;
        String searchParam;
        if (searchTerm != null) {
            searchParam = searchTerm;
        } else {
            searchParam = "";
        }

        //Find Permitted Collections now takes a Search Term to filter down collections the user may link
        Permission permToCheck;
        if (dvObject instanceof Dataset) {
            permToCheck = Permission.LinkDataset;
        } else {
            permToCheck = Permission.LinkDataverse;
        }
        //dependin on the already linked boolean the command will return a list of Dataverses available for linking
        // or a list of dataverses to which the object has already been linked - for the unlink function
        if (!alreadyLinked) {
            dataversesForLinking = ctxt.permissions().findPermittedCollections(getRequest(), authUser, permToCheck, searchParam);
            //Don't bother with checking for already linked if there are none to be tested.
            if (dataversesForLinking == null || dataversesForLinking.isEmpty()) {
                return dataversesForLinking;
            }
            return ctxt.dataverses().removeUnlinkableDataverses(dataversesForLinking, dvObject);
        } else {
            if (dvObject instanceof Dataset) {
                return ctxt.dsLinking().findLinkingDataverses(dvObject.getId(), searchParam);
            } else {
                return ctxt.dvLinking().findLinkingDataverses(dvObject.getId());
            }
        }
    }
}
