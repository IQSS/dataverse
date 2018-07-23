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
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Lists the content of a dataverse - both datasets and dataverses.
 *
 * @author michael
 */
// no annotations here, since permissions are dynamically decided
public class ListDataverseContentCommand extends AbstractCommand<List<DvObject>> {

    private static final Logger logger = Logger.getLogger(ListDataverseContentCommand.class.getName());

    private final Dataverse dvToList;

    public ListDataverseContentCommand(DataverseRequest aRequest, Dataverse anAffectedDataverse) {
        super(aRequest, anAffectedDataverse);
        dvToList = anAffectedDataverse;
    }

    @Override
    public List<DvObject> execute(CommandContext ctxt) throws CommandException {
        LinkedList<DvObject> result = new LinkedList<>();
        User user = getRequest().getUser();
        if (user.isSuperuser()) {
            result.addAll(ctxt.datasets().findByOwnerId(dvToList.getId()));
            result.addAll(ctxt.dataverses().findByOwnerId(dvToList.getId()));
        } else if (user.isAuthenticated()) {
            AuthenticatedUser au = (AuthenticatedUser) user;
            List<Dataset> datasets = ctxt.datasets().findByOwnerId(dvToList.getId());
            int i = 0;
            long t0 = System.currentTimeMillis();
            for (Dataset ds : datasets) {
                i++;
                logger.info("On "+i+" out of " + datasets.size());
                if (ds.isReleased() || ctxt.permissions().requestOn(getRequest(), ds).has(Permission.ViewUnpublishedDataset)) {
                    result.add(ds);
                }
            }
            logger.info(""+(System.currentTimeMillis()-t0));
            List<Dataverse> dataverses = ctxt.dataverses().findByOwnerId(dvToList.getId());
            for (Dataverse dv : dataverses) {
                i++;
                logger.info("On "+i+" out of " + (datasets.size()+dataverses.size()));
                if (dv.isReleased() || ctxt.permissions().requestOn(getRequest(), dv).has(Permission.ViewUnpublishedDataverse)) {
                    result.add(dv);
                }
            }
            logger.info(""+(System.currentTimeMillis()-t0));
        } else {
            result.addAll(ctxt.datasets().findPublishedByOwnerId(dvToList.getId()));
            result.addAll(ctxt.dataverses().findPublishedByOwnerId(dvToList.getId()));
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
