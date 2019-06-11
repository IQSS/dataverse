package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseFacet;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * List the search facets {@link DataverseFacet} of a {@link Dataverse}.
 * @author michaelsuo
 */
// no annotations here, since permissions are dynamically decided
public class ListFacetsCommand extends AbstractCommand<List<DataverseFacet>> {

    private final Dataverse dv;

    public ListFacetsCommand(DataverseRequest aRequest, Dataverse aDataverse) {
        super(aRequest, aDataverse);
        dv = aDataverse;
    }

    @Override
    public List<DataverseFacet> execute(CommandContext ctxt) throws CommandException {
        return dv.getDataverseFacets();
    }

    @Override
    public Map<String, Set<Permission>> getRequiredPermissions() {
        return Collections.singletonMap("",
                dv.isReleased() ? Collections.<Permission>emptySet()
                : Collections.singleton(Permission.ViewUnpublishedDataverse));
    }
}
