package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DatasetVersionUser;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.IndexServiceBean;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.AbstractVoidCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

/**
 * Same as {@link DeleteDatasetCommand}, but does not stop it the dataset is
 * published. This command is reserved for super-users, if at all.
 *
 * @author michael
 */
// No permission check as destory currently works outside the permission system
// (user is checked for superuser)
//@RequiredPermissions(Permission.DestructiveEdit)
public class DestroyDatasetCommand extends AbstractVoidCommand {

    private final Dataset doomed;
    private final User user;

    public DestroyDatasetCommand(Dataset doomed, User aUser) {
        super(aUser, doomed);
        this.doomed = doomed;
        this.user = aUser;
    }

    @Override
    protected void executeImpl(CommandContext ctxt) throws CommandException {

        if (user instanceof AuthenticatedUser) {
            if (((AuthenticatedUser) user).isSuperuser()) {
                Set<Permission> returnSet = Collections.emptySet();
                throw new PermissionException("Destroy can only be called by superusers.",
                    this, returnSet, doomed);                
            }
        }
        
        final Dataset managedDoomed = ctxt.em().merge(doomed);

        // ASSIGNMENTS
        for (RoleAssignment ra : ctxt.roles().directRoleAssignments(doomed)) {
            ctxt.em().remove(ra);
        }
        // ROLES
        for (DataverseRole ra : ctxt.roles().findByOwnerId(doomed.getId())) {
            ctxt.em().remove(ra);
        }

        // files need to iterate through and remove 'by hand' to avoid
        // optimistic lock issues....
        
        Iterator <DataFile> dfIt = doomed.getFiles().iterator();
        while (dfIt.hasNext()){
            DataFile df = dfIt.next();
            ctxt.engine().submit(new DeleteDataFileCommand(df, getUser()));
            dfIt.remove();
        }
        
        // version users and versions 
        for (DatasetVersion ver : managedDoomed.getVersions()) {
            for (DatasetVersionUser ddu : ver.getDatasetVersionDataverseUsers()) {
                ctxt.em().remove(ddu);
            }
            ctxt.em().remove(ver);
        }
        /* commented out because handled above
         -- not sure the reason for the merge
         doesn't seem to be needed in above code
         for ( DatasetVersion ver : managedDoomed.getVersions() ) {
         DatasetVersion managed = ctxt.em().merge(ver);
         ctxt.em().remove( managed );
         }*/

        Dataverse toReIndex = managedDoomed.getOwner();

        // dataset
        ctxt.em().remove(managedDoomed);

        //remove from index
        String indexingResult = ctxt.index().removeSolrDocFromIndex(IndexServiceBean.solrDocIdentifierDataset + doomed.getId() + IndexServiceBean.draftSuffix);

        ctxt.index().indexDataverse(toReIndex);
    }

}
