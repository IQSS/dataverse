package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lists the content of a dataverse - both datasets and dataverses.
 *
 * @author michael
 */
// no annotations here, since permissions are dynamically decided
public class ListDataverseContentCommand extends AbstractCommand<List<DvObject>> {

    private final Dataverse dvToList;

    public ListDataverseContentCommand(DataverseRequest aRequest, Dataverse anAffectedDataverse) {
        super(aRequest, anAffectedDataverse);
        dvToList = anAffectedDataverse;
    }

    @Override
    public List<DvObject> execute(CommandContext ctxt) throws CommandException {
        LinkedList<DvObject> result = new LinkedList<>();
        
        for (Dataset ds : ctxt.datasets().findByOwnerId(dvToList.getId())) {            
            if (ds.isReleased() || ctxt.permissions().requestOn(getRequest(), ds).has(Permission.ViewUnpublishedDataset)) {
                result.add(ds);
            }
        }
        
        for (Dataverse dv : ctxt.dataverses().findByOwnerId(dvToList.getId())) {
            if (dv.isReleased() || ctxt.permissions().requestOn(getRequest(), dv).has(Permission.ViewUnpublishedDataverse)) {
                result.add(dv);
            }
        }

        return result;
    }

    @Override
    public Map<String, Set<Permission>> getRequiredPermissions() {
        return Collections.singletonMap("",
                dvToList.isReleased() ? Collections.<Permission>emptySet()
                : Collections.singleton(Permission.ViewUnpublishedDataverse));
    }

}
